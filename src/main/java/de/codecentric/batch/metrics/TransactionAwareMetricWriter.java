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
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.writer.Delta;
import org.springframework.boot.actuate.metrics.writer.MetricWriter;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * This {@link MetricWriter} delays actions until the transactions has been successfully
 * committed. If the transaction is rolled back, the changes are not applied.
 * Actions outside of transactions are applied immediately.
 * 
 * @author Tobias Flohre
 * @author Dennis Schulte
 */
public class TransactionAwareMetricWriter extends TransactionSynchronizationAdapter implements MetricWriter {

	private static final Log log = LogFactory.getLog(TransactionAwareMetricWriter.class);

	private MetricWriter delegate;
	private ThreadLocal<MetricContainer> metricContainer;
	private final Object serviceKey;

	public TransactionAwareMetricWriter(MetricWriter delegate) {
		super();
		this.delegate = delegate;
		this.serviceKey = new Object();
		this.metricContainer = new ThreadLocal<MetricContainer>();
	}

	@Override
	public void increment(Delta<?> delta) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			initializeMetricContainerAndRegisterTransactionSynchronizationIfNecessary();
			metricContainer.get().metrics.add(delta);
		} else {
			delegate.increment(delta);
		}
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

	@Override
	public void set(Metric<?> value) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			initializeMetricContainerAndRegisterTransactionSynchronizationIfNecessary();
			metricContainer.get().metrics.add(value);
		} else {
			delegate.set(value);
		}
	}

	@Override
	public void reset(String metricName) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			initializeMetricContainerAndRegisterTransactionSynchronizationIfNecessary();
			metricContainer.get().metrics.add(metricName);
		} else {
			delegate.reset(metricName);
		}
	}

	@Override
	public void afterCompletion(int status) {
		if (log.isDebugEnabled()) {
			log.debug("Entered afterCompletion with status " + status + ".");
		}
		if (status == STATUS_COMMITTED) {
			MetricContainer currentMetricContainer = metricContainer.get();
			for (Object metric : currentMetricContainer.metrics) {
				if (metric instanceof Delta) {
					if (log.isDebugEnabled()) {
						log.debug("Increment " + metric + ".");
					}
					delegate.increment((Delta<?>) metric);
				} else if (metric instanceof Metric) {
					if (log.isDebugEnabled()) {
						log.debug("Gauge " + metric + ".");
					}
					delegate.set((Metric<?>) metric);
				} else if (metric instanceof String) {
					if (log.isDebugEnabled()) {
						log.debug("Reset " + metric + ".");
					}
					delegate.reset((String) metric);
				}
			}
		}
		metricContainer.remove();
		if (TransactionSynchronizationManager.hasResource(serviceKey)) {
			TransactionSynchronizationManager.unbindResource(serviceKey);
		}
	}

	private static class MetricContainer {
		List<Object> metrics = new ArrayList<Object>();
	}

}
