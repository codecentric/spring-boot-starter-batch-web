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

package de.codecentric.batch.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.converter.DefaultJobParametersConverter;
import org.springframework.batch.core.converter.JobParametersConverter;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobExecutionNotRunningException;
import org.springframework.batch.core.launch.JobInstanceAlreadyExistsException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.support.PropertiesConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import de.codecentric.batch.logging.DefaultJobLogFileNameCreator;
import de.codecentric.batch.logging.JobLogFileNameCreator;

/**
 * Very simple REST-API for starting and stopping jobs and keeping track of its status.
 * Made for script interaction.
 * 
 * <p>The base url can be set via property batch.web.operations.base, its default is /batch/operations.
 * There are four endpoints available:
 * 
 * <ol>
 * <li>Starting jobs<br>
 * {base_url}/jobs/{jobName} / POST<br>
 * Optionally you may define job parameters via request param 'jobParameters'. If a JobParametersIncrementer
 * is specified in the job, it is used to increment the parameters.<br>
 * On success, it returns the JobExecution's id as a plain string.<br>
 * On failure, it returns the message of the Exception as a plain string. There are different failure 
 * possibilities:
 * <ul>
 * <li>HTTP response code 404 (NOT_FOUND): the job cannot be found, not deployed on this server.</li>
 * <li>HTTP response code 409 (CONFLICT): the JobExecution already exists and is either running or not restartable.</li>
 * <li>HTTP response code 422 (UNPROCESSABLE_ENTITY): the job parameters didn't pass the validator.</li>
 * <li>HTTP response code 500 (INTERNAL_SERVER_ERROR): any other unexpected failure.</li>
 * </ul></li>
 * 
 * <li>Retrieving an JobExecution's BatchStatus<br>
 * {base_url}/jobs/executions/{executionId} / GET<br>
 * On success, it returns the BatchStatus of the JobExecution specified by the executionId as a plain string.<br>
 * On failure, it returns the message of the Exception as a plain string. There are different failure 
 * possibilities:
 * <ul>
 * <li>HTTP response code 404 (NOT_FOUND): the JobExecution cannot be found.</li>
 * <li>HTTP response code 500 (INTERNAL_SERVER_ERROR): any other unexpected failure.</li>
 * </ul></li>
 * 
 * <li>Retrieving a log file for a specific JobExecution<br>
 * {base_url}/jobs/executions/{executionId}/log / GET<br>
 * On success, it returns the log file belonging to the run of the JobExecution specified by the executionId 
 * as a plain string.<br>
 * On failure, it returns the message of the Exception as a plain string. There are different failure 
 * possibilities:
 * <ul>
 * <li>HTTP response code 404 (NOT_FOUND): the log file cannot be found.</li>
 * <li>HTTP response code 500 (INTERNAL_SERVER_ERROR): any other unexpected failure.</li>
 * </ul></li>
 * 
 * <li>Stopping jobs<br>
 * {base_url}/jobs/executions/{executionId} / DELETE<br>
 * On success, it returns true.<br>
 * On failure, it returns the message of the Exception as a plain string. There are different failure 
 * possibilities:
 * <ul>
 * <li>HTTP response code 404 (NOT_FOUND): the JobExecution cannot be found.</li>
 * <li>HTTP response code 409 (CONFLICT): the JobExecution is not running.</li>
 * <li>HTTP response code 500 (INTERNAL_SERVER_ERROR): any other unexpected failure.</li>
 * </ul></li>
 * </ol>
 * 
 *
 * @author Dennis Schulte
 * @author Tobias Flohre
 *
 */
@RestController
@RequestMapping("${batch.web.operations.base:/batch/operations}")
public class JobOperationsController {

	private static final Logger LOG = LoggerFactory.getLogger(JobOperationsController.class);

	public static final String JOB_PARAMETERS = "jobParameters";

	private JobOperator jobOperator;
	private JobExplorer jobExplorer;
	private JobRegistry jobRegistry;
	private JobRepository jobRepository;
	private JobParametersConverter jobParametersConverter = new DefaultJobParametersConverter();
	private JobLogFileNameCreator jobLogFileNameCreator = new DefaultJobLogFileNameCreator();
	
	public JobOperationsController(JobOperator jobOperator,
			JobExplorer jobExplorer, JobRegistry jobRegistry,
			JobRepository jobRepository) {
		super();
		this.jobOperator = jobOperator;
		this.jobExplorer = jobExplorer;
		this.jobRegistry = jobRegistry;
		this.jobRepository = jobRepository;
	}

	@RequestMapping(value = "/jobs/{jobName}", method = RequestMethod.POST)
	public String launch(@PathVariable String jobName, @RequestParam MultiValueMap<String, String> payload) throws NoSuchJobException, JobInstanceAlreadyExistsException, JobParametersInvalidException {
		String parameters = payload.getFirst(JOB_PARAMETERS);
		if (LOG.isDebugEnabled()) {
			LOG.debug("Attempt to start job with name " + jobName + " and parameters "+parameters+".");
		}
		Job job = jobRegistry.getJob(jobName);
		parameters = createJobParametersWithIncrementerIfAvailable(parameters, job);
		Long id = jobOperator.start(jobName, parameters);
		return String.valueOf(id);
	}
	
	private String createJobParametersWithIncrementerIfAvailable(String parameters, Job job) {
		JobParameters jobParameters = jobParametersConverter.getJobParameters(PropertiesConverter.stringToProperties(parameters));
		// use JobParametersIncrementer to create JobParameters if incrementer is set and only if the job is no restart
		if (job.getJobParametersIncrementer() != null){
			JobExecution lastJobExecution = jobRepository.getLastJobExecution(job.getName(), jobParameters);
			boolean restart = false;
			// check if job failed before
			if (lastJobExecution != null) {
				BatchStatus status = lastJobExecution.getStatus();
				if (status.isUnsuccessful() && status != BatchStatus.ABANDONED) {
					restart = true;
				}
			}
			// if it's not a restart, create new JobParameters with the incrementer
			if (!restart) {
				jobParameters = job.getJobParametersIncrementer().getNext(jobParameters);
				Properties newParameters = jobParametersConverter.getProperties(jobParameters);
				parameters = PropertiesConverter.propertiesToString(newParameters);
			}
		}
		return parameters;
	}

	@RequestMapping(value = "/jobs/executions/{executionId}", method = RequestMethod.GET)
	public String getStatus(@PathVariable long executionId) throws NoSuchJobExecutionException {
		if (LOG.isDebugEnabled()) {
			LOG.debug("Get BatchStatus for JobExecution with id: " + executionId+".");
		}
		JobExecution jobExecution = jobExplorer.getJobExecution(executionId);
		if (jobExecution != null){
			return jobExecution.getStatus().toString();
		} else {
			throw new NoSuchJobExecutionException("JobExecution with id "+executionId+" not found.");
		}
	}

	@RequestMapping(value = "/jobs/executions/{executionId}/log", method = RequestMethod.GET)
	public void getLogFile(HttpServletResponse response, @PathVariable long executionId) throws NoSuchJobExecutionException, IOException {
		if (LOG.isDebugEnabled()) {
			LOG.debug("Get log file for job with executionId: " + executionId);
		}
    	String loggingPath = createLoggingPath();
		JobExecution jobExecution = jobExplorer.getJobExecution(executionId);
		if (jobExecution == null){
			throw new NoSuchJobExecutionException("JobExecution with id "+executionId+" not found.");
		}
        File downloadFile = new File(loggingPath+jobLogFileNameCreator.createJobLogFileName(jobExecution));
        InputStream is = new FileInputStream(downloadFile);	        
        FileCopyUtils.copy(is, response.getOutputStream());
        response.flushBuffer();
	}

	private String createLoggingPath() {
		String loggingPath = System.getProperty("LOG_PATH");
    	if (loggingPath == null){
    		loggingPath = System.getProperty("java.io.tmpdir");
    	}
    	if (loggingPath == null){
    		loggingPath = "/tmp";
    	}
    	if (!loggingPath.endsWith("/")){
    		loggingPath = loggingPath+"/";
    	}
		return loggingPath;
	}

	@RequestMapping(value = "/jobs/executions/{executionId}", method = RequestMethod.DELETE)
	public String stop(@PathVariable long executionId) throws NoSuchJobExecutionException, JobExecutionNotRunningException {
		if (LOG.isDebugEnabled()) {
			LOG.debug("Stop JobExecution with id: " + executionId);
		}
		Boolean successful = jobOperator.stop(executionId);
		return successful.toString();
	}

	@ResponseStatus(HttpStatus.NOT_FOUND)
	@ExceptionHandler({NoSuchJobException.class, NoSuchJobExecutionException.class})
	public String handleNotFound(Exception ex) {
		LOG.warn("Job or JobExecution not found.",ex);
	    return ex.getMessage();
	}

	@ResponseStatus(HttpStatus.CONFLICT)
	@ExceptionHandler({UnexpectedJobExecutionException.class, JobInstanceAlreadyExistsException.class})
	public String handleAlreadyExists(Exception ex) {
		LOG.warn("JobInstance or JobExecution already exists.",ex);
	    return ex.getMessage();
	}

	@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
	@ExceptionHandler(JobParametersInvalidException.class)
	public String handleParametersInvalid(Exception ex) {
		LOG.warn("Job parameters are invalid.",ex);
	    return ex.getMessage();
	}

	@ResponseStatus(HttpStatus.NOT_FOUND)
	@ExceptionHandler(FileNotFoundException.class)
	public String handleFileNotFound(Exception ex) {
		LOG.warn("Logfile not found.",ex);
	    return ex.getMessage();
	}

	@ResponseStatus(HttpStatus.CONFLICT)
	@ExceptionHandler(JobExecutionNotRunningException.class)
	public String handleNotRunning(Exception ex) {
		LOG.warn("JobExecution is not running.",ex);
	    return ex.getMessage();
	}

	@Autowired(required=false)
	public void setJobLogFileNameCreator(JobLogFileNameCreator jobLogFileNameCreator) {
		this.jobLogFileNameCreator = jobLogFileNameCreator;
	}
}
