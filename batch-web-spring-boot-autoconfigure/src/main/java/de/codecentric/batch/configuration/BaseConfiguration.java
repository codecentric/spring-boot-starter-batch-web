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

import javax.sql.DataSource;

import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;

/**
 * I don't like autowiring of business components, I prefer to reference them from a
 * JavaConfig configuration class. But since some components are created through other
 * Spring Boot components or by special mechanisms (for example the BatchConfigurer
 * mechanism) I want to have one place where I autowire all of those components and
 * explain where they are coming from. Whenever I need one of those components I
 * import this configuration class and reference those components via method call.
 *
 * @author Tobias Flohre
 */
@Configuration
public class BaseConfiguration {

	// Created by spring-boot-starter-batch in combination with our TaskExecutorBatchConfigurer
	@Autowired
	private JobOperator jobOperator;
	@Autowired
	private JobExplorer jobExplorer;
	@Autowired
	private JobRegistry jobRegistry;
	@Autowired
	private JobRepository jobRepository;
	@Autowired
	private JobLauncher jobLauncher;

	// Created by spring-boot-starter-jdbc
	@Autowired
	private DataSource dataSource;

	// Created by our TaskExecutorBatchConfigurer if it is used. If an alternative BatchConfigurer is used,
	// a TaskExecutor instance has to be provided somehow.
	@Autowired
	private TaskExecutor taskExecutor;

	public JobOperator jobOperator() {
		return jobOperator;
	}
	public JobExplorer jobExplorer() {
		return jobExplorer;
	}
	public JobRegistry jobRegistry() {
		return jobRegistry;
	}
	public JobRepository jobRepository() {
		return jobRepository;
	}
	public JobLauncher jobLauncher() {
		return jobLauncher;
	}
	public DataSource dataSource() {
		return dataSource;
	}
	public TaskExecutor taskExecutor() {
		return taskExecutor;
	}
}
