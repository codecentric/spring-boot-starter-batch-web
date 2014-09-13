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

import java.util.Map.Entry;

import org.springframework.boot.actuate.metrics.GaugeService;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * This {@link GaugeService} delays actions until the transactions has been successfully
 * committed. If the transaction is rolled back, the changes are not applied.
 * Actions outside of transactions are ignored.
 * 
 * @author Tobias Flohre
 */
public class TransactionAwareGaugeService extends TransactionSynchronizationAdapter implements GaugeService {
	
	private GaugeService delegate;
	private ThreadLocal<MetricContainer> metricContainer;
	private final Object serviceKey;

	public TransactionAwareGaugeService(GaugeService delegate) {
		super();
		this.delegate = delegate;
		this.serviceKey = new Object();
		this.metricContainer = new ThreadLocal<MetricContainer>();
	}

	@Override
	public void submit(String metricName, double value) {
		if (TransactionSynchronizationManager.isSynchronizationActive()){
			initializeMetricContainerAndRegisterTransactionSynchronizationIfNecessary();
			metricContainer.get().gauges.put(metricName, value);
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
	public void afterCompletion(int status) {
		if (status == STATUS_COMMITTED){
			MetricContainer currentMetricContainer = metricContainer.get();
			for (Entry<String,Double> gauge: currentMetricContainer.gauges.entrySet()){
				delegate.submit(gauge.getKey(),gauge.getValue());
			}
		}
		metricContainer.remove();
	}

}
