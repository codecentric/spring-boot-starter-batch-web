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
 * @author tobias.flohre
 *
 */
public class LoggingListener implements JobExecutionListener, StepExecutionListener, Ordered {
	
	private JobLogFileNameCreator jobLogFileNameCreator = new DefaultJobLogFileNameCreator();

	public static final String JOB_INFO = "jobInfo";

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.JobExecutionListener#beforeJob(org.springframework.batch.core.JobExecution)
	 */
	@Override
	public void beforeJob(JobExecution jobExecution) {
		insertValuesIntoMDC(jobExecution);
	}

	private void insertValuesIntoMDC(JobExecution jobExecution) {
		MDC.put(JOB_INFO, jobLogFileNameCreator.createJobLogFileName(jobExecution));
	}

	/* (non-Javadoc)
	 * @see org.springframework.batch.core.JobExecutionListener#afterJob(org.springframework.batch.core.JobExecution)
	 */
	@Override
	public void afterJob(JobExecution jobExecution) {
		removeValuesFromMDC();
	}

	private void removeValuesFromMDC() {
		MDC.remove(JOB_INFO);
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
