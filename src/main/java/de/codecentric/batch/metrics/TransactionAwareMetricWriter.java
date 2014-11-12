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

import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.writer.Delta;
import org.springframework.boot.actuate.metrics.writer.MetricWriter;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * This {@link CounterService} delays actions until the transactions has been successfully
 * committed. If the transaction is rolled back, the changes are not applied.
 * Actions outside of transactions are applied immediately.
 * 
 * @author Tobias Flohre, Dennis Schulte
 * 
 */
public class TransactionAwareMetricWriter extends TransactionSynchronizationAdapter implements MetricWriter {

	private MetricWriter delegate;
	private ThreadLocal<CounterContainer> counterContainer;
	private ThreadLocal<GaugeContainer> gaugeContainer;
	private final Object serviceKey;

	public TransactionAwareMetricWriter(MetricWriter delegate) {
		super();
		this.delegate = delegate;
		this.serviceKey = new Object();
		this.counterContainer = new ThreadLocal<CounterContainer>();
		this.gaugeContainer = new ThreadLocal<GaugeContainer>();
	}

	@Override
	public void increment(Delta<?> delta) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			initializeMetricContainerAndRegisterTransactionSynchronizationIfNecessary();
			counterContainer.get().incrementations.add(delta);
		} else {
			delegate.increment(delta);
		}
	}

	private void initializeMetricContainerAndRegisterTransactionSynchronizationIfNecessary() {
		if (!TransactionSynchronizationManager.hasResource(serviceKey)) {
			TransactionSynchronizationManager.bindResource(serviceKey, new StringBuffer());
			TransactionSynchronizationManager.registerSynchronization(this);
		}
		if (counterContainer.get() == null) {
			counterContainer.set(new CounterContainer());
		}
		if (gaugeContainer.get() == null) {
			gaugeContainer.set(new GaugeContainer());
		}
	}

	@Override
	public void set(Metric<?> value) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			initializeMetricContainerAndRegisterTransactionSynchronizationIfNecessary();
			gaugeContainer.get().gauges.add(value);
		} else {
			delegate.set(value);
		}
	}

	@Override
	public void reset(String metricName) {
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			initializeMetricContainerAndRegisterTransactionSynchronizationIfNecessary();
			counterContainer.get().resets.add(metricName);
		} else {
			delegate.reset(metricName);
		}
	}

	@Override
	public void afterCompletion(int status) {
		if (status == STATUS_COMMITTED) {
			CounterContainer currentCounterContainer = counterContainer.get();
			for (Delta<?> incrementation : currentCounterContainer.incrementations) {
				delegate.increment(incrementation);
			}
			for (String reset : currentCounterContainer.resets) {
				delegate.reset(reset);
			}
			GaugeContainer currentGaugeContainer = gaugeContainer.get();
			for (Metric<?> gauge : currentGaugeContainer.gauges) {
				delegate.set(gauge);
			}
		}
		counterContainer.remove();
		gaugeContainer.remove();
		if (TransactionSynchronizationManager.hasResource(serviceKey)) {
			TransactionSynchronizationManager.unbindResource(serviceKey);
		}
	}

	private static class CounterContainer {

		List<Delta> incrementations = new ArrayList<Delta>();
		List<String> resets = new ArrayList<String>();

	}

	private static class GaugeContainer {

		List<Metric> gauges = new ArrayList<Metric>();

	}

}
