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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;

import de.codecentric.batch.metrics.Action;
import de.codecentric.batch.metrics.BatchMetrics;
import de.codecentric.batch.metrics.Item;
import de.codecentric.batch.metrics.MetricNames;
import de.codecentric.batch.metrics.MetricsTestException;

/**
 * @author Tobias Flohre
 */
public class MetricsTestItemProcessor implements ItemProcessor<Item, Item> {

	private static final Logger LOGGER = LoggerFactory.getLogger(MetricsTestItemProcessor.class);

	private BatchMetrics businessMetrics;

	private boolean processorTransactional;

	public MetricsTestItemProcessor(BatchMetrics businessMetrics, boolean processorTransactional) {
		this.businessMetrics = businessMetrics;
		this.processorTransactional = processorTransactional;
	}

	@Override
	public Item process(Item item) throws Exception {
		LOGGER.debug("Processed item: {}", item.toString());
		if (!processorTransactional || item.getActions().contains(Action.FILTER)) {
			businessMetrics.incrementNonTransactional(MetricNames.PROCESS_COUNT.getName());
			businessMetrics.submitNonTransactional(MetricNames.PROCESS_GAUGE.getName(), 5);
		} else {
			businessMetrics.increment(MetricNames.PROCESS_COUNT.getName());
			businessMetrics.submit(MetricNames.PROCESS_GAUGE.getName(), 5);
		}
		if (item.getActions().contains(Action.FAIL_ON_PROCESS)) {
			throw new MetricsTestException(Action.FAIL_ON_PROCESS);
		}
		if (item.getActions().contains(Action.FILTER)) {
			return null;
		}
		return item;
	}

}
