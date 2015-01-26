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
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;

import de.codecentric.batch.logging.DefaultJobLogFileNameCreator;
import de.codecentric.batch.logging.JobLogFileNameCreator;

/**
 * This extra listener is needed, because the {@link LoggingListener} removes the variable from the MDC
 * in its afterStep method. We re-set it here at the beginning of the execution of all afterJob methods
 * of JobExecutionListeners.
 * @see LoggingListener
 * 
 * @author Tobias Flohre
 * 
 */
public class LoggingAfterJobListener implements JobExecutionListener, Ordered {

	private JobLogFileNameCreator jobLogFileNameCreator = new DefaultJobLogFileNameCreator();

	@Override
	public void beforeJob(JobExecution jobExecution) {
	}

	private void insertValuesIntoMDC(JobExecution jobExecution) {
		MDC.put(LoggingListener.JOBLOG_FILENAME, jobLogFileNameCreator.getBaseName(jobExecution));
	}

	@Override
	public void afterJob(JobExecution jobExecution) {
		insertValuesIntoMDC(jobExecution);
	}

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}

	@Autowired(required = false)
	public void setJobLogFileNameCreator(JobLogFileNameCreator jobLogFileNameCreator) {
		this.jobLogFileNameCreator = jobLogFileNameCreator;
	}

}
