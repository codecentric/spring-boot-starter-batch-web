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
package de.codecentric.batch.metrics;

/**
 * @author Tobias Flohre
 */
public enum MetricNames {
	BEFORE_JOB_COUNT("before.job.count"),
	AFTER_JOB_COUNT("after.job.count"),
	BEFORE_STEP_COUNT("before.step.count"),
	AFTER_STEP_COUNT("after.step.count"),
	BEFORE_CHUNK_COUNT("before.chunk.count"),
	AFTER_CHUNK_COUNT("after.chunk.count"),
	CHUNK_ERROR_COUNT("chunk.error.count"),
	STREAM_OPEN_COUNT("stream.open.count"),
	STREAM_UPDATE_COUNT("stream.update.count"),
	STREAM_CLOSE_COUNT("stream.close.count"),
	BEFORE_READ_COUNT("before.read.count"),
	READ_COUNT("read.count"), 
	AFTER_READ_COUNT("after.read.count"), 
	READ_ERROR_COUNT("read.error.count"),
	BEFORE_PROCESS_COUNT("before.process.count"), 
	PROCESS_COUNT("process.count"), 
	AFTER_PROCESS_COUNT("after.process.count"), 
	PROCESS_ERROR_COUNT("process.error.count"),
	BEFORE_WRITE_COUNT("before.write.count"), 
	WRITE_COUNT("write.count"), 
	AFTER_WRITE_COUNT("after.write.count"), 
	WRITE_ERROR_COUNT("write.error.count"),
	SKIP_IN_READ_COUNT("skip.read.count"), 
	SKIP_IN_PROCESS_COUNT("skip.process.count"), 
	SKIP_IN_WRITE_COUNT("skip.write.count"),
	
	BEFORE_JOB_GAUGE("before.job.gauge"),
	AFTER_JOB_GAUGE("after.job.gauge"),
	BEFORE_STEP_GAUGE("before.step.gauge"),
	AFTER_STEP_GAUGE("after.step.gauge"),
	BEFORE_CHUNK_GAUGE("before.chunk.gauge"),
	AFTER_CHUNK_GAUGE("after.chunk.gauge"),
	CHUNK_ERROR_GAUGE("chunk.error.gauge"),
	STREAM_OPEN_GAUGE("stream.open.gauge"),
	STREAM_UPDATE_GAUGE("stream.update.gauge"),
	STREAM_CLOSE_GAUGE("stream.close.gauge"),
	BEFORE_READ_GAUGE("before.read.gauge"),
	READ_GAUGE("read.gauge"), 
	AFTER_READ_GAUGE("after.read.gauge"), 
	READ_ERROR_GAUGE("read.error.gauge"),
	BEFORE_PROCESS_GAUGE("before.process.gauge"), 
	PROCESS_GAUGE("process.gauge"), 
	AFTER_PROCESS_GAUGE("after.process.gauge"), 
	PROCESS_ERROR_GAUGE("process.error.gauge"),
	BEFORE_WRITE_GAUGE("before.write.gauge"), 
	WRITE_GAUGE("write.gauge"), 
	AFTER_WRITE_GAUGE("after.write.gauge"), 
	WRITE_ERROR_GAUGE("write.error.gauge"),
	SKIP_IN_READ_GAUGE("skip.read.gauge"), 
	SKIP_IN_PROCESS_GAUGE("skip.process.gauge"), 
	SKIP_IN_WRITE_GAUGE("skip.write.gauge");
	
	private String name;
	private MetricNames(String name){
		this.name = name;
	}
	public String getName(){
		return name;
	}
}
