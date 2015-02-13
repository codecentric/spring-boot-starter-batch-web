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

import metrics_influxdb.InfluxdbReporter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.export.Exporter;
import org.springframework.boot.actuate.metrics.reader.MetricReader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.graphite.GraphiteReporter;

import de.codecentric.batch.metrics.ConsoleMetricsExporter;
import de.codecentric.batch.metrics.GraphiteMetricsExporter;
import de.codecentric.batch.metrics.InfluxdbMetricsExporter;

/**
 * Configuration for the Metrics Exporters
 * 
 * @author Dennis Schulte
 */
@ConditionalOnProperty("batch.metrics.enabled")
@Configuration
@EnableScheduling
public class MetricsExporterConfiguration {

	@Autowired
	private MetricReader metricReader;
	
	@Autowired
	private MetricRegistry metricRegistry;
	
	@Bean
	@ConditionalOnProperty("batch.metrics.export.console.enabled")
	public Exporter consoleExporter(){
		return new ConsoleMetricsExporter(metricRegistry,metricReader);
	}
	
	@Bean
	@ConditionalOnProperty("batch.metrics.export.graphite.enabled")
	@ConditionalOnClass(GraphiteReporter.class)
	public Exporter graphiteExporter(){
		return new GraphiteMetricsExporter(metricRegistry,metricReader);
	}
	
	@Bean
	@ConditionalOnProperty("batch.metrics.export.influxdb.enabled")
	@ConditionalOnClass(InfluxdbReporter.class)
	public Exporter influxdbExporter() throws Exception{
		return new InfluxdbMetricsExporter(metricRegistry);
	}
	
	@Scheduled(initialDelay = 60000, fixedDelay = 60000)
	@ConditionalOnBean(name = "graphiteExporter")
    void exportMetrics() {
		graphiteExporter().export();
    }

}
