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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;

import de.codecentric.batch.metrics.Action;
import de.codecentric.batch.metrics.BatchMetrics;
import de.codecentric.batch.metrics.Item;
import de.codecentric.batch.metrics.MetricNames;
import de.codecentric.batch.metrics.MetricsTestException;

/**
 * @author Tobias Flohre
 */
public class MetricsTestItemReader implements ItemStreamReader<Item> {
	
	private static final Log log = LogFactory.getLog(MetricsTestItemReader.class);

	private ItemStreamReader<Item> delegate;
	private BatchMetrics businessMetrics;
	private boolean readerTransactional;

	public MetricsTestItemReader(ItemStreamReader<Item> delegate,
			BatchMetrics businessMetrics, boolean readerTransactional) {
		this.delegate = delegate;
		this.businessMetrics = businessMetrics;
		this.readerTransactional = readerTransactional;
	}

	@Override
	public Item read() throws Exception, UnexpectedInputException, ParseException,
			NonTransientResourceException {
		if (readerTransactional){
			businessMetrics.increment(MetricNames.READ_COUNT.getName());
			businessMetrics.submit(MetricNames.READ_GAUGE.getName(), 5);
		} else {
			businessMetrics.incrementNonTransactional(MetricNames.READ_COUNT.getName());
			businessMetrics.submitNonTransactional(MetricNames.READ_GAUGE.getName(), 5);
		}
		Item item = delegate.read();
		if (item != null && item.getActions().contains(Action.FAIL_ON_READ)){
			throw new MetricsTestException(Action.FAIL_ON_READ);
		}
		log.debug("Read item: "+item);
		return item;
	}

	@Override
	public void open(ExecutionContext executionContext)
			throws ItemStreamException {
		businessMetrics.increment(MetricNames.STREAM_OPEN_COUNT.getName());
		businessMetrics.submit(MetricNames.STREAM_OPEN_GAUGE.getName(), 5);
		delegate.open(executionContext);
	}

	@Override
	public void update(ExecutionContext executionContext)
			throws ItemStreamException {
		businessMetrics.increment(MetricNames.STREAM_UPDATE_COUNT.getName());
		businessMetrics.submit(MetricNames.STREAM_UPDATE_GAUGE.getName(), 5);
		delegate.update(executionContext);
	}

	@Override
	public void close() throws ItemStreamException {
		// close is called after step processing, after StepExecutionListener execution,
		// that's why we don't see these counters in the log.
		businessMetrics.increment(MetricNames.STREAM_CLOSE_COUNT.getName());
		businessMetrics.submit(MetricNames.STREAM_CLOSE_GAUGE.getName(), 5);
		delegate.close();
	}

}
