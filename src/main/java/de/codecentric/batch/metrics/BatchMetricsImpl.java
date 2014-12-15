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

import org.slf4j.MDC;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.writer.Delta;
import org.springframework.boot.actuate.metrics.writer.MetricWriter;

import de.codecentric.batch.listener.LoggingListener;

/**
 * See {@link BatchMetrics} for documentation.
 * 
 * @author Tobias Flohre
 */
public class BatchMetricsImpl implements BatchMetrics {

	private MetricWriter metricWriter;
	private MetricWriter transactionAwareMetricWriter;

	public BatchMetricsImpl(MetricWriter metricWriter) {
		this.metricWriter = metricWriter;
		this.transactionAwareMetricWriter = new TransactionAwareMetricWriter(metricWriter);
	}

	@Override
	public void increment(String metricName) {
		transactionAwareMetricWriter.increment(new Delta<Long>(wrapCounter(metricName), 1L));
	}

	@Override
	public void increment(String metricName, Long value) {
		transactionAwareMetricWriter.increment(new Delta<Long>(wrapCounter(metricName), value));
	}

	@Override
	public void decrement(String metricName) {
		transactionAwareMetricWriter.increment(new Delta<Long>(wrapCounter(metricName), -1L));
	}

	@Override
	public void decrement(String metricName, Long value) {
		transactionAwareMetricWriter.increment(new Delta<Long>(wrapCounter(metricName), -value));
	}

	@Override
	public void reset(String metricName) {
		transactionAwareMetricWriter.reset(wrapCounter(metricName));
	}

	@Override
	public void submit(String metricName, double value) {
		transactionAwareMetricWriter.set(new Metric<Double>(wrapGauge(metricName), value));
	}

	@Override
	public void incrementNonTransactional(String metricName) {
		metricWriter.increment(new Delta<Long>(wrapCounter(metricName), 1L));
	}

	@Override
	public void incrementNonTransactional(String metricName, Long value) {
		metricWriter.increment(new Delta<Long>(wrapCounter(metricName), value));
	}

	@Override
	public void decrementNonTransactional(String metricName) {
		metricWriter.increment(new Delta<Long>(wrapCounter(metricName), -1L));
	}

	@Override
	public void decrementNonTransactional(String metricName, Long value) {
		metricWriter.increment(new Delta<Long>(wrapCounter(metricName), -value));
	}

	@Override
	public void resetNonTransactional(String metricName) {
		metricWriter.reset(wrapCounter(metricName));
	}

	@Override
	public void submitNonTransactional(String metricName, double value) {
		metricWriter.set(new Metric<Double>(wrapGauge(metricName), value));
	}
	
	private String wrapCounter(String metricName) {
		return "counter." + "batch." + MDC.get(LoggingListener.STEP_EXECUTION_IDENTIFIER) + "." + metricName;
	}
	
	private String wrapGauge(String metricName) {
		return "gauge." + "batch." + MDC.get(LoggingListener.STEP_EXECUTION_IDENTIFIER) + "." + metricName;
	}

}
