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
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * This {@link CounterService} delays actions until the transactions has been successfully
 * committed. If the transaction is rolled back, the changes are not applied.
 * Actions outside of transactions are applied immediately.
 * 
 * @author Tobias Flohre
 */
public class TransactionAwareCounterService extends TransactionSynchronizationAdapter implements CounterService {
	
	private CounterService delegate;
	private ThreadLocal<CounterContainer> counterContainer;
	private final Object serviceKey;

	public TransactionAwareCounterService(CounterService delegate) {
		super();
		this.delegate = delegate;
		this.serviceKey = new Object();
		this.counterContainer = new ThreadLocal<CounterContainer>();
	}

	@Override
	public void increment(String metricName) {
		if (TransactionSynchronizationManager.isSynchronizationActive()){
			initializeMetricContainerAndRegisterTransactionSynchronizationIfNecessary();
			counterContainer.get().incrementations.add(metricName);
		} else {
			delegate.increment(metricName);
		}
	}

	private void initializeMetricContainerAndRegisterTransactionSynchronizationIfNecessary(){
		if (!TransactionSynchronizationManager.hasResource(serviceKey)) {
			TransactionSynchronizationManager.bindResource(serviceKey, new StringBuffer());
			TransactionSynchronizationManager.registerSynchronization(this);
		}
		if (counterContainer.get() == null){
			counterContainer.set(new CounterContainer());
		}
	}

	@Override
	public void decrement(String metricName) {
		if (TransactionSynchronizationManager.isSynchronizationActive()){
			initializeMetricContainerAndRegisterTransactionSynchronizationIfNecessary();
			counterContainer.get().decrementations.add(metricName);
		} else {
			delegate.decrement(metricName);
		}
	}

	@Override
	public void reset(String metricName) {
		if (TransactionSynchronizationManager.isSynchronizationActive()){
			initializeMetricContainerAndRegisterTransactionSynchronizationIfNecessary();
			counterContainer.get().resets.add(metricName);
		} else {
			delegate.reset(metricName);
		}
	}

	@Override
	public void afterCompletion(int status) {
		if (status == STATUS_COMMITTED){
			CounterContainer currentCounterContainer = counterContainer.get();
			for (String incrementation: currentCounterContainer.incrementations){
				delegate.increment(incrementation);
			}
			for (String decrementation: currentCounterContainer.decrementations){
				delegate.decrement(decrementation);
			}
			for (String reset: currentCounterContainer.resets){
				delegate.reset(reset);
			}
		}
		counterContainer.remove();
		if (TransactionSynchronizationManager.hasResource(serviceKey)) {
			TransactionSynchronizationManager.unbindResource(serviceKey);
		}
	}
	
	private static class CounterContainer {
		
		List<String> incrementations = new ArrayList<String>();
		List<String> decrementations = new ArrayList<String>();
		List<String> resets = new ArrayList<String>();
		
	}

	
}
