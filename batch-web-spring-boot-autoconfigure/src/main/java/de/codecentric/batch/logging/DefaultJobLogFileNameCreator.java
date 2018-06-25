/*
 * Copyright 2018 the original author or authors.
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

package de.codecentric.batch.logging;

import org.springframework.batch.core.JobExecution;

/**
 * Default implementation used when there's no other Spring bean implementing {@link JobLogFileNameCreator}
 * in the ApplicationContext.
 * 
 * @author Tobias Flohre
 * @author Dennis Schulte
 *
 */
public class DefaultJobLogFileNameCreator implements JobLogFileNameCreator {

	private final static String DEFAULT_EXTENSION = ".log";

	@Override
	public String getName(JobExecution jobExecution) {
		return getBaseName(jobExecution) + getExtension();
	}
	
	@Override
	public String getBaseName(JobExecution jobExecution) {
		return "batch-"+jobExecution.getJobInstance().getJobName()+"-"+Long.toString(jobExecution.getId());
	}
	
	@Override
	public String getExtension(){
		return DEFAULT_EXTENSION;
	}



}
