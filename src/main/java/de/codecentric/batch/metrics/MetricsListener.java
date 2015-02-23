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
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.listener.StepExecutionListenerSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.GaugeService;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.export.Exporter;
import org.springframework.boot.actuate.metrics.repository.MetricRepository;
import org.springframework.boot.actuate.metrics.rich.RichGauge;
import org.springframework.boot.actuate.metrics.rich.RichGaugeRepository;
import org.springframework.core.Ordered;

/**
 * This listener exports all metrics with the prefix 'counter.batch.{jobName}.{jobExecutionId}.{stepName}
 * and all gauges with the prefix 'gauge.batch.{jobName}.{stepName}' to the Step-
 * ExecutionContext without the prefix. All metrics and gauges are logged as well. For
 * overriding the default format of the logging a component implementing {@link MetricsOutputFormatter} may be added to the ApplicationContext.
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
	
	private GaugeService gaugeService;

	private RichGaugeRepository richGaugeRepository;
	private MetricRepository metricRepository;
	private List<Exporter> exporters;
	@Autowired(required = false)
	private MetricsOutputFormatter metricsOutputFormatter = new SimpleMetricsOutputFormatter();

	public MetricsListener(GaugeService gaugeService, RichGaugeRepository richGaugeRepository,
			MetricRepository metricRepository, List<Exporter> exporters) {
		this.gaugeService = gaugeService;
		this.richGaugeRepository = richGaugeRepository;
		this.metricRepository = metricRepository;
		this.exporters = exporters;
	}

	@Override
	public void beforeJob(JobExecution jobExecution) {
		// no action
	}

	@Override
	public ExitStatus afterStep(StepExecution stepExecution) {
		// Calculate step execution time
		// Why is stepExecution.getEndTime().getTime() not available here? (see AbstractStep)
		long stepDuration = System.currentTimeMillis() - stepExecution.getStartTime().getTime();
		gaugeService.submit(GAUGE_PREFIX + getStepExecutionIdentifier(stepExecution) + ".duration", stepDuration);
		long itemCount = stepExecution.getWriteCount() + stepExecution.getSkipCount();
		gaugeService.submit(GAUGE_PREFIX + getStepExecutionIdentifier(stepExecution) + ".item.count", itemCount);
		// Calculate execution time per item
		long durationPerItem = 0;
		if (itemCount > 0) {
			durationPerItem = stepDuration / itemCount;
		}
		gaugeService.submit(GAUGE_PREFIX + getStepExecutionIdentifier(stepExecution) + ".item.duration", durationPerItem);
		// Export metrics from StepExecution to MetricRepositories
		Set<Entry<String, Object>> metrics = stepExecution.getExecutionContext().entrySet();
		for (Entry<String, Object> metric : metrics) {
			if (metric.getValue() instanceof Long) {
				gaugeService.submit(GAUGE_PREFIX + getStepExecutionIdentifier(stepExecution) + "." + metric.getKey(), (Long) metric.getValue());
			} else if (metric.getValue() instanceof Double) {
				gaugeService.submit(GAUGE_PREFIX + getStepExecutionIdentifier(stepExecution) + "." + metric.getKey(), (Double) metric.getValue());
			}
		}
		return null;
	}

	@Override
	public void afterJob(JobExecution jobExecution) {
		long jobDuration = jobExecution.getEndTime().getTime() - jobExecution.getStartTime().getTime();
		gaugeService.submit(GAUGE_PREFIX + jobExecution.getJobInstance().getJobName() + ".duration", jobDuration);
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
		// Export Metrics to Console or Remote Systems
		LOGGER.info(metricsOutputFormatter.format(exportBatchRichGauges(), exportBatchMetrics()));
		// Codahale
		if (exporters != null) {
			for (Exporter exporter : exporters) {
				if (exporter != null) {
					LOGGER.info("Exporting Metrics with " + exporter.getClass().getName());
					exporter.export();
				}
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

	private String getStepExecutionIdentifier(StepExecution stepExecution) {
		return stepExecution.getJobExecution().getJobInstance().getJobName() + "." + stepExecution.getStepName();
	}
}
