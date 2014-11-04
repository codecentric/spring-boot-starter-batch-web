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
package de.codecentric.batch.metrics.item;

import org.springframework.batch.core.ItemProcessListener;

import de.codecentric.batch.metrics.Action;
import de.codecentric.batch.metrics.Item;
import de.codecentric.batch.metrics.MetricNames;
import de.codecentric.batch.metrics.MetricsTestException;
import de.codecentric.batch.metrics.business.BatchMetrics;

/**
 * @author Tobias Flohre
 */
public class MetricsTestItemProcessListener implements ItemProcessListener<Item, Item> {

	private BatchMetrics businessMetrics;
	private boolean processorTransactional;

	public MetricsTestItemProcessListener(BatchMetrics businessMetrics,
			boolean processorTransactional) {
		this.businessMetrics = businessMetrics;
		this.processorTransactional = processorTransactional;
	}

	@Override
	public void beforeProcess(Item item) {
		if (processorTransactional){
			businessMetrics.increment(MetricNames.BEFORE_PROCESS_COUNT.getName());
			businessMetrics.submit(MetricNames.BEFORE_PROCESS_GAUGE.getName(), 5);
		} else {
			businessMetrics.incrementNonTransactional(MetricNames.BEFORE_PROCESS_COUNT.getName());
			businessMetrics.submitNonTransactional(MetricNames.BEFORE_PROCESS_GAUGE.getName(), 5);
		}
		if (item != null && item.getActions().contains(Action.FAIL_ON_BEFORE_PROCESS)){
			throw new MetricsTestException(Action.FAIL_ON_BEFORE_PROCESS);
		}
	}

	@Override
	public void afterProcess(Item item, Item result) {
		if (processorTransactional){
			businessMetrics.increment(MetricNames.AFTER_PROCESS_COUNT.getName());
			businessMetrics.submit(MetricNames.AFTER_PROCESS_GAUGE.getName(), 5);
		} else {
			businessMetrics.incrementNonTransactional(MetricNames.AFTER_PROCESS_COUNT.getName());
			businessMetrics.submitNonTransactional(MetricNames.AFTER_PROCESS_GAUGE.getName(), 5);
		}
		if (item != null && item.getActions().contains(Action.FAIL_ON_AFTER_PROCESS)){
			throw new MetricsTestException(Action.FAIL_ON_AFTER_PROCESS);
		}
	}

	@Override
	public void onProcessError(Item item, Exception e) {
		businessMetrics.incrementNonTransactional(MetricNames.PROCESS_ERROR_COUNT.getName());
		businessMetrics.submitNonTransactional(MetricNames.PROCESS_ERROR_GAUGE.getName(), 5);
		if (item != null && item.getActions().contains(Action.FAIL_ON_PROCESS_ERROR)){
			throw new MetricsTestException(Action.FAIL_ON_PROCESS_ERROR);
		}
	}

}
