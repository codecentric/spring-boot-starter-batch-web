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
package de.codecentric.batch.test.metrics;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.assertj.core.api.Assertions;
import org.hamcrest.MatcherAssert;
import org.springframework.batch.item.ExecutionContext;

import de.codecentric.batch.metrics.MetricNames;

/**
 * @author Tobias Flohre
 */
public class MetricValidator {

	private ExecutionContext executionContext;

	private boolean validateGauge = true;

	private long beforeChunkCount;

	private long afterChunkCount;

	private long chunkErrorCount;

	private long streamOpenCount;

	private long streamUpdateCount;

	private long streamCloseCount;

	private long beforeReadCount;

	private long readCount;

	private long afterReadCount;

	private long readErrorCount;

	private long beforeProcessCount;

	private long processCount;

	private long afterProcessCount;

	private long processErrorCount;

	private long beforeWriteCount;

	private long writeCount;

	private long afterWriteCount;

	private long writeErrorCount;

	private long skipInReadCount;

	private long skipInProcessCount;

	private long skipInWriteCount;

	public void validate() {
		assertThat(executionContext.getLong(MetricNames.BEFORE_CHUNK_COUNT.getName(), 0L), is(beforeChunkCount));
		validateGauge(MetricNames.BEFORE_CHUNK_GAUGE, beforeChunkCount);
		assertThat(executionContext.getLong(MetricNames.STREAM_OPEN_COUNT.getName(), 0L), is(streamOpenCount));
		validateGauge(MetricNames.STREAM_OPEN_GAUGE, streamOpenCount);
		assertThat(executionContext.getLong(MetricNames.STREAM_UPDATE_COUNT.getName(), 0L), is(streamUpdateCount));
		validateGauge(MetricNames.STREAM_UPDATE_GAUGE, streamUpdateCount);
		// close is called after step processing, after StepExecutionListener execution,
		// that's why we don't see these counters here.
		assertThat(executionContext.getLong(MetricNames.STREAM_CLOSE_COUNT.getName(), 0L), is(streamCloseCount));
		validateGauge(MetricNames.STREAM_CLOSE_GAUGE, streamCloseCount);
		assertThat(executionContext.getLong(MetricNames.BEFORE_READ_COUNT.getName(), 0L), is(beforeReadCount));
		validateGauge(MetricNames.BEFORE_READ_GAUGE, beforeReadCount);
		assertThat(executionContext.getLong(MetricNames.READ_COUNT.getName(), 0L), is(readCount));
		validateGauge(MetricNames.READ_GAUGE, readCount);
		assertThat(executionContext.getLong(MetricNames.AFTER_READ_COUNT.getName(), 0L), is(afterReadCount));
		validateGauge(MetricNames.AFTER_READ_GAUGE, afterReadCount);
		assertThat(executionContext.getLong(MetricNames.READ_ERROR_COUNT.getName(), 0L), is(readErrorCount));
		validateGauge(MetricNames.READ_ERROR_GAUGE, readErrorCount);
		assertThat(executionContext.getLong(MetricNames.BEFORE_PROCESS_COUNT.getName(), 0L), is(beforeProcessCount));
		validateGauge(MetricNames.BEFORE_PROCESS_GAUGE, beforeProcessCount);
		assertThat(executionContext.getLong(MetricNames.PROCESS_COUNT.getName(), 0L), is(processCount));
		validateGauge(MetricNames.PROCESS_GAUGE, processCount);
		assertThat(executionContext.getLong(MetricNames.AFTER_PROCESS_COUNT.getName(), 0L), is(afterProcessCount));
		validateGauge(MetricNames.AFTER_PROCESS_GAUGE, afterProcessCount);
		assertThat(executionContext.getLong(MetricNames.PROCESS_ERROR_COUNT.getName(), 0L), is(processErrorCount));
		validateGauge(MetricNames.PROCESS_ERROR_GAUGE, processErrorCount);
		assertThat(executionContext.getLong(MetricNames.BEFORE_WRITE_COUNT.getName(), 0L), is(beforeWriteCount));
		validateGauge(MetricNames.BEFORE_WRITE_GAUGE, beforeWriteCount);
		assertThat(executionContext.getLong(MetricNames.WRITE_COUNT.getName(), 0L), is(writeCount));
		validateGauge(MetricNames.WRITE_GAUGE, writeCount);
		assertThat(executionContext.getLong(MetricNames.AFTER_WRITE_COUNT.getName(), 0L), is(afterWriteCount));
		validateGauge(MetricNames.AFTER_WRITE_GAUGE, afterWriteCount);
		assertThat(executionContext.getLong(MetricNames.WRITE_ERROR_COUNT.getName(), 0L), is(writeErrorCount));
		validateGauge(MetricNames.WRITE_ERROR_GAUGE, writeErrorCount);
		assertThat(executionContext.getLong(MetricNames.AFTER_CHUNK_COUNT.getName(), 0L), is(afterChunkCount));
		validateGauge(MetricNames.AFTER_CHUNK_GAUGE, afterChunkCount);
		assertThat(executionContext.getLong(MetricNames.CHUNK_ERROR_COUNT.getName(), 0L), is(chunkErrorCount));
		validateGauge(MetricNames.CHUNK_ERROR_GAUGE, chunkErrorCount);
		assertThat(executionContext.getLong(MetricNames.SKIP_IN_READ_COUNT.getName(), 0L), is(skipInReadCount));
		validateGauge(MetricNames.SKIP_IN_READ_GAUGE, skipInReadCount);
		assertThat(executionContext.getLong(MetricNames.SKIP_IN_PROCESS_COUNT.getName(), 0L), is(skipInProcessCount));
		validateGauge(MetricNames.SKIP_IN_PROCESS_GAUGE, skipInProcessCount);
		assertThat(executionContext.getLong(MetricNames.SKIP_IN_WRITE_COUNT.getName(), 0L), is(skipInWriteCount));
		validateGauge(MetricNames.SKIP_IN_WRITE_GAUGE, skipInWriteCount);
	}

	private void validateGauge(MetricNames metricName, long count) {
		if (validateGauge) {
			Double gauge = (Double) executionContext.get(metricName.getName());
			if (count == 0) {
				assertThat(gauge, nullValue());
			} else {
				assertThat(gauge, notNullValue());
				assertThat(gauge, is(5.0));
			}
		}
	}

	public void setExecutionContext(ExecutionContext executionContext) {
		this.executionContext = executionContext;
	}

	public long getBeforeChunkCount() {
		return beforeChunkCount;
	}

	public void setBeforeChunkCount(long beforeChunkCount) {
		this.beforeChunkCount = beforeChunkCount;
	}

	public long getAfterChunkCount() {
		return afterChunkCount;
	}

	public void setAfterChunkCount(long afterChunkCount) {
		this.afterChunkCount = afterChunkCount;
	}

	public long getChunkErrorCount() {
		return chunkErrorCount;
	}

	public void setChunkErrorCount(long chunkErrorCount) {
		this.chunkErrorCount = chunkErrorCount;
	}

	public long getStreamOpenCount() {
		return streamOpenCount;
	}

	public void setStreamOpenCount(long streamOpenCount) {
		this.streamOpenCount = streamOpenCount;
	}

	public long getStreamUpdateCount() {
		return streamUpdateCount;
	}

	public void setStreamUpdateCount(long streamUpdateCount) {
		this.streamUpdateCount = streamUpdateCount;
	}

	public long getStreamCloseCount() {
		return streamCloseCount;
	}

	public void setStreamCloseCount(long streamCloseCount) {
		this.streamCloseCount = streamCloseCount;
	}

	public long getBeforeReadCount() {
		return beforeReadCount;
	}

	public void setBeforeReadCount(long beforeReadCount) {
		this.beforeReadCount = beforeReadCount;
	}

	public long getReadCount() {
		return readCount;
	}

	public void setReadCount(long readCount) {
		this.readCount = readCount;
	}

	public long getAfterReadCount() {
		return afterReadCount;
	}

	public void setAfterReadCount(long afterReadCount) {
		this.afterReadCount = afterReadCount;
	}

	public long getReadErrorCount() {
		return readErrorCount;
	}

	public void setReadErrorCount(long readErrorCount) {
		this.readErrorCount = readErrorCount;
	}

	public long getBeforeProcessCount() {
		return beforeProcessCount;
	}

	public void setBeforeProcessCount(long beforeProcessCount) {
		this.beforeProcessCount = beforeProcessCount;
	}

	public long getProcessCount() {
		return processCount;
	}

	public void setProcessCount(long processCount) {
		this.processCount = processCount;
	}

	public long getAfterProcessCount() {
		return afterProcessCount;
	}

	public void setAfterProcessCount(long afterProcessCount) {
		this.afterProcessCount = afterProcessCount;
	}

	public long getProcessErrorCount() {
		return processErrorCount;
	}

	public void setProcessErrorCount(long processErrorCount) {
		this.processErrorCount = processErrorCount;
	}

	public long getBeforeWriteCount() {
		return beforeWriteCount;
	}

	public void setBeforeWriteCount(long beforeWriteCount) {
		this.beforeWriteCount = beforeWriteCount;
	}

	public long getWriteCount() {
		return writeCount;
	}

	public void setWriteCount(long writeCount) {
		this.writeCount = writeCount;
	}

	public long getAfterWriteCount() {
		return afterWriteCount;
	}

	public void setAfterWriteCount(long afterWriteCount) {
		this.afterWriteCount = afterWriteCount;
	}

	public long getWriteErrorCount() {
		return writeErrorCount;
	}

	public void setWriteErrorCount(long writeErrorCount) {
		this.writeErrorCount = writeErrorCount;
	}

	public long getSkipInReadCount() {
		return skipInReadCount;
	}

	public void setSkipInReadCount(long skipInReadCount) {
		this.skipInReadCount = skipInReadCount;
	}

	public long getSkipInProcessCount() {
		return skipInProcessCount;
	}

	public void setSkipInProcessCount(long skipInProcessCount) {
		this.skipInProcessCount = skipInProcessCount;
	}

	public long getSkipInWriteCount() {
		return skipInWriteCount;
	}

	public void setSkipInWriteCount(long skipInWriteCount) {
		this.skipInWriteCount = skipInWriteCount;
	}

	public boolean isValidateGauge() {
		return validateGauge;
	}

	public void setValidateGauge(boolean validateGauge) {
		this.validateGauge = validateGauge;
	}

}
