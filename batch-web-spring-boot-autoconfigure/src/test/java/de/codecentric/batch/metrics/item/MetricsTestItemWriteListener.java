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

import java.util.List;

import org.springframework.batch.core.ItemWriteListener;

import de.codecentric.batch.metrics.Action;
import de.codecentric.batch.metrics.BatchMetrics;
import de.codecentric.batch.metrics.Item;
import de.codecentric.batch.metrics.MetricNames;
import de.codecentric.batch.metrics.MetricsTestException;

/**
 * @author Tobias Flohre
 */
public class MetricsTestItemWriteListener implements ItemWriteListener<Item> {

	private BatchMetrics businessMetrics;

	public MetricsTestItemWriteListener(BatchMetrics businessMetrics) {
		this.businessMetrics = businessMetrics;
	}

	@Override
	public void beforeWrite(List<? extends Item> items) {
		for (Item item : items) {
			businessMetrics.increment(MetricNames.BEFORE_WRITE_COUNT.getName());
			businessMetrics.submit(MetricNames.BEFORE_WRITE_GAUGE.getName(), 5);
			if (item != null && item.getActions().contains(Action.FAIL_ON_BEFORE_WRITE)) {
				throw new MetricsTestException(Action.FAIL_ON_BEFORE_WRITE);
			}
		}
	}

	@Override
	public void afterWrite(List<? extends Item> items) {
		for (Item item : items) {
			businessMetrics.increment(MetricNames.AFTER_WRITE_COUNT.getName());
			businessMetrics.submit(MetricNames.AFTER_WRITE_GAUGE.getName(), 5);
			if (item != null && item.getActions().contains(Action.FAIL_ON_AFTER_WRITE)) {
				throw new MetricsTestException(Action.FAIL_ON_AFTER_WRITE);
			}
		}
	}

	@Override
	public void onWriteError(Exception exception, List<? extends Item> items) {
		for (Item item : items) {
			businessMetrics.incrementNonTransactional(MetricNames.WRITE_ERROR_COUNT.getName());
			businessMetrics.submitNonTransactional(MetricNames.WRITE_ERROR_GAUGE.getName(), 5);
			if (item != null && item.getActions().contains(Action.FAIL_ON_WRITE_ERROR)) {
				throw new MetricsTestException(Action.FAIL_ON_WRITE_ERROR);
			}
		}
	}

}
