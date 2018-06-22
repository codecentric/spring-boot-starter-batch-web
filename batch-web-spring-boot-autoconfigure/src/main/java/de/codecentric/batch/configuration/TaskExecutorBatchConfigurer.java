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

package de.codecentric.batch.configuration;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.BatchConfigurer;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.explore.support.JobExplorerFactoryBean;
import org.springframework.batch.core.explore.support.MapJobExplorerFactoryBean;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.SimpleJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.core.repository.support.MapJobRepositoryFactoryBean;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * This batch infrastructure configuration is quite similar to the
 * {@link org.springframework.batch.core.configuration.annotation.DefaultBatchConfigurer}, it only references a
 * {@link org.springframework.core.task.TaskExecutor} used in the
 * {@link org.springframework.batch.core.launch.support.SimpleJobLauncher} for starting jobs asynchronously.
 * 
 * @author Tobias Flohre
 * @author Dennis Schulte
 * 
 */
@ConditionalOnMissingBean(BatchConfigurer.class)
@Configuration
public class TaskExecutorBatchConfigurer implements BatchConfigurer {

	private static final Logger LOGGER = LoggerFactory.getLogger(TaskExecutorBatchConfigurer.class);

	@Autowired
	private Environment env;

	// Created by TaskExecutorConfiguration if it is used. If an alternative TaskExecutor is configured,
	// it will be injected here.
	@Autowired
	private TaskExecutor taskExecutor;

	private DataSource dataSource;

	private PlatformTransactionManager transactionManager;

	private JobRepository jobRepository;

	private JobLauncher jobLauncher;

	private JobExplorer jobExplorer;

	@Autowired
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
		this.transactionManager = new DataSourceTransactionManager(dataSource);
	}

	@Override
	public JobRepository getJobRepository() {
		return jobRepository;
	}

	@Override
	public PlatformTransactionManager getTransactionManager() {
		return transactionManager;
	}

	@Override
	public JobLauncher getJobLauncher() {
		return jobLauncher;
	}

	@Override
	public JobExplorer getJobExplorer() throws Exception {
		return jobExplorer;
	}

	private JobLauncher createJobLauncher() throws Exception {
		SimpleJobLauncher jobLauncher = new SimpleJobLauncher();
		jobLauncher.setJobRepository(jobRepository);
		jobLauncher.setTaskExecutor(taskExecutor);
		jobLauncher.afterPropertiesSet();
		return jobLauncher;
	}

	protected JobRepository createJobRepository() throws Exception {
		JobRepositoryFactoryBean factory = new JobRepositoryFactoryBean();
		factory.setDataSource(dataSource);
		factory.setTransactionManager(transactionManager);
		String isolationLevelForCreate = env.getProperty("batch.repository.isolationlevelforcreate");
		if (isolationLevelForCreate != null) {
			factory.setIsolationLevelForCreate(isolationLevelForCreate);
		}
		String tablePrefix = env.getProperty("batch.repository.tableprefix");
		if (tablePrefix != null) {
			factory.setTablePrefix(tablePrefix);
		}
		factory.afterPropertiesSet();
		return factory.getObject();
	}

	@PostConstruct
	public void initialize() throws Exception {
		if (dataSource == null) {
			LOGGER.warn("No datasource was provided...using a Map based JobRepository");

			if (this.transactionManager == null) {
				this.transactionManager = new ResourcelessTransactionManager();
			}

			MapJobRepositoryFactoryBean jobRepositoryFactory = new MapJobRepositoryFactoryBean(this.transactionManager);
			jobRepositoryFactory.afterPropertiesSet();
			this.jobRepository = jobRepositoryFactory.getObject();

			MapJobExplorerFactoryBean jobExplorerFactory = new MapJobExplorerFactoryBean(jobRepositoryFactory);
			jobExplorerFactory.afterPropertiesSet();
			this.jobExplorer = jobExplorerFactory.getObject();
		} else {
			this.jobRepository = createJobRepository();

			JobExplorerFactoryBean jobExplorerFactoryBean = new JobExplorerFactoryBean();
			jobExplorerFactoryBean.setDataSource(this.dataSource);
			String tablePrefix = env.getProperty("batch.repository.tableprefix");
			if (tablePrefix != null) {
				jobExplorerFactoryBean.setTablePrefix(tablePrefix);
			}
			jobExplorerFactoryBean.afterPropertiesSet();
			this.jobExplorer = jobExplorerFactoryBean.getObject();
		}

		this.jobLauncher = createJobLauncher();
	}

}
