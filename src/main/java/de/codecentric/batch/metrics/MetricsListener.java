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
 */
public class MetricsListener extends StepExecutionListenerSupport implements Ordered, JobExecutionListener {

	private static final Log LOGGER = LogFactory.getLog(MetricsListener.class);

	public static final String GAUGE_PREFIX = "gauge.batch.";

	public static final String COUNTER_PREFIX = "counter.batch.";

	private GaugeService gaugeService;
	private CounterService counterService;

	private RichGaugeRepository richGaugeRepository;
	private MetricRepository metricRepository;
	private ScheduledReporter reporter;
	private boolean deleteMetricsOnStepFinish;
	@Autowired(required = false)
	private MetricsOutputFormatter metricsOutputFormatter = new SimpleMetricsOutputFormatter();

	public MetricsListener(GaugeService gaugeService, CounterService counterService, RichGaugeRepository richGaugeRepository,
			MetricRepository metricRepository, ScheduledReporter reporter, boolean deleteMetricsOnStepFinish) {
		this.gaugeService = gaugeService;
		this.counterService = counterService;
		this.richGaugeRepository = richGaugeRepository;
		this.metricRepository = metricRepository;
		this.reporter = reporter;
		this.deleteMetricsOnStepFinish = deleteMetricsOnStepFinish;
	}

	@Override
	public void beforeJob(JobExecution jobExecution) {
		counterService.increment(COUNTER_PREFIX + jobExecution.getJobInstance().getJobName());
	}

	@Override
	public void beforeStep(StepExecution stepExecution) {
		counterService.increment(COUNTER_PREFIX + stepExecution.getJobExecution().getJobInstance().getJobName() + ".step."
				+ stepExecution.getStepName());
	}

	@Override
	public ExitStatus afterStep(StepExecution stepExecution) {
		// Calculate step execution time
		// Why is stepExecution.getEndTime().getTime() not available here? (see AbstractStep)
		long duration = System.currentTimeMillis() - stepExecution.getStartTime().getTime();
		gaugeService.submit(GAUGE_PREFIX + stepExecution.getJobExecution().getJobInstance().getJobName() + ".step." + stepExecution.getStepName()
				+ ".duration", duration);
		long count = stepExecution.getWriteCount() + stepExecution.getSkipCount();
		gaugeService.submit(GAUGE_PREFIX + stepExecution.getJobExecution().getJobInstance().getJobName() + ".step." + stepExecution.getStepName()
				+ ".item.count", count);
		// Calculate execution time per item
		long durationPerItem = duration / count;
		gaugeService.submit(GAUGE_PREFIX + stepExecution.getJobExecution().getJobInstance().getJobName() + ".step." + stepExecution.getStepName()
				+ ".item.duration", durationPerItem);
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
		// Export Metrics to the Step-ExecutionContext, so that they are available after a restart
		List<RichGauge> gauges = exportBatchRichGauges(stepExecution);
		List<Metric<?>> metrics = exportBatchMetrics(stepExecution);
		LOGGER.info(metricsOutputFormatter.format(gauges, metrics));
		return null;
	}

	@Override
	public void afterJob(JobExecution jobExecution) {
		long duration = jobExecution.getEndTime().getTime() - jobExecution.getStartTime().getTime();
		gaugeService.submit("batch." + jobExecution.getJobInstance().getJobName() + ".duration", duration);
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		reporter.report();
	}

	private List<Metric<?>> exportBatchMetrics(StepExecution stepExecution) {
		String stepExecutionIdentifier = getStepExecutionIdentifier();
		List<Metric<?>> metrics = new ArrayList<Metric<?>>();
		for (Metric<?> metric : metricRepository.findAll()) {
			if (metric.getName().startsWith(COUNTER_PREFIX + stepExecutionIdentifier)) {
				if (metric.getValue() instanceof Long) {
					// "batch."+ stepExecutionIdentifier is removed from the key before insertion in Step-ExecutionContext
					String key = metric.getName().substring((COUNTER_PREFIX + stepExecutionIdentifier).length() + 1);
					// Values from former failed StepExecution runs are added
					Long newValue = (Long) metric.getValue();
					if (stepExecution.getExecutionContext().containsKey(key)) {
						Long oldValue = stepExecution.getExecutionContext().getLong(key);
						newValue += oldValue;
						metric = metric.set(newValue);
					}
					stepExecution.getExecutionContext().putLong(key, newValue);
				}
				metrics.add(metric);
				if (deleteMetricsOnStepFinish) {
					metricRepository.reset(metric.getName());
				}
			}
		}
		return metrics;
	}

	private List<RichGauge> exportBatchRichGauges(StepExecution stepExecution) {
		String stepExecutionIdentifier = getStepExecutionIdentifier();
		List<RichGauge> gauges = new ArrayList<RichGauge>();
		for (RichGauge gauge : richGaugeRepository.findAll()) {
			if (gauge.getName().startsWith(GAUGE_PREFIX + stepExecutionIdentifier)) {
				// "batch."+ stepExecutionIdentifier is removed from the key before insertion in Step-ExecutionContext
				stepExecution.getExecutionContext().put(gauge.getName().substring((GAUGE_PREFIX + stepExecutionIdentifier).length() + 1), gauge);
				gauges.add(gauge);
				if (deleteMetricsOnStepFinish) {
					richGaugeRepository.reset(gauge.getName());
				}
			}
		}
		return gauges;
	}

	private static class SimpleMetricsOutputFormatter implements MetricsOutputFormatter {

		@Override
		public String format(List<RichGauge> gauges, List<Metric<?>> metrics) {
			StringBuilder builder = new StringBuilder("\n########## Metrics Start ##########\n");
			for (RichGauge gauge : gauges) {
				builder.append(gauge.toString() + "\n");
			}
			for (Metric<?> metric : metrics) {
				builder.append(metric.toString() + "\n");
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
		JobExecution jobExecution = stepExecution.getJobExecution();
		return stepContext.getJobName() + "." + jobExecution.getId() + "." + stepExecution.getStepName();
	}

}
