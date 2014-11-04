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

import org.springframework.batch.core.SkipListener;

import de.codecentric.batch.metrics.Item;
import de.codecentric.batch.metrics.MetricNames;
import de.codecentric.batch.metrics.business.BatchMetrics;

/**
 * @author Tobias Flohre
 */
public class MetricsTestSkipListener implements SkipListener<Item, Item> {

	private BatchMetrics businessMetrics;

	public MetricsTestSkipListener(BatchMetrics businessMetrics) {
		this.businessMetrics = businessMetrics;
	}

	@Override
	public void onSkipInRead(Throwable t) {
		businessMetrics.increment(MetricNames.SKIP_IN_READ_COUNT.getName());
		businessMetrics.submit(MetricNames.SKIP_IN_READ_GAUGE.getName(), 5);
	}

	@Override
	public void onSkipInWrite(Item item, Throwable t) {
		businessMetrics.increment(MetricNames.SKIP_IN_WRITE_COUNT.getName());
		businessMetrics.submit(MetricNames.SKIP_IN_WRITE_GAUGE.getName(), 5);
	}

	@Override
	public void onSkipInProcess(Item item, Throwable t) {
		businessMetrics.increment(MetricNames.SKIP_IN_PROCESS_COUNT.getName());
		businessMetrics.submit(MetricNames.SKIP_IN_PROCESS_GAUGE.getName(), 5);
	}

}
