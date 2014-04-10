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

import java.io.File;
import java.io.FileInputStream;
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
import org.springframework.batch.core.launch.JobInstanceAlreadyExistsException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.support.PropertiesConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.codecentric.batch.logging.DefaultJobLogFileNameCreator;
import de.codecentric.batch.logging.JobLogFileNameCreator;

/**
 * Very simple REST-API for starting and stopping jobs and keeping track of its status.
 * Made for script interaction.
 *
 * @author Dennis Schulte
 * @author Tobias Flohre
 *
 */
@RestController
@RequestMapping("/batch/operations")
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
	public String launch(@PathVariable String jobName, @RequestParam MultiValueMap<String, String> payload) {
		try {
			String parameters = payload.getFirst(JOB_PARAMETERS);
			JobParameters jobParameters = jobParametersConverter.getJobParameters(PropertiesConverter.stringToProperties(parameters));

			// get a job 
			Job job = jobRegistry.getJob(jobName);
			// use JobParametersIncrementer to create JobParameters if incrementer is set and only if the job is no restart
			if (job.getJobParametersIncrementer() != null){
				JobExecution lastJobExecution = jobRepository.getLastJobExecution(jobName, jobParameters);
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
			Long id = jobOperator.start(jobName, parameters);
			return String.valueOf(id);
		} catch (UnexpectedJobExecutionException e) {
			LOG.error("Fehler beim Starten des Jobs", e);
			return e.getMessage();
		} catch (NoSuchJobException e) {
			return e.getMessage();
		} catch (JobInstanceAlreadyExistsException e) {
			return e.getMessage();
		} catch (JobParametersInvalidException e) {
			return e.getMessage();
		}
	}

	@RequestMapping(value = "/executions/{executionId}", method = RequestMethod.GET)
	public String getStatus(@PathVariable long executionId) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("Ermittle Status zu Job mit ExecutionId: " + executionId);
		}
		JobExecution jobExecution = jobExplorer.getJobExecution(executionId);
		if (jobExecution != null){
			return jobExecution.getStatus().toString();
		} else {
			// TODO 404 werfen
			return "Not found";
		}
	}

	@RequestMapping(value = "/executions/{executionId}/log", method = RequestMethod.GET)
	public void getLogFile(HttpServletResponse response, @PathVariable long executionId) {
		if (LOG.isDebugEnabled()) {
			LOG.debug("Get log file for job with executionId: " + executionId);
		}
	    try {
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
	        // get your file as InputStream
			JobExecution jobExecution = jobExplorer.getJobExecution(executionId);
	        File downloadFile = new File(loggingPath+jobLogFileNameCreator.createJobLogFileName(jobExecution));
	        InputStream is = new FileInputStream(downloadFile);	        
	        // copy it to response's OutputStream
	        FileCopyUtils.copy(is, response.getOutputStream());
	        response.flushBuffer();
	      } catch (IOException ex) {
	        LOG.info("Error writing file to output stream.");
	        throw new RuntimeException("IOError writing file to output stream");
	      }		
	}

	@RequestMapping(value = "/executions/{executionId}", method = RequestMethod.DELETE)
	public String stop(@PathVariable long executionId) {
		LOG.info("Stoppe Job mit ExecutionId: " + executionId);
		try {
			Boolean successful = jobOperator.stop(executionId);
			return successful.toString();
		} catch (Exception e) {
			LOG.error("Fehler beim Stoppen des Jobs", e);
			return Boolean.FALSE.toString();
		}
	}

	@Autowired(required=false)
	public void setJobLogFileNameCreator(JobLogFileNameCreator jobLogFileNameCreator) {
		this.jobLogFileNameCreator = jobLogFileNameCreator;
	}
}
