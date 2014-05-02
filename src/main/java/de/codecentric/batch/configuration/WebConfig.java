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

import java.util.List;

import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import de.codecentric.batch.web.JobMonitoringController;
import de.codecentric.batch.web.JobOperationsController;

/**
 * This configuration adds the controllers for the two endpoints, and it adds a Jackson MixIn to the
 * message converter to avoid a stack overflow through circular references in the JobExecution /
 * StepExecution.
 * 
 * @author Tobias Flohre
 *
 */
@Configuration
public class WebConfig extends WebMvcConfigurerAdapter {

	@Autowired
	private JobOperator jobOperator;
	@Autowired
	private JobExplorer jobExplorer;
	@Autowired
	private JobRegistry jobRegistry;
	@Autowired
	private JobRepository jobRepository;
	@Autowired
	private BatchWebAutoConfiguration batchWebAutoConfiguration;

	@Override
	public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
		for (HttpMessageConverter<?> httpMessageConverter : converters) {
			if (httpMessageConverter instanceof MappingJackson2HttpMessageConverter) {
				final MappingJackson2HttpMessageConverter converter = (MappingJackson2HttpMessageConverter) httpMessageConverter;
				converter.getObjectMapper().addMixInAnnotations(StepExecution.class,
						StepExecutionJacksonMixIn.class);
			}
		}
	}

	@Bean
	public JobMonitoringController jobMonitoringController(){
		return new JobMonitoringController(jobOperator,jobExplorer,batchWebAutoConfiguration.runningExecutionTracker());
	}
	
	@Bean
	public JobOperationsController jobOperationsController(){
		return new JobOperationsController(jobOperator,jobExplorer,jobRegistry,jobRepository);
	}
	
}
