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
import org.slf4j.MDC;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.repository.MetricRepository;
import org.springframework.boot.actuate.metrics.rich.RichGauge;
import org.springframework.boot.actuate.metrics.rich.RichGaugeRepository;
import org.springframework.core.Ordered;

import de.codecentric.batch.listener.LoggingListener;

/**
 * @author Tobias Flohre
 */
public class MetricsListener extends JobExecutionListenerSupport implements Ordered{

	private static final Log LOGGER = LogFactory.getLog(MetricsListener.class);
	
	private RichGaugeRepository richGaugeRepository;
	private MetricRepository metricRepository;
	private boolean deleteMetricsOnJobFinish;
	@Autowired(required=false)
	private MetricsOutputFormatter metricsOutputFormatter = new SimpleMetricsOutputFormatter();

	public MetricsListener(RichGaugeRepository richGaugeRepository,
			MetricRepository metricRepository, boolean deleteMetricsOnJobFinish) {
		this.richGaugeRepository = richGaugeRepository;
		this.metricRepository = metricRepository;
		this.deleteMetricsOnJobFinish = deleteMetricsOnJobFinish;
	}

	@Override
	public void afterJob(JobExecution jobExecution) {
		List<RichGauge> gauges = exportBatchGauges(jobExecution);
		List<Metric<?>> metrics = exportBatchMetrics(jobExecution);
		LOGGER.info(metricsOutputFormatter.format(gauges, metrics));
	}

	private List<Metric<?>> exportBatchMetrics(JobExecution jobExecution) {
		String jobExecutionIdentifier = MDC.get(LoggingListener.JOB_EXECUTION_IDENTIFIER);
		List<Metric<?>> metrics = new ArrayList<Metric<?>>();
		for (Metric<?> metric : metricRepository.findAll()) {
			if (metric.getName().startsWith("counter.batch." + jobExecutionIdentifier)) {
				if (metric.getValue() instanceof Long){
					// "batch."+ jobExecutionIdentifier is removed from the key before insertion in Job-ExecutionContext
					String key = metric.getName().substring(("counter.batch." + jobExecutionIdentifier).length()+1);
					// Values from former failed JobExecution runs are added
					Long newValue = (Long)metric.getValue();
					if (jobExecution.getExecutionContext().containsKey(key)){
						Long oldValue = jobExecution.getExecutionContext().getLong(key);
						newValue += oldValue;
						metric = metric.set(newValue);
					}
					jobExecution.getExecutionContext().putLong(key, newValue);
				}
				metrics.add(metric);
				if (deleteMetricsOnJobFinish){
					metricRepository.reset(metric.getName());
				}
			}
		}
		return metrics;
	}

	private List<RichGauge> exportBatchGauges(JobExecution jobExecution) {
		String jobExecutionIdentifier = MDC.get(LoggingListener.JOB_EXECUTION_IDENTIFIER);
		List<RichGauge> gauges = new ArrayList<RichGauge>();
		for (RichGauge gauge : richGaugeRepository.findAll()) {
			if (gauge.getName().startsWith("gauge.batch." + jobExecutionIdentifier)) {
				// "batch."+ jobExecutionIdentifier is removed from the key before insertion in Job-ExecutionContext
				jobExecution.getExecutionContext().put(gauge.getName().substring(("gauge.batch." + jobExecutionIdentifier).length()+1), gauge);
				gauges.add(gauge);
				if (deleteMetricsOnJobFinish){
					richGaugeRepository.reset(gauge.getName());
				}
			}
		}
		return gauges;
	}
	
	private static class SimpleMetricsOutputFormatter implements MetricsOutputFormatter{

		@Override
		public String format(List<RichGauge> gauges, List<Metric<?>> metrics) {
			StringBuilder builder = new StringBuilder("\n########## Metrics Start ##########\n");
			for (RichGauge gauge: gauges){
				builder.append(gauge.toString()+"\n");
			}
			for (Metric<?> metric: metrics){
				builder.append(metric.toString()+"\n");
			}
			builder.append("########## Metrics End ############");
			return builder.toString();
		}
		
	}

	@Override
	public int getOrder() {
		return 0;
	}

}
