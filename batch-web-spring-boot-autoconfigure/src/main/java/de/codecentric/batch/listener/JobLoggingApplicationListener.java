package de.codecentric.batch.listener;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;

/**
 * This ApplicationListener makes the batch.joblog.path available before the LoggingSystem is started.
 * 
 * @author Johannes Stelzer
 */
public class JobLoggingApplicationListener implements ApplicationListener<ApplicationPreparedEvent>, Ordered {

	@Override
	public void onApplicationEvent(ApplicationPreparedEvent event) {
		String jobLogPath = event.getApplicationContext().getEnvironment().getProperty("batch.joblog.path");
		if (!StringUtils.isEmpty(jobLogPath)) {
			if (!jobLogPath.endsWith("/")) {
				jobLogPath = jobLogPath + "/";
			}
			System.setProperty("JOB_LOG_PATH", jobLogPath);
		}
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE + 11;
	}

}
