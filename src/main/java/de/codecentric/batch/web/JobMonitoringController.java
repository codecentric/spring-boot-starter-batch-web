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

package de.codecentric.batch.web;

import java.util.Set;

import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import de.codecentric.batch.monitoring.RunningExecutionTracker;

/**
 * Controller for delivering monitoring information, like
 * 	which jobs are deployed?
 * 	which jobs are currently running on this machine?
 * 	detailed information on any running job.
 *
 * @author Tobias Flohre
 *
 */
@RestController
@RequestMapping("/batch/monitoring")
public class JobMonitoringController {

	private JobOperator jobOperator;
	private JobExplorer jobExplorer;
	private RunningExecutionTracker runningExecutionTracker;
	
	public JobMonitoringController(JobOperator jobOperator,
			JobExplorer jobExplorer,
			RunningExecutionTracker runningExecutionTracker) {
		super();
		this.jobOperator = jobOperator;
		this.jobExplorer = jobExplorer;
		this.runningExecutionTracker = runningExecutionTracker;
	}

	@RequestMapping(value="/jobs", method = RequestMethod.GET)
	public Set<String> findRegisteredJobs() {
		return jobOperator.getJobNames();
	}

	@RequestMapping(value = "/runningexecutions", method = RequestMethod.GET)
	public Set<Long> findAllRunningExecutions() {
		return runningExecutionTracker.getAllRunningExecutionIds();
	}

	@RequestMapping(value = "/runningexecutions/{jobName}", method = RequestMethod.GET)
	public Set<Long> findRunningExecutionsForJobName(@PathVariable String jobName) {
		return runningExecutionTracker.getRunningExecutionIdsForJobName(jobName);
	}

	@RequestMapping(value = "/executions/{executionId}", method = RequestMethod.GET)
	public JobExecution findExecution(@PathVariable long executionId) {
		return jobExplorer.getJobExecution(executionId);
	}

}
