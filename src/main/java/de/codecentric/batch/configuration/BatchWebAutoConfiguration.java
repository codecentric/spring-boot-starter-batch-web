/*
 * Copyright 2012-2014 the original author or authors.
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.support.AutomaticJobRegistrar;
import org.springframework.batch.core.configuration.support.GenericApplicationContextFactory;
import org.springframework.batch.core.job.AbstractJob;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.step.AbstractStep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.ClassUtils;
import org.springframework.util.SystemPropertyUtils;

import de.codecentric.batch.listener.LoggingListener;
import de.codecentric.batch.listener.LoggingAfterJobListener;
import de.codecentric.batch.listener.ProtocolListener;
import de.codecentric.batch.listener.RunningExecutionTrackerListener;
import de.codecentric.batch.monitoring.RunningExecutionTracker;

@Configuration
@EnableBatchProcessing(modular = true)
@PropertySource("classpath:spring-boot-starter-batch-web.properties")
@Import({WebConfig.class,TaskExecutorBatchConfigurer.class})
public class BatchWebAutoConfiguration implements ApplicationListener<ContextRefreshedEvent> {

	@Autowired
	private Environment env;
	@Autowired
	private AutomaticJobRegistrar automaticJobRegistrar;
	@Autowired
	private JobRegistry jobRegistry;

	//################### Listeners automatically added to each job #################################
	
	@Bean
	public LoggingListener loggingListener(){
		return new LoggingListener();
	}
	
	@Bean
	public LoggingAfterJobListener loggingReDoListener(){
		return new LoggingAfterJobListener();
	}
	
	@Bean
	public ProtocolListener protocolListener() {
		return new ProtocolListener();
	}
	
	@Bean
	public RunningExecutionTracker runningExecutionTracker(){
		return new RunningExecutionTracker();
	}
	
	@Bean
	public RunningExecutionTrackerListener runningExecutionTrackerListener(){
		return new RunningExecutionTrackerListener(runningExecutionTracker());
	}
	
	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		try {
			addListenerToJob();
		} catch (NoSuchJobException e) {
			throw new IllegalStateException(e);
		}
	}
	
	private void addListenerToJob() throws NoSuchJobException {
		for (String jobName : jobRegistry.getJobNames()) {
			AbstractJob job = (AbstractJob)jobRegistry.getJob(jobName);
			job.registerJobExecutionListener(protocolListener());
			job.registerJobExecutionListener(runningExecutionTrackerListener());
			job.registerJobExecutionListener(loggingListener());
			job.registerJobExecutionListener(loggingReDoListener());
			for (String stepName: job.getStepNames()){
				AbstractStep step = (AbstractStep)job.getStep(stepName);
				step.registerStepExecutionListener(loggingListener());
			}
		}
	}

	//################### Registering jobs from certain locations and packages #########################

	@PostConstruct
	public void initialize() throws Exception {
		registerJobsFromXml();
		registerJobsFromJavaConfig();
	}

	private void registerJobsFromXml() throws IOException {
		// Add all XML-Configurations to the AutomaticJobRegistrar
		ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
		Resource[] xmlConfigurations = resourcePatternResolver.getResources("classpath*:"+ env.getProperty("batch.config.path.xml",
								"/META-INF/spring/batch/jobs") + "/*.xml");
		for (Resource resource : xmlConfigurations) {
			automaticJobRegistrar.addApplicationContextFactory(new GenericApplicationContextFactory(resource));
		}
	}

	private void registerJobsFromJavaConfig() throws ClassNotFoundException,
			IOException {
		List<Class<?>> classes = findMyTypes(env.getProperty("batch.config.package.javaconfig", "spring.batch.jobs"));
		for (Class<?> clazz : classes) {
			automaticJobRegistrar.addApplicationContextFactory(new GenericApplicationContextFactory(clazz));
		}
	}

	private List<Class<?>> findMyTypes(String basePackage) throws IOException,
			ClassNotFoundException {
		ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
		MetadataReaderFactory metadataReaderFactory = new CachingMetadataReaderFactory(resourcePatternResolver);

		List<Class<?>> candidates = new ArrayList<Class<?>>();
		String packageSearchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX	+ resolveBasePackage(basePackage) + "/" + "**/*.class";
		Resource[] resources = resourcePatternResolver.getResources(packageSearchPath);
		for (Resource resource : resources) {
			if (resource.isReadable()) {
				MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(resource);
				if (isCandidate(metadataReader)) {
					candidates.add(Class.forName(metadataReader.getClassMetadata().getClassName()));
				}
			}
		}
		return candidates;
	}

	private String resolveBasePackage(String basePackage) {
		return ClassUtils.convertClassNameToResourcePath(SystemPropertyUtils.resolvePlaceholders(basePackage));
	}

	private boolean isCandidate(MetadataReader metadataReader)
			throws ClassNotFoundException {
		try {
			Class<?> c = Class.forName(metadataReader.getClassMetadata().getClassName());
			if (c.getAnnotation(Configuration.class) != null) {
				return true;
			}
		} catch (Throwable e) {
		}
		return false;
	}

}
