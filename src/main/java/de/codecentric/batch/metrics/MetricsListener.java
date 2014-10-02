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
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.listener.StepExecutionListenerSupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.repository.MetricRepository;
import org.springframework.boot.actuate.metrics.rich.RichGauge;
import org.springframework.boot.actuate.metrics.rich.RichGaugeRepository;
import org.springframework.core.Ordered;

import de.codecentric.batch.listener.LoggingListener;

/**
 * This listener exports all metrics with the prefix 'counter.batch.{jobName}.{jobExecutionId}.{stepName}
 * and all gauges with the prefix 'gauge.batch.{jobName}.{jobExecutionId}.{stepName}' to the Step-
 * ExecutionContext without the prefix. All metrics and gauges are logged as well. For
 * overriding the default format of the logging a component implementing {@link MetricsOutputFormatter}
 * may be added to the ApplicationContext.
 * 
 * If deleteMetricsOnStepFinish is true, all metrics will be removed from Spring Boot's metric
 * framework when the job finishes and the metrics are written to the Step-ExecutionContext.
 * 
 * Counters are cumulated over several StepExecutions belonging to one Step in one JobInstance.
 * 
 * @author Tobias Flohre
 */
public class MetricsListener extends StepExecutionListenerSupport implements Ordered{

	private static final Log LOGGER = LogFactory.getLog(MetricsListener.class);
	
	private RichGaugeRepository richGaugeRepository;
	private MetricRepository metricRepository;
	private boolean deleteMetricsOnStepFinish;
	@Autowired(required=false)
	private MetricsOutputFormatter metricsOutputFormatter = new SimpleMetricsOutputFormatter();
	
	public MetricsListener(RichGaugeRepository richGaugeRepository,
			MetricRepository metricRepository, boolean deleteMetricsOnStepFinish) {
		this.richGaugeRepository = richGaugeRepository;
		this.metricRepository = metricRepository;
		this.deleteMetricsOnStepFinish = deleteMetricsOnStepFinish;
	}

	@Override
	public ExitStatus afterStep(StepExecution stepExecution) {
		List<RichGauge> gauges = exportBatchGauges(stepExecution);
		List<Metric<?>> metrics = exportBatchMetrics(stepExecution);
		LOGGER.info(metricsOutputFormatter.format(gauges, metrics));
		return null;
	}

	private List<Metric<?>> exportBatchMetrics(StepExecution stepExecution) {
		String stepExecutionIdentifier = MDC.get(LoggingListener.STEP_EXECUTION_IDENTIFIER);
		List<Metric<?>> metrics = new ArrayList<Metric<?>>();
		for (Metric<?> metric : metricRepository.findAll()) {
			if (metric.getName().startsWith("counter.batch." + stepExecutionIdentifier)) {
				if (metric.getValue() instanceof Long){
					// "batch."+ stepExecutionIdentifier is removed from the key before insertion in Step-ExecutionContext
					String key = metric.getName().substring(("counter.batch." + stepExecutionIdentifier).length()+1);
					// Values from former failed StepExecution runs are added
					Long newValue = (Long)metric.getValue();
					if (stepExecution.getExecutionContext().containsKey(key)){
						Long oldValue = stepExecution.getExecutionContext().getLong(key);
						newValue += oldValue;
						metric = metric.set(newValue);
					}
					stepExecution.getExecutionContext().putLong(key, newValue);
				}
				metrics.add(metric);
				if (deleteMetricsOnStepFinish){
					metricRepository.reset(metric.getName());
				}
			}
		}
		return metrics;
	}

	private List<RichGauge> exportBatchGauges(StepExecution stepExecution) {
		String stepExecutionIdentifier = MDC.get(LoggingListener.STEP_EXECUTION_IDENTIFIER);
		List<RichGauge> gauges = new ArrayList<RichGauge>();
		for (RichGauge gauge : richGaugeRepository.findAll()) {
			if (gauge.getName().startsWith("gauge.batch." + stepExecutionIdentifier)) {
				// "batch."+ stepExecutionIdentifier is removed from the key before insertion in Step-ExecutionContext
				stepExecution.getExecutionContext().put(gauge.getName().substring(("gauge.batch." + stepExecutionIdentifier).length()+1), gauge);
				gauges.add(gauge);
				if (deleteMetricsOnStepFinish){
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
		return Ordered.LOWEST_PRECEDENCE-1;
	}

}
