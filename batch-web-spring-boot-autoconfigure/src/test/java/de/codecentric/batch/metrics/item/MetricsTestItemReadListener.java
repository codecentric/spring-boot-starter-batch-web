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
package de.codecentric.batch.metrics.item;

import org.springframework.batch.core.ItemReadListener;

import de.codecentric.batch.metrics.Action;
import de.codecentric.batch.metrics.BatchMetrics;
import de.codecentric.batch.metrics.Item;
import de.codecentric.batch.metrics.MetricNames;
import de.codecentric.batch.metrics.MetricsTestException;

/**
 * @author Tobias Flohre
 */
public class MetricsTestItemReadListener implements ItemReadListener<Item> {

	private BatchMetrics businessMetrics;

	private boolean readerTransactional;

	public MetricsTestItemReadListener(BatchMetrics businessMetrics, boolean readerTransactional) {
		this.businessMetrics = businessMetrics;
		this.readerTransactional = readerTransactional;
	}

	@Override
	public void beforeRead() {
		if (readerTransactional) {
			businessMetrics.increment(MetricNames.BEFORE_READ_COUNT.getName());
			businessMetrics.submit(MetricNames.BEFORE_READ_GAUGE.getName(), 5);
		} else {
			businessMetrics.incrementNonTransactional(MetricNames.BEFORE_READ_COUNT.getName());
			businessMetrics.submitNonTransactional(MetricNames.BEFORE_READ_GAUGE.getName(), 5);
		}
	}

	@Override
	public void afterRead(Item item) {
		if (readerTransactional) {
			businessMetrics.increment(MetricNames.AFTER_READ_COUNT.getName());
			businessMetrics.submit(MetricNames.AFTER_READ_GAUGE.getName(), 5);
		} else {
			businessMetrics.incrementNonTransactional(MetricNames.AFTER_READ_COUNT.getName());
			businessMetrics.submitNonTransactional(MetricNames.AFTER_READ_GAUGE.getName(), 5);
		}
		if (item != null && item.getActions().contains(Action.FAIL_ON_AFTER_READ)) {
			throw new MetricsTestException(Action.FAIL_ON_AFTER_READ);
		}
	}

	@Override
	public void onReadError(Exception ex) {
		businessMetrics.incrementNonTransactional(MetricNames.READ_ERROR_COUNT.getName());
		businessMetrics.submitNonTransactional(MetricNames.READ_ERROR_GAUGE.getName(), 5);
	}

}
