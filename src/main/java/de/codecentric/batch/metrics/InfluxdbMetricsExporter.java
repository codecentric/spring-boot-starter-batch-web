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
package de.codecentric.batch.metrics;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import metrics_influxdb.Influxdb;
import metrics_influxdb.InfluxdbReporter;

import org.springframework.boot.actuate.metrics.export.Exporter;
import org.springframework.boot.actuate.metrics.reader.MetricReader;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;

/**
 * Configuration for the InfluxDB Metric Exporter.
 * 
 * @author Dennis Schulte
 */
public class InfluxdbMetricsExporter implements Exporter {

	private ScheduledReporter reporter;

	private Date lastExport = new Date();

	public InfluxdbMetricsExporter(MetricRegistry metricRegistry,
			final MetricReader metricReader, String server, Integer port, String dbName,
			String user, String password, String environment) throws Exception {
		Influxdb influxdb = new Influxdb(server, port, dbName, user, password);
		influxdb.debugJson = true;
		MetricFilter filter = new MetricFilter() {
			@Override
			public boolean matches(String name, Metric metric) {
				org.springframework.boot.actuate.metrics.Metric<?> bootMetric = metricReader
						.findOne(name);
				if (bootMetric.getTimestamp().after(lastExport)) {
					return true;
				}
				return false;
			}
		};
		reporter = InfluxdbReporter.forRegistry(metricRegistry).prefixedWith(environment)
				.convertRatesTo(TimeUnit.SECONDS)
				.convertDurationsTo(TimeUnit.MILLISECONDS).filter(filter).build(influxdb);
	}

	@Override
	public void export() {
		reporter.report();
	}

}
