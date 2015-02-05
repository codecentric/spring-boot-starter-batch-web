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
package de.codecentric.batch.metrics;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.listener.StepExecutionListenerSupport;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.boot.actuate.metrics.GaugeService;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.repository.MetricRepository;
import org.springframework.boot.actuate.metrics.rich.RichGauge;
import org.springframework.boot.actuate.metrics.rich.RichGaugeRepository;
import org.springframework.core.Ordered;

import com.codahale.metrics.ScheduledReporter;

/**
 * This listener exports all metrics with the prefix 'counter.batch.{jobName}.{jobExecutionId}.{stepName}
 * and all gauges with the prefix 'gauge.batch.{jobName}.{jobExecutionId}.{stepName}' to the Step-
 * ExecutionContext without the prefix. All metrics and gauges are logged as well. For
 * overriding the default format of the logging a component implementing {@link MetricsOutputFormatter} may be added to the ApplicationContext.
 * 
 * If deleteMetricsOnStepFinish is true, all metrics will be removed from Spring Boot's metric
 * framework (MetricRepository) when the job finishes and the metrics are written to the Step-ExecutionContext.
 * 
 * Counters are cumulated over several StepExecutions belonging to one Step in one JobInstance,
 * important for restarted jobs.
 * 
 * @author Tobias Flohre
 * @author Dennis Schulte
 */
public class MetricsListener extends StepExecutionListenerSupport implements Ordered, JobExecutionListener {

	private static final Log LOGGER = LogFactory.getLog(MetricsListener.class);

	public static final String GAUGE_PREFIX = "gauge.batch.";

	public static final String COUNTER_PREFIX = "counter.batch.";

	private GaugeService gaugeService;
	private CounterService counterService;

	private RichGaugeRepository richGaugeRepository;
	private MetricRepository metricRepository;
	private List<ScheduledReporter> metricReporters;
	private boolean deleteMetricsOnStepFinish;
	@Autowired(required = false)
	private MetricsOutputFormatter metricsOutputFormatter = new SimpleMetricsOutputFormatter();

	public MetricsListener(GaugeService gaugeService, CounterService counterService, RichGaugeRepository richGaugeRepository,
			MetricRepository metricRepository, List<ScheduledReporter> metricReporters, boolean deleteMetricsOnStepFinish) {
		this.gaugeService = gaugeService;
		this.counterService = counterService;
		this.richGaugeRepository = richGaugeRepository;
		this.metricRepository = metricRepository;
		this.metricReporters = metricReporters;
		this.deleteMetricsOnStepFinish = deleteMetricsOnStepFinish;
	}

	@Override
	public void beforeJob(JobExecution jobExecution) {
		counterService.increment(COUNTER_PREFIX + jobExecution.getJobInstance().getJobName());
	}

	@Override
	public void beforeStep(StepExecution stepExecution) {
		counterService.increment(COUNTER_PREFIX + getStepExecutionIdentifier());
	}

	@Override
	public ExitStatus afterStep(StepExecution stepExecution) {
		// Calculate step execution time
		// Why is stepExecution.getEndTime().getTime() not available here? (see AbstractStep)
		long stepDuration = System.currentTimeMillis() - stepExecution.getStartTime().getTime();
		gaugeService.submit(GAUGE_PREFIX + getStepExecutionIdentifier() + ".duration", stepDuration);
		long itemCount = stepExecution.getWriteCount() + stepExecution.getSkipCount();
		gaugeService.submit(GAUGE_PREFIX + getStepExecutionIdentifier() + ".item.count", itemCount);
		// Calculate execution time per item
		long durationPerItem = 0;
		if (itemCount > 0) {
			durationPerItem = stepDuration / itemCount;
		}
		gaugeService.submit(GAUGE_PREFIX + getStepExecutionIdentifier() + ".item.duration", durationPerItem);
		// What the f*** is that Thread.sleep doing here? ;-)
		// Metrics are written asynchronously to Spring Boot's repository. In our tests we experienced
		// that sometimes batch execution was so fast that this listener couldn't export the metrics
		// because they hadn't been written. It all happened in the same millisecond. So we added
		// a Thread.sleep of 100 milliseconds which gives us enough safety and doesn't hurt anyone.
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		// Export Metrics to console
		List<RichGauge> gauges = exportBatchRichGauges();
		LOGGER.info(metricsOutputFormatter.format(gauges, null));
		return null;
	}

	@Override
	public void afterJob(JobExecution jobExecution) {
		long jobDuration = jobExecution.getEndTime().getTime() - jobExecution.getStartTime().getTime();
		gaugeService.submit(GAUGE_PREFIX + jobExecution.getJobInstance().getJobName() + ".duration", jobDuration);
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		if (metricReporters != null) {
			for (ScheduledReporter reporter : metricReporters) {
				if (reporter != null) {
					LOGGER.info("Exporting Metrics with " + reporter.getClass().getName());
					reporter.report();
				}
			}
		}
		// Delete local metrics on step finish?
		if (deleteMetricsOnStepFinish) {
			for (Metric<?> metric : metricRepository.findAll()) {
				metricRepository.reset(metric.getName());
			}
			for (RichGauge gauge : richGaugeRepository.findAll()) {
				richGaugeRepository.reset(gauge.getName());
			}
		}
	}

	private List<Metric<?>> exportBatchMetrics() {
		List<Metric<?>> metrics = new ArrayList<Metric<?>>();
		for (Metric<?> metric : metricRepository.findAll()) {
			metrics.add(metric);
		}
		return metrics;
	}

	private List<RichGauge> exportBatchRichGauges() {
		List<RichGauge> gauges = new ArrayList<RichGauge>();
		for (RichGauge gauge : richGaugeRepository.findAll()) {
			gauges.add(gauge);
		}
		return gauges;
	}

	private static class SimpleMetricsOutputFormatter implements MetricsOutputFormatter {

		@Override
		public String format(List<RichGauge> gauges, List<Metric<?>> metrics) {
			StringBuilder builder = new StringBuilder("\n########## Metrics Start ##########\n");
			if (gauges != null) {
				for (RichGauge gauge : gauges) {
					builder.append(gauge.toString() + "\n");
				}
			}
			if (metrics != null) {
				for (Metric<?> metric : metrics) {
					builder.append(metric.toString() + "\n");
				}
			}
			builder.append("########## Metrics End ############");
			return builder.toString();
		}

	}

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE - 1;
	}

	private String getStepExecutionIdentifier() {
		StepContext stepContext = StepSynchronizationManager.getContext();
		StepExecution stepExecution = StepSynchronizationManager.getContext().getStepExecution();
		return stepContext.getJobName() + "." + stepExecution.getStepName();
	}
}
