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

import org.slf4j.MDC;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;

import de.codecentric.batch.logging.DefaultJobLogFileNameCreator;
import de.codecentric.batch.logging.JobLogFileNameCreator;

/**
 * This listener writes the job log file name to the MDC so that it can be picked up by the logging
 * framework for logging to it. It's a {@link JobExecutionListener} and a {@link StepExecutionListener}
 * because in partitioning we may have a lot of {@link StepExecution}s running in different threads.
 * Due to the fact that the afterStep - method would remove the variable from the MDC in single threaded
 * execution we need to re-set it, that's what's the {@link LoggingAfterJobListener} is for.
 * Note that, of the three local parallelization features in Spring Batch, log file separation only 
 * works for partitioning and parallel step, not for multi-threaded step.
 * 
 * The log file name is determined by a {@link JobLogFileNameCreator}. It's default implementation
 * {@link DefaultJobLogFileNameCreator} is used when there's no other bean of this type in the 
 * ApplicationContext.
 * 
 * @author Tobias Flohre
 *
 */
public class LoggingListener implements JobExecutionListener, StepExecutionListener, Ordered {
	
	private JobLogFileNameCreator jobLogFileNameCreator = new DefaultJobLogFileNameCreator();

	public static final String JOBLOG_FILENAME = "jobLogFileName";
	public static final String JOB_EXECUTION_IDENTIFIER = "jobExecutionIdentifier";

	@Override
	public void beforeJob(JobExecution jobExecution) {
		insertValuesIntoMDC(jobExecution);
	}

	private void insertValuesIntoMDC(JobExecution jobExecution) {
		MDC.put(JOBLOG_FILENAME, jobLogFileNameCreator.createJobLogFileName(jobExecution));
		MDC.put(JOB_EXECUTION_IDENTIFIER, jobExecution.getJobInstance().getJobName()+"."+jobExecution.getId());
	}

	@Override
	public void afterJob(JobExecution jobExecution) {
		removeValuesFromMDC();
	}

	private void removeValuesFromMDC() {
		MDC.remove(JOBLOG_FILENAME);
		MDC.remove(JOB_EXECUTION_IDENTIFIER);
	}

	@Override
	public void beforeStep(StepExecution stepExecution) {
		insertValuesIntoMDC(stepExecution.getJobExecution());
	}

	@Override
	public ExitStatus afterStep(StepExecution stepExecution) {
		removeValuesFromMDC();
		return null;
	}

	@Override
	public int getOrder() {
		return Ordered.HIGHEST_PRECEDENCE;
	}

	@Autowired(required=false)
	public void setJobLogFileNameCreator(JobLogFileNameCreator jobLogFileNameCreator) {
		this.jobLogFileNameCreator = jobLogFileNameCreator;
	}

}
