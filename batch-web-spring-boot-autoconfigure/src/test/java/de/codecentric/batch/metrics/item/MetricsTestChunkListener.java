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

import org.springframework.batch.core.ChunkListener;
import org.springframework.batch.core.scope.context.ChunkContext;

import de.codecentric.batch.metrics.BatchMetrics;
import de.codecentric.batch.metrics.MetricNames;

/**
 * @author Tobias Flohre
 */
public class MetricsTestChunkListener implements ChunkListener {

	private BatchMetrics businessMetrics;

	public MetricsTestChunkListener(BatchMetrics businessMetrics) {
		this.businessMetrics = businessMetrics;
	}

	@Override
	public void beforeChunk(ChunkContext context) {
		businessMetrics.increment(MetricNames.BEFORE_CHUNK_COUNT.getName());
		businessMetrics.submit(MetricNames.BEFORE_CHUNK_GAUGE.getName(), 5);
	}

	@Override
	public void afterChunk(ChunkContext context) {
		businessMetrics.incrementNonTransactional(MetricNames.AFTER_CHUNK_COUNT.getName());
		businessMetrics.submitNonTransactional(MetricNames.AFTER_CHUNK_GAUGE.getName(), 5);
	}

	@Override
	public void afterChunkError(ChunkContext context) {
		businessMetrics.incrementNonTransactional(MetricNames.CHUNK_ERROR_COUNT.getName());
		businessMetrics.submitNonTransactional(MetricNames.CHUNK_ERROR_GAUGE.getName(), 5);
	}

}
