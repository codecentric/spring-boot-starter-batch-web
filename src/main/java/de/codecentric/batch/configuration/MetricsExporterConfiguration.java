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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import metrics_influxdb.Influxdb;
import metrics_influxdb.InfluxdbReporter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;

/**
 * Configuration for the Metrics Exporters
 * 
 * @author Dennis Schulte
 */
public class MetricsExporterConfiguration {

	private static final Logger LOGGER = LoggerFactory.getLogger(MetricsExporterConfiguration.class);

	@Autowired
	private Environment env;

	@Bean
	@ConditionalOnProperty("batch.metrics.export.console.enabled")
	public ScheduledReporter consoleReporter(MetricRegistry metricRegistry) {
		ConsoleReporter reporter = ConsoleReporter.forRegistry(metricRegistry).convertRatesTo(TimeUnit.SECONDS)
				.convertDurationsTo(TimeUnit.MILLISECONDS).build();
		Integer interval = env.getProperty("batch.metrics.export.console.interval", Integer.class, 0);
		if (interval > 0) {
			reporter.start(interval, TimeUnit.MILLISECONDS);
		}
		return reporter;
	}

	@Bean
	@ConditionalOnProperty("batch.metrics.export.graphite.enabled")
	@ConditionalOnClass(GraphiteReporter.class)
	public ScheduledReporter graphiteReporter(MetricRegistry metricRegistry) {
		if (!env.containsProperty("batch.metrics.export.graphite.server")) {
			LOGGER.warn("The hostname for the Graphite server is missing (batch.metrics.export.graphite.server).");
			return null;
		}
		Graphite graphite = new Graphite(new InetSocketAddress(env.getProperty("batch.metrics.export.graphite.server"), env.getProperty(
				"batch.metrics.export.graphite.port", Integer.class, 2003)));
		String hostname;
		try {
			hostname = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			hostname = "unknown";
		}
		GraphiteReporter reporter = GraphiteReporter.forRegistry(metricRegistry).prefixedWith(hostname).convertRatesTo(TimeUnit.SECONDS)
				.convertDurationsTo(TimeUnit.MILLISECONDS).filter(MetricFilter.ALL).build(graphite);
		Integer interval = env.getProperty("batch.metrics.export.graphite.interval", Integer.class, 0);
		if (interval > 0) {
			reporter.start(interval, TimeUnit.MILLISECONDS);
		}
		return reporter;
	}
	
	@Bean
	@ConditionalOnProperty("batch.metrics.export.influxdb.enabled")
	@ConditionalOnClass(InfluxdbReporter.class)
	public ScheduledReporter influxdbReporter(MetricRegistry metricRegistry) throws Exception {
		Influxdb influxdb = new Influxdb(env.getProperty("batch.metrics.export.influxdb.server"), env.getProperty(
				"batch.metrics.export.influxdb.port", Integer.class, 8086), env.getProperty("batch.metrics.export.influxdb.db", "db1"),
				env.getProperty("batch.metrics.export.influxdb.username", "root"), env.getProperty("batch.metrics.export.influxdb.password", "root"));
		influxdb.debugJson = true;
		String hostname;
		try {
			hostname = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			hostname = "unknown";
		}
		final InfluxdbReporter reporter = InfluxdbReporter.forRegistry(metricRegistry).prefixedWith(hostname).convertRatesTo(TimeUnit.SECONDS)
				.convertDurationsTo(TimeUnit.MILLISECONDS).filter(MetricFilter.ALL).build(influxdb);
		Integer interval = env.getProperty("batch.metrics.export.influxdb.interval", Integer.class, 0);
		if (interval > 0) {			
			reporter.start(interval, TimeUnit.MILLISECONDS);
		}
		return reporter;
	}
}
