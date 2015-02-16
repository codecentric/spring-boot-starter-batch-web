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
import java.net.UnknownHostException;

import metrics_influxdb.InfluxdbReporter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.export.Exporter;
import org.springframework.boot.actuate.metrics.reader.MetricReader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.annotation.EnableScheduling;

import com.codahale.metrics.ConsoleReporter;
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
@ConditionalOnClass(MetricRegistry.class)
@ConditionalOnProperty("batch.metrics.enabled")
@Configuration
@EnableScheduling
public class MetricsExporterConfiguration {

	@Autowired
	private Environment env;

	@Autowired
	private MetricReader metricReader;

	@Autowired
	private MetricRegistry metricRegistry;

	@Bean
	@ConditionalOnProperty("batch.metrics.export.console.enabled")
	@ConditionalOnClass(ConsoleReporter.class)
	public Exporter consoleExporter() {
		return new ConsoleMetricsExporter(metricRegistry);
	}

	@Bean
	@ConditionalOnProperty("batch.metrics.export.graphite.enabled")
	@ConditionalOnClass(GraphiteReporter.class)
	public Exporter graphiteExporter() {
		String server = env.getProperty("batch.metrics.export.graphite.server");
		Integer port = env.getProperty(
				"batch.metrics.export.graphite.port", Integer.class, 2003);
		String environment = env.getProperty("batch.metrics.export.environment",
				getShortHostname());
		return new GraphiteMetricsExporter(metricRegistry, metricReader, server, port, environment);
	}

	@Bean
	@ConditionalOnProperty("batch.metrics.export.influxdb.enabled")
	@ConditionalOnClass(InfluxdbReporter.class)
	public Exporter influxdbExporter() throws Exception {
		String server = env.getProperty("batch.metrics.export.influxdb.server");
		Integer port = env.getProperty("batch.metrics.export.influxdb.port",
				Integer.class, 8086);
		String dbName = env.getProperty("batch.metrics.export.influxdb.db", "db1");
		String user = env.getProperty("batch.metrics.export.influxdb.username", "root");
		String password = env.getProperty("batch.metrics.export.influxdb.password",
				"root");
		String environment = env.getProperty("batch.metrics.export.environment",
				getShortHostname());
		return new InfluxdbMetricsExporter(metricRegistry, metricReader, server, port,
				dbName, user, password, environment);
	}

	private String getShortHostname() {
		try {
			String hostname = InetAddress.getLocalHost().getHostName();
			if (hostname.indexOf('.') > 0) {
				hostname = hostname.substring(0, hostname.indexOf('.'));
			}
			return hostname;
		}
		catch (UnknownHostException e) {
			return "unknown";
		}
	}

}
