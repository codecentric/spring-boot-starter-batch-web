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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import de.codecentric.batch.monitoring.RunningExecutionTracker;

/**
 * Controller for delivering monitoring information, like
 * <ul>
 * <li>	which jobs are deployed?</li>
 * <li>	which jobs are currently running on this machine?</li>
 * <li>	detailed information on any running or finished job.</li>
 * </ul>
 * <p>The base url can be set via property batch.web.monitoring.base, its default is /batch/monitoring.
 * 
 * There are four endpoints available:
 * 
 * <ol>
 * <li>Retrieving the names of deployed jobs<br>
 * {base_url}/jobs / GET<br>
 * On success, it returns a JSON array of String containing the names of the deployed jobs.</li>
 * 
 * <li>Retrieving the ids of JobExecutions running on this server<br>
 * {base_url}/runningexecutions / GET<br>
 * On success, it returns a JSON array containing the ids of the JobExecutions running on this server.</li>
 * 
 * <li>Retrieving the ids of JobExecutions running on this server for a certain job name<br>
 * {base_url}/runningexecutions/{jobName} / GET<br>
 * On success, it returns a JSON array containing the ids of the JobExecutions running on this server
 * belonging to the specified job.</li>
 * 
 * <li>Retrieving the JobExecution<br>
 * {base_url}/executions/{executionId} / GET<br>
 * On success, it returns a JSON representation of the JobExecution specified by the id. This representation
 * contains everything you need to know about that job, from job name and BatchStatus to the number of 
 * processed items and time used and so on.<br>
 * If the JobExecution cannot be found, a HTTP response code 404 is returned.</li>
 * </ol>
 * 
 * 
 * @author Tobias Flohre
 *
 */
@RestController
@RequestMapping("${batch.web.monitoring.base:/batch/monitoring}")
public class JobMonitoringController {

	private static final Logger LOG = LoggerFactory.getLogger(JobMonitoringController.class);

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
	public JobExecution findExecution(@PathVariable long executionId) throws NoSuchJobExecutionException {
		JobExecution jobExecution = jobExplorer.getJobExecution(executionId);
		if (jobExecution == null){
			throw new NoSuchJobExecutionException("JobExecution with id "+executionId+" not found.");
		}
		return jobExecution;
	}

	@ResponseStatus(HttpStatus.NOT_FOUND)
	@ExceptionHandler(NoSuchJobExecutionException.class)
	public String handleNotFound(Exception ex) {
		LOG.warn("JobExecution not found.",ex);
	    return ex.getMessage();
	}

}
