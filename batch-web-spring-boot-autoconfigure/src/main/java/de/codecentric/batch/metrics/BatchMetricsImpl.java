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
package de.codecentric.batch.metrics;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * See {@link BatchMetrics} for documentation.
 * 
 * @author Tobias Flohre
 * @author Dennis Schulte
 */
public class BatchMetricsImpl extends TransactionSynchronizationAdapter implements BatchMetrics {

	private static final Logger LOGGER = LoggerFactory.getLogger(BatchMetricsImpl.class);

	private ThreadLocal<MetricContainer> metricContainer;

	private final Object serviceKey;

	public BatchMetricsImpl() {
		this.serviceKey = new Object();
		this.metricContainer = new ThreadLocal<MetricContainer>();
	}

	@Override
	public void increment(String metricName) {
		increment(metricName, 1L);

	}

	@Override
	public void increment(String metricName, Long value) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			initializeMetricContainerAndRegisterTransactionSynchronizationIfNecessary();
			metricContainer.get().metrics.add(Pair.of(metricName, value));
		} else {
			incrementNonTransactional(metricName, value);
		}
	}

	@Override
	public void decrement(String metricName) {
		decrement(metricName, -1L);
	}

	@Override
	public void decrement(String metricName, Long value) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			initializeMetricContainerAndRegisterTransactionSynchronizationIfNecessary();
			metricContainer.get().metrics.add(Pair.of(metricName, -value));
		} else {
			decrementNonTransactional(metricName, value);
		}
	}

	@Override
	public void reset(String metricName) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			initializeMetricContainerAndRegisterTransactionSynchronizationIfNecessary();
			metricContainer.get().metrics.add(Pair.of(metricName, (Number) null));
		} else {
			resetNonTransactional(metricName);
		}
	}

	@Override
	public void submit(String metricName, double value) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			initializeMetricContainerAndRegisterTransactionSynchronizationIfNecessary();
			metricContainer.get().metrics.add(Pair.of(metricName, value));
		} else {
			set(metricName, value);
		}
	}

	@Override
	public void incrementNonTransactional(String metricName) {
		incrementNonTransactional(metricName, 1L);
	}

	@Override
	public void incrementNonTransactional(String metricName, Long value) {
		modifyCounter(metricName, value);
	}

	@Override
	public void decrementNonTransactional(String metricName) {
		decrementNonTransactional(metricName, -1L);
	}

	@Override
	public void decrementNonTransactional(String metricName, Long value) {
		modifyCounter(metricName, -value);
	}

	@Override
	public void resetNonTransactional(String metricName) {
		remove(metricName);
	}

	@Override
	public void submitNonTransactional(String metricName, double value) {
		set(metricName, value);
	}

	@Override
	public void afterCompletion(int status) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Entered afterCompletion with status {}.", status);
		}
		if (status == STATUS_COMMITTED) {
			MetricContainer currentMetricContainer = metricContainer.get();
			for (Pair<String, ? extends Number> metric : currentMetricContainer.metrics) {
				if (metric.getRight() instanceof Long) {
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("Increment {}.", metric);
					}
					incrementNonTransactional(metric.getLeft(), (Long) metric.getRight());
				} else if (metric.getRight() instanceof Double) {
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("Gauge {}.", metric);
					}
					set(metric.getLeft(), (Double) metric.getRight());
				} else if (metric.getRight() == null) {
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("Reset {}.", metric);
					}
					remove(metric.getLeft());
				}
			}
		}
		metricContainer.remove();
		if (TransactionSynchronizationManager.hasResource(serviceKey)) {
			TransactionSynchronizationManager.unbindResource(serviceKey);
		}
	}

	synchronized private void modifyCounter(String metricName, Long value) {
		StepExecution stepExecution = getStepExecution();
		Long oldValue = 0L;
		if (stepExecution.getExecutionContext().containsKey(metricName)) {
			oldValue = stepExecution.getExecutionContext().getLong(metricName);
		}
		stepExecution.getExecutionContext().put(metricName, oldValue + value);
	}

	private void remove(String metricName) {
		StepExecution stepExecution = getStepExecution();
		if (stepExecution.getExecutionContext().containsKey(metricName)) {
			stepExecution.getExecutionContext().remove(metricName);
		}
	}

	private void set(String metricName, double value) {
		StepExecution stepExecution = getStepExecution();
		stepExecution.getExecutionContext().put(metricName, value);
	}

	private static class MetricContainer {

		List<Pair<String, ? extends Number>> metrics = new ArrayList<Pair<String, ? extends Number>>();
	}

	private StepExecution getStepExecution() {
		if (StepSynchronizationManager.getContext() != null) {
			return StepSynchronizationManager.getContext().getStepExecution();
		}
		return null;
	}

	private void initializeMetricContainerAndRegisterTransactionSynchronizationIfNecessary() {
		if (!TransactionSynchronizationManager.hasResource(serviceKey)) {
			TransactionSynchronizationManager.bindResource(serviceKey, new StringBuffer());
			TransactionSynchronizationManager.registerSynchronization(this);
		}
		if (metricContainer.get() == null) {
			metricContainer.set(new MetricContainer());
		}
	}

}
