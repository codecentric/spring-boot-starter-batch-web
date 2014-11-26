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

package de.codecentric.batch.listener;

import java.util.Iterator;
import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.JobParameter;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.core.Ordered;

/**
 * This listener adds a protocol header and a protocol summary to the log.
 * 
 * @author Tobias Flohre
 * @author Dennis Schulte
 * 
 */
public class ProtocolListener implements JobExecutionListener, Ordered {

	private static final int DEFAULT_WIDTH = 80;
	private static final Log LOGGER = LogFactory.getLog(ProtocolListener.class);

	public void afterJob(JobExecution jobExecution) {
		StringBuilder protocol = new StringBuilder();
		protocol.append("\n");
		protocol.append(createFilledLine('*'));
		protocol.append(createFilledLine('-'));
		protocol.append("Protocol for " + jobExecution.getJobInstance().getJobName() + " \n");
		protocol.append("  Started:      " + jobExecution.getStartTime() + "\n");
		protocol.append("  Finished:     " + jobExecution.getEndTime() + "\n");
		protocol.append("  Exit-Code:    " + jobExecution.getExitStatus().getExitCode() + "\n");
		protocol.append("  Exit-Descr:   " + jobExecution.getExitStatus().getExitDescription() + "\n");
		protocol.append("  Status:       " + jobExecution.getStatus() + "\n");
		protocol.append("  Content of Job-ExecutionContext:\n");
		for (Entry<String, Object> entry : jobExecution.getExecutionContext().entrySet()) {
			protocol.append("  " + entry.getKey() + "=" + entry.getValue() + "\n");
		}
		protocol.append("  Job-Parameter: \n");
		JobParameters jp = jobExecution.getJobParameters();
		for (Iterator<Entry<String, JobParameter>> iter = jp.getParameters().entrySet().iterator(); iter.hasNext();) {
			Entry<String, JobParameter> entry = iter.next();
			protocol.append("  " + entry.getKey() + "=" + entry.getValue() + "\n");
		}
		protocol.append(createFilledLine('-'));
		for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
			protocol.append("Step " + stepExecution.getStepName() + " \n");
			protocol.append("  ReadCount:    " + stepExecution.getReadCount() + "\n");
			protocol.append("  WriteCount:   " + stepExecution.getWriteCount() + "\n");
			protocol.append("  Commits:      " + stepExecution.getCommitCount() + "\n");
			protocol.append("  SkipCount:    " + stepExecution.getSkipCount() + "\n");
			protocol.append("  Rollbacks:    " + stepExecution.getRollbackCount() + "\n");
			protocol.append("  Filter:       " + stepExecution.getFilterCount() + "\n");
			protocol.append("  Content of Step-ExecutionContext:\n");
			for (Entry<String, Object> entry : stepExecution.getExecutionContext().entrySet()) {
				protocol.append("  " + entry.getKey() + "=" + entry.getValue() + "\n");
			}
			protocol.append(createFilledLine('-'));
		}
		protocol.append(createFilledLine('*'));
		LOGGER.info(protocol.toString());
	}

	public void beforeJob(JobExecution jobExecution) {
		StringBuilder protocol = new StringBuilder();
		protocol.append(createFilledLine('-'));
		protocol.append("Job " + jobExecution.getJobInstance().getJobName() + " started with Job-Execution-Id " + jobExecution.getId() + " \n");
		protocol.append("Job-Parameter: \n");
		JobParameters jp = jobExecution.getJobParameters();
		for (Iterator<Entry<String, JobParameter>> iter = jp.getParameters().entrySet().iterator(); iter.hasNext();) {
			Entry<String, JobParameter> entry = iter.next();
			protocol.append("  " + entry.getKey() + "=" + entry.getValue() + "\n");
		}
		protocol.append(createFilledLine('-'));
		LOGGER.info(protocol.toString());
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE + 10;
	}

	/**
	 * Create line with defined char
	 * 
	 * @param filler
	 */
	private String createFilledLine(char filler) {
		return StringUtils.leftPad("", DEFAULT_WIDTH, filler) + "\n";
	}

}
