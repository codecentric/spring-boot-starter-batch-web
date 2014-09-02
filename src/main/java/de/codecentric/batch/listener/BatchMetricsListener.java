package de.codecentric.batch.listener;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.boot.actuate.metrics.rich.RichGauge;
import org.springframework.boot.actuate.metrics.rich.RichGaugeRepository;

/**
 * 
 * Listener for the batch metrics handling (resetting before job start and logging after job completion)
 * 
 * @author Dennis Schulte
 */
public class BatchMetricsListener implements JobExecutionListener {

	private static final Logger LOG = LoggerFactory.getLogger(BatchMetricsListener.class);

	private RichGaugeRepository richGaugeRepository;

	public BatchMetricsListener(RichGaugeRepository richGaugeRepository) {
		this.richGaugeRepository = richGaugeRepository;
	}

	@Override
	public void beforeJob(JobExecution jobExecution) {
		String jobName = getJobName();
		List<String> metricsToReset = new ArrayList<String>();
		for (RichGauge metric : richGaugeRepository.findAll()) {
			if (metric.getName().startsWith("gauge.duration.batch.jobs." + jobName) || metric.getName().startsWith("counter.batch.jobs." + jobName)) {
				metricsToReset.add(metric.getName());
			}
		}
		LOG.info("Resetting rich gauge batch metrics: " + metricsToReset.toString());
		for (String metricName : metricsToReset) {
			richGaugeRepository.reset(metricName);
		}
	}

	@Override
	public void afterJob(JobExecution jobExecution) {
		for (RichGauge richGauge : richGaugeRepository.findAll()) {
			LOG.info(richGauge.toString());
		}
	}

	private String getJobName() {
		String jobName = MDC.get(LoggingListener.JOBNAME);
		if (jobName == null) {
			throw new UnexpectedJobExecutionException("Jobname could not be read from MDC.");
		}
		return jobName;
	}

}
