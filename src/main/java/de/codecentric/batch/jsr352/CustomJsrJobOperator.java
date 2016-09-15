/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.codecentric.batch.jsr352;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

import javax.batch.operations.BatchRuntimeException;
import javax.batch.operations.JobSecurityException;
import javax.batch.operations.JobStartException;
import javax.batch.operations.NoSuchJobExecutionException;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.configuration.DuplicateJobException;
import org.springframework.batch.core.converter.JobParametersConverter;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.job.AbstractJob;
import org.springframework.batch.core.jsr.JsrJobContextFactoryBean;
import org.springframework.batch.core.jsr.configuration.xml.JsrXmlApplicationContext;
import org.springframework.batch.core.jsr.launch.JsrJobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.Assert;

import de.codecentric.batch.listener.AddListenerToJobService;

/**
 * We cannot use Spring Batch's JsrJobOperator out of two reasons:
 *
 * <p>In the current implementation it's not possible to use an existing ApplicationContext
 * as base context for the batch job contexts.<br>
 * Second reason is that we want to add listeners automatically to the job for having features
 * like log file separation and standard batch protocols.<br>
 *
 * That's why I patched it to add the functionality we need.
 *
 * @author Tobias Flohre
 */
public class CustomJsrJobOperator extends JsrJobOperator {
	private static final String JSR_JOB_CONTEXT_BEAN_NAME = "jsr_jobContext";

	private ApplicationContext parentContext;
	private JobRepository jobRepository;
	private TaskExecutor taskExecutor;
	private JobParametersConverter jobParametersConverter;
	private static ExecutingJobRegistry jobRegistry = new ExecutingJobRegistry();
	private AddListenerToJobService addListenerToJobService;

	public CustomJsrJobOperator(JobExplorer jobExplorer,
			JobRepository jobRepository,
			JobParametersConverter jobParametersConverter,
			AddListenerToJobService addListenerToJobService,
			PlatformTransactionManager transactionManager) {
		super(jobExplorer, jobRepository, jobParametersConverter, transactionManager);
		this.jobRepository = jobRepository;
		this.jobParametersConverter = jobParametersConverter;
		this.addListenerToJobService = addListenerToJobService;
	}

	@Override
	public void setTaskExecutor(TaskExecutor taskExecutor) {
		super.setTaskExecutor(taskExecutor);
		this.taskExecutor = taskExecutor;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.parentContext = applicationContext;
	}

	@Override
	public long start(String jobName, Properties params)
			throws JobStartException, JobSecurityException {
		final JsrXmlApplicationContext batchContext = new JsrXmlApplicationContext(params);
		batchContext.setValidating(false);

		Resource batchXml = new ClassPathResource("/META-INF/batch.xml");
		String jobConfigurationLocation = "/META-INF/batch-jobs/" + jobName + ".xml";
		Resource jobXml = new ClassPathResource(jobConfigurationLocation);

		if(batchXml.exists()) {
			batchContext.load(batchXml);
		}

		if(jobXml.exists()) {
			batchContext.load(jobXml);
		}

		AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder.genericBeanDefinition("org.springframework.batch.core.jsr.JsrJobContextFactoryBean").getBeanDefinition();
		beanDefinition.setScope(BeanDefinition.SCOPE_SINGLETON);
		batchContext.registerBeanDefinition(JSR_JOB_CONTEXT_BEAN_NAME, beanDefinition);

		batchContext.setParent(parentContext);

		try {
			batchContext.refresh();
		} catch (BeanCreationException e) {
			throw new JobStartException(e);
		}

		Assert.notNull(jobName, "The job name must not be null.");

		final org.springframework.batch.core.JobExecution jobExecution;

		try {
			JobParameters jobParameters = jobParametersConverter.getJobParameters(params);
			String [] jobNames = batchContext.getBeanNamesForType(Job.class);

			if(jobNames == null || jobNames.length <= 0) {
				throw new BatchRuntimeException("No Job defined in current context");
			}

			org.springframework.batch.core.JobInstance jobInstance = jobRepository.createJobInstance(jobNames[0], jobParameters);
			jobExecution = jobRepository.createJobExecution(jobInstance, jobParameters, jobConfigurationLocation);
		} catch (Exception e) {
			throw new JobStartException(e);
		}

		try {
			final Semaphore semaphore = new Semaphore(1);
			final List<Exception> exceptionHolder = Collections.synchronizedList(new ArrayList<Exception>());
			semaphore.acquire();

			taskExecutor.execute(new Runnable() {

				@Override
				public void run() {
					JsrJobContextFactoryBean factoryBean = null;
					try {
						factoryBean = (JsrJobContextFactoryBean) batchContext.getBean("&" + JSR_JOB_CONTEXT_BEAN_NAME);
						factoryBean.setJobExecution(jobExecution);
						final AbstractJob job = batchContext.getBean(AbstractJob.class);
						addListenerToJobService.addListenerToJob(job);
						semaphore.release();
						// Initialization of the JobExecution for job level dependencies
						jobRegistry.register(job, jobExecution);
						job.execute(jobExecution);
						jobRegistry.remove(jobExecution);
					}
					catch (Exception e) {
						exceptionHolder.add(e);
					} finally {
						if(factoryBean != null) {
							factoryBean.close();
						}

						batchContext.close();

						if(semaphore.availablePermits() == 0) {
							semaphore.release();
						}
					}
				}
			});

			semaphore.acquire();
			if(exceptionHolder.size() > 0) {
				semaphore.release();
				throw new JobStartException(exceptionHolder.get(0));
			}
		}
		catch (Exception e) {
			if(jobRegistry.exists(jobExecution.getId())) {
				jobRegistry.remove(jobExecution);
			}
			jobExecution.upgradeStatus(BatchStatus.FAILED);
			if (jobExecution.getExitStatus().equals(ExitStatus.UNKNOWN)) {
				jobExecution.setExitStatus(ExitStatus.FAILED.addExitDescription(e));
			}
			jobRepository.update(jobExecution);

			if(batchContext.isActive()) {
				batchContext.close();
			}

			throw new JobStartException(e);
		}
		return jobExecution.getId();
	}

	private static class ExecutingJobRegistry {

		private Map<Long, Job> registry = new ConcurrentHashMap<Long, Job>();

		public void register(Job job, org.springframework.batch.core.JobExecution jobExecution) throws DuplicateJobException {

			if(registry.containsKey(jobExecution.getId())) {
				throw new DuplicateJobException("This job execution has already been registered");
			} else {
				registry.put(jobExecution.getId(), job);
			}
		}

		public void remove(org.springframework.batch.core.JobExecution jobExecution) {
			if(!registry.containsKey(jobExecution.getId())) {
				throw new NoSuchJobExecutionException("The job execution " + jobExecution.getId() + " was not found");
			} else {
				registry.remove(jobExecution.getId());
			}
		}

		public boolean exists(long jobExecutionId) {
			return registry.containsKey(jobExecutionId);
		}

	}


}
