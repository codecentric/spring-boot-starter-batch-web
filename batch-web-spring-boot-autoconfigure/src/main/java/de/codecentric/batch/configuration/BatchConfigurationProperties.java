/*
 * Copyright 2018 the original author or authors.
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

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("batch")
public class BatchConfigurationProperties {

	/**
	 * Configures the automatic registration of job configurations.
	 */
	private JobConfigurationProperties config = new JobConfigurationProperties();

	/**
	 * Enable printing of the the default job protocol to the logs.
	 */
	private Toggle defaultProtocol = new Toggle(true);

	/**
	 * Enable writing of a new logfile for each job execution.
	 */
	private Toggle logfileSeparation = new Toggle(true);

	/**
	 * Configures the jobRepository.
	 */
	private RepositoryConfigurationProperties repository = new RepositoryConfigurationProperties();

	/**
	 * Configures the taskExecutor.
	 */
	private TaskExecutorProperties taskExecutor = new TaskExecutorProperties();

	public Toggle getDefaultProtocol() {
		return defaultProtocol;
	}

	public Toggle getLogfileSeparation() {
		return logfileSeparation;
	}

	public JobConfigurationProperties getConfig() {
		return config;
	}

	public RepositoryConfigurationProperties getRepository() {
		return repository;
	}

	public TaskExecutorProperties getTaskExecutor() {
		return taskExecutor;
	}

	public static class TaskExecutorProperties {

		/**
		 * Core pool size of the thread pool.
		 */
		private int corePoolSize = 5;

		/**
		 * Queue capacity of the task executor.
		 */
		private int queueCapacity = Integer.MAX_VALUE;

		/**
		 * Max pool size of the thread pool.
		 */
		private int maxPoolSize = Integer.MAX_VALUE;

		public int getCorePoolSize() {
			return corePoolSize;
		}

		public void setCorePoolSize(int corePoolSize) {
			this.corePoolSize = corePoolSize;
		}

		public int getQueueCapacity() {
			return queueCapacity;
		}

		public void setQueueCapacity(int queueCapacity) {
			this.queueCapacity = queueCapacity;
		}

		public int getMaxPoolSize() {
			return maxPoolSize;
		}

		public void setMaxPoolSize(int maxPoolSize) {
			this.maxPoolSize = maxPoolSize;
		}

	}

	public static class JobConfigurationProperties {

		/**
		 * Location in the classpath where Spring Batch job definitions in XML are picked up.
		 */
		private String pathXml = "classpath*:/META-INF/spring/batch/jobs/*.xml";

		/**
		 * Package where Spring Batch job definitions in JavaConfig are picked up.
		 */
		private String packageJavaconfig = "spring.batch.jobs";

		public void setPathXml(String pathXml) {
			this.pathXml = pathXml;
		}

		public String getPathXml() {
			return pathXml;
		}

		public void setPackageJavaconfig(String packageJavaconfig) {
			this.packageJavaconfig = packageJavaconfig;
		}

		public String getPackageJavaconfig() {
			return packageJavaconfig;
		}

	}

	public static class RepositoryConfigurationProperties {

		/**
		 * Database isolation level for creating job executions.
		 */
		private String isolationLevelForCreate = null;

		/**
		 * Prefix for Spring Batch meta data tables.
		 */
		private String tablePrefix = null;

		public void setIsolationLevelForCreate(String isolationLevelForCreate) {
			this.isolationLevelForCreate = isolationLevelForCreate;
		}

		public String getIsolationLevelForCreate() {
			return isolationLevelForCreate;
		}

		public void setTablePrefix(String tablePrefix) {
			this.tablePrefix = tablePrefix;
		}

		public String getTablePrefix() {
			return tablePrefix;
		}

	}

	public static class Toggle {

		private boolean enabled;

		public Toggle(boolean enabled) {
			this.enabled = enabled;
		}

		public void setEnabled(boolean enabled) {
			this.enabled = enabled;
		}

		public boolean isEnabled() {
			return enabled;
		}
	}

}