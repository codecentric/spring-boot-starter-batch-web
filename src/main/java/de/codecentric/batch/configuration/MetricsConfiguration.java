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
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;

import metrics_influxdb.Influxdb;
import metrics_influxdb.InfluxdbReporter;

import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.rich.InMemoryRichGaugeRepository;
import org.springframework.boot.actuate.metrics.rich.RichGaugeRepository;
import org.springframework.boot.actuate.metrics.writer.MetricWriter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;

import de.codecentric.batch.metrics.BatchMetricsImpl;
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
	@Autowired
	private MetricWriter metricWriter;
	@Autowired
	private MetricRegistry metricRegistry;
	
	@Bean
	public BatchMetricsImpl batchMetrics(){
		return new BatchMetricsImpl(metricWriter);
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
	
	@PostConstruct
	public void configureReporter() throws Exception{
		final Influxdb influxdb = new Influxdb("192.168.59.103", 8086, "mydata", "root", "root");
	    influxdb.debugJson = true; // to print json on System.err
	    //influxdb.jsonBuilder = new MyJsonBuildler(); // to use MyJsonBuilder to create json
	    final InfluxdbReporter reporter = InfluxdbReporter
	            .forRegistry(metricRegistry)
	            //.prefixedWith("test")
	            .convertRatesTo(TimeUnit.SECONDS)
	            .convertDurationsTo(TimeUnit.MILLISECONDS)
	            .filter(MetricFilter.ALL)
	            .build(influxdb);
	    reporter.start(10, TimeUnit.SECONDS);
	}

}
