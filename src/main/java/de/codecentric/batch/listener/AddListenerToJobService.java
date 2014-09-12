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

import java.util.Set;

import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.job.AbstractJob;
import org.springframework.batch.core.step.AbstractStep;
import org.springframework.beans.factory.annotation.Autowired;

import de.codecentric.batch.configuration.ListenerProvider;

/**
 * This service adds listeners to jobs. Listeners provided by beans implementing
 * ListenerProvider are added automatically.
 * 
 * @author Tobias Flohre
 */
public class AddListenerToJobService {
	
	/**
	 * All beans implementing ListenerProvider are injected here.
	 */
	@Autowired(required=false)
	private Set<ListenerProvider> listenerProviders;
	
	private boolean addProtocolListener;
	private boolean addLoggingListener;
	private ProtocolListener protocolListener;
	private RunningExecutionTrackerListener runningExecutionTrackerListener;
	private LoggingListener loggingListener;
	private LoggingAfterJobListener loggingAfterJobListener;

	public AddListenerToJobService(boolean addProtocolListener,
			boolean addLoggingListener, ProtocolListener protocolListener,
			RunningExecutionTrackerListener runningExecutionTrackerListener,
			LoggingListener loggingListener,
			LoggingAfterJobListener loggingAfterJobListener) {
		super();
		this.addProtocolListener = addProtocolListener;
		this.addLoggingListener = addLoggingListener;
		this.protocolListener = protocolListener;
		this.runningExecutionTrackerListener = runningExecutionTrackerListener;
		this.loggingListener = loggingListener;
		this.loggingAfterJobListener = loggingAfterJobListener;
	}

	public void addListenerToJob(AbstractJob job){
		if (addProtocolListener){
			job.registerJobExecutionListener(protocolListener);
		}
		job.registerJobExecutionListener(runningExecutionTrackerListener);
		if (addLoggingListener){
			job.registerJobExecutionListener(loggingListener);
			job.registerJobExecutionListener(loggingAfterJobListener);
			for (String stepName: job.getStepNames()){
				AbstractStep step = (AbstractStep)job.getStep(stepName);
				step.registerStepExecutionListener(loggingListener);
			}
		}
		if (listenerProviders != null){
			for (ListenerProvider listenerProvider: listenerProviders){
				for (JobExecutionListener jobExecutionListener: listenerProvider.jobExecutionListeners()){
					job.registerJobExecutionListener(jobExecutionListener);
				}
				for (StepExecutionListener stepExecutionListener: listenerProvider.stepExecutionListeners()){
					for (String stepName: job.getStepNames()){
						AbstractStep step = (AbstractStep)job.getStep(stepName);
						step.registerStepExecutionListener(stepExecutionListener);
					}
				}
			}
		}
	}
	
	public void setListenerProviders(Set<ListenerProvider> listenerProviders){
		this.listenerProviders = listenerProviders;
	}

}
