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
package de.codecentric.batch;

import java.util.HashSet;
import java.util.Set;

import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.codecentric.batch.configuration.ListenerProvider;
import de.codecentric.batch.listener.TestListener;

/**
 * @author Tobias Flohre
 */
@Configuration
public class TestListenerConfiguration implements ListenerProvider {

	/* (non-Javadoc)
	 * @see de.codecentric.batch.configuration.ListenerProvider#jobExecutionListeners()
	 */
	@Override
	public Set<JobExecutionListener> jobExecutionListeners() {
		Set<JobExecutionListener> listeners = new HashSet<JobExecutionListener>();
		listeners.add(testListener());
		return listeners;
	}

	/* (non-Javadoc)
	 * @see de.codecentric.batch.configuration.ListenerProvider#stepExecutionListeners()
	 */
	@Override
	public Set<StepExecutionListener> stepExecutionListeners() {
		return new HashSet<StepExecutionListener>();
	}
	
	@Bean
	public TestListener testListener(){
		return new TestListener();
	}

}
