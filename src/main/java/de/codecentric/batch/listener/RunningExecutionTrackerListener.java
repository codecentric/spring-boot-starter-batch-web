/*
 * Copyright 2012-2014 the original author or authors.
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

package de.codecentric.batch.listener;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

import de.codecentric.batch.monitoring.RunningExecutionTracker;

public class RunningExecutionTrackerListener implements JobExecutionListener {
	
	private RunningExecutionTracker runningExecutionTracker;
	
	public RunningExecutionTrackerListener(
			RunningExecutionTracker runningExecutionTracker) {
		super();
		this.runningExecutionTracker = runningExecutionTracker;
	}

	@Override
	public void beforeJob(JobExecution jobExecution) {
		runningExecutionTracker.addRunningExecution(jobExecution.getJobInstance().getJobName(), jobExecution.getId());
	}

	@Override
	public void afterJob(JobExecution jobExecution) {
		runningExecutionTracker.removeRunningExecution(jobExecution.getId());
	}

}
