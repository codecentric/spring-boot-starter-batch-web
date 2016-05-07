package de.codecentric.batch.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("batch")
public class BatchConfigurationProperties {

	/*
	 * Configures the automatic registration of job configurations.
	 */
	private JobConfigurationProperties config = new JobConfigurationProperties();

	/*
	 * Enable printing of the the default job protocol to the logs.
	 */
	private Toggle defaultProtocol = new Toggle(true);

	/*
	 * Enable writing of a new logfile for each job execution.
	 */
	private Toggle logfileSeparation = new Toggle(true);

	/*
	 * Configures the jobReposiotry
	 */
	private RepositoryConfigurationProperties repository = new RepositoryConfigurationProperties();


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

	public static class JobConfigurationProperties {
		/*
		 * Location in the classpath where Spring Batch job definitions in XML are picked
		 * up.
		 */
		private String pathXml = "classpath*:/META-INF/spring/batch/jobs/*.xml";

		/*
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
		/*
		 *
		 */
		private String isolationLevelForCreate = null;

		/*
		 *
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
