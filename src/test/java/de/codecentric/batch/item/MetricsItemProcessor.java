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
package de.codecentric.batch.item;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.item.ItemProcessor;

import de.codecentric.batch.metrics.BatchMetrics;


/**
 * Dummy {@link ItemProcessor} which logs data it receives and increments a counter.
 */
public class MetricsItemProcessor implements ItemProcessor<String,String> {

	private static final Log log = LogFactory.getLog(MetricsItemProcessor.class);
	
	private BatchMetrics businessMetrics;
	
	public MetricsItemProcessor(BatchMetrics businessMetrics) {
		this.businessMetrics = businessMetrics;
	}

	public String process(String item) throws Exception {
		log.info(item);
		businessMetrics.increment("processor");
		return item;
	}

}
