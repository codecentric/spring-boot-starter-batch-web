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

import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * This {@link CounterService} delays actions until the transactions has been successfully
 * committed. If the transaction is rolled back, the changes are not applied.
 * Counting outside of transactions is ignored.
 * 
 * @author Tobias Flohre
 */
public class TransactionAwareCounterService extends TransactionSynchronizationAdapter implements CounterService {
	
	private CounterService delegate;
	private ThreadLocal<MetricContainer> metricContainer;
	private final Object serviceKey;

	public TransactionAwareCounterService(CounterService delegate) {
		super();
		this.delegate = delegate;
		this.serviceKey = new Object();
		this.metricContainer = new ThreadLocal<MetricContainer>();
	}

	@Override
	public void increment(String metricName) {
		if (TransactionSynchronizationManager.isSynchronizationActive()){
			initializeMetricContainerAndRegisterTransactionSynchronizationIfNecessary();
			metricContainer.get().incrementations.add(metricName);
		}
	}

	private void initializeMetricContainerAndRegisterTransactionSynchronizationIfNecessary(){
		if (!TransactionSynchronizationManager.hasResource(serviceKey)) {
			TransactionSynchronizationManager.bindResource(serviceKey, new StringBuffer());
			TransactionSynchronizationManager.registerSynchronization(this);
		}
		if (metricContainer.get() == null){
			metricContainer.set(new MetricContainer());
		}
	}

	@Override
	public void decrement(String metricName) {
		if (TransactionSynchronizationManager.isSynchronizationActive()){
			initializeMetricContainerAndRegisterTransactionSynchronizationIfNecessary();
			metricContainer.get().decrementations.add(metricName);
		}
	}

	@Override
	public void reset(String metricName) {
		if (TransactionSynchronizationManager.isSynchronizationActive()){
			initializeMetricContainerAndRegisterTransactionSynchronizationIfNecessary();
			metricContainer.get().resets.add(metricName);
		}
	}

	@Override
	public void afterCompletion(int status) {
		if (status == STATUS_COMMITTED){
			MetricContainer currentMetricContainer = metricContainer.get();
			for (String incrementation: currentMetricContainer.incrementations){
				delegate.increment(incrementation);
			}
			for (String decrementation: currentMetricContainer.decrementations){
				delegate.decrement(decrementation);
			}
			for (String reset: currentMetricContainer.resets){
				delegate.reset(reset);
			}
		}
		metricContainer.remove();
	}
	
}
