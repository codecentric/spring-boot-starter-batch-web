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

/**
 * This listener is needed for tracking the running JobExecutions on this server.
 *  
 * <p>It's easy to find out which jobs are running in general by looking into the database, but it has 
 * some drawbacks:
 * <ul>
 * <li>Data in the database might be corrupted. A job may be in status STARTED simply because someone
 * killed the process and Spring Batch didn't have the chance to update the status.</li>
 * <li>We cannot tell from the database on which server the job is running.</li>
 * <li>We might just use an in-memory database, then we cannot access it.</li>
 * </ul>
 *  
 * <p>This listener uses the {@link RunningExecutionTracker} to keep this information in memory and 
 * accessible for the http endpoints.
 * 
 * @author Tobias Flohre
 *
 */
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
