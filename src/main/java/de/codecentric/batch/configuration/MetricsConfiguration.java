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
package de.codecentric.batch.configuration;

import java.util.HashSet;
import java.util.Set;

import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.rich.InMemoryRichGaugeRepository;
import org.springframework.boot.actuate.metrics.rich.RichGaugeRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import de.codecentric.batch.metrics.BatchMetricsImpl;
import de.codecentric.batch.metrics.DefaultExtendedCounterService;
import de.codecentric.batch.metrics.ExtendedCounterService;
import de.codecentric.batch.metrics.MetricsListener;
import de.codecentric.batch.metrics.ReaderProcessorWriterMetricsAspect;

/**
 * Configuration containing all metrics stuff. Can be activated by setting the property
 * batch.metrics.enabled to true.
 * 
 * @author Tobias Flohre
 */
@ConditionalOnProperty("batch.metrics.enabled")
@Configuration
public class MetricsConfiguration implements ListenerProvider{
	
	@Autowired
	private Environment env;
	@Autowired
	private BaseConfiguration baseConfig;
	@Autowired
	private RichGaugeRepository richGaugeRepository;
	
	@Bean
	public BatchMetricsImpl businessMetrics(){
		return new BatchMetricsImpl(counterService(), baseConfig.gaugeService());
	}
	
	@Bean
	public ExtendedCounterService counterService() {
		return new DefaultExtendedCounterService(baseConfig.metricRepository());
	}
	
	@ConditionalOnProperty("batch.metrics.profiling.readprocesswrite.enabled")
	@Bean
	public ReaderProcessorWriterMetricsAspect batchMetricsAspects() {
		return new ReaderProcessorWriterMetricsAspect(baseConfig.gaugeService());
	}

	@Bean
	public MetricsListener metricsListener(){
		return new MetricsListener(richGaugeRepository,baseConfig.metricRepository(), env.getProperty("batch.metrics.deletemetricsonstepfinish", boolean.class, true));
	}

	@Override
	public Set<JobExecutionListener> jobExecutionListeners() {
		return new HashSet<JobExecutionListener>();
	}

	@Override
	public Set<StepExecutionListener> stepExecutionListeners() {
		Set<StepExecutionListener> listeners = new HashSet<StepExecutionListener>();
		listeners.add(metricsListener());
		return listeners;
	}
	
	@ConditionalOnProperty("batch.metrics.enabled")
	@Configuration
	static class MetricsRepositoryConfiguration {

		/**
		 * This repository will be added automatically to the ones getting data from the
		 * GaugeService. Take a look at the MetricRepositoryAutoConfiguration for more information:
		 * The 'primaryMetricWriter' is collecting references to all MetricWriter implementations,
		 * and this is an implementation of MetricWriter.
		 */
		@Bean
		@ConditionalOnMissingBean(RichGaugeRepository.class)
		public RichGaugeRepository richGaugeRepository() {
			return new InMemoryRichGaugeRepository();
		}

	}

}
