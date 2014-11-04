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

import java.util.List;
import java.util.Map.Entry;

import org.springframework.boot.actuate.metrics.GaugeService;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * This {@link GaugeService} delays actions until the transactions has been successfully
 * committed. If the transaction is rolled back, the changes are not applied.
 * Actions outside of transactions are applied immediately.
 * 
 * @author Tobias Flohre
 */
public class TransactionAwareGaugeService extends TransactionSynchronizationAdapter implements GaugeService {
	
	private GaugeService delegate;
	private ThreadLocal<GaugeContainer> gaugeContainer;
	private final Object serviceKey;

	public TransactionAwareGaugeService(GaugeService delegate) {
		super();
		this.delegate = delegate;
		this.serviceKey = new Object();
		this.gaugeContainer = new ThreadLocal<GaugeContainer>();
	}

	@Override
	public void submit(String metricName, double value) {
		if (TransactionSynchronizationManager.isSynchronizationActive()){
			initializeMetricContainerAndRegisterTransactionSynchronizationIfNecessary();
			gaugeContainer.get().gauges.add(metricName, value);
		} else {
			delegate.submit(metricName, value);
		}
	}
	
	private void initializeMetricContainerAndRegisterTransactionSynchronizationIfNecessary(){
		if (!TransactionSynchronizationManager.hasResource(serviceKey)) {
			TransactionSynchronizationManager.bindResource(serviceKey, new StringBuffer());
			TransactionSynchronizationManager.registerSynchronization(this);
		}
		if (gaugeContainer.get() == null){
			gaugeContainer.set(new GaugeContainer());
		}
	}

	@Override
	public void afterCompletion(int status) {
		if (status == STATUS_COMMITTED){
			GaugeContainer currentGaugeContainer = gaugeContainer.get();
			for (Entry<String,List<Double>> gauge: currentGaugeContainer.gauges.entrySet()){
				for (Double value: gauge.getValue()){
					delegate.submit(gauge.getKey(),value);
				}
			}
		}
		gaugeContainer.remove();
		if (TransactionSynchronizationManager.hasResource(serviceKey)) {
			TransactionSynchronizationManager.unbindResource(serviceKey);
		}
	}

	private static class GaugeContainer {
		
		MultiValueMap<String, Double> gauges = new LinkedMultiValueMap<String, Double>();

	}

}
