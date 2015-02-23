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

import java.util.concurrent.TimeUnit;

import org.springframework.boot.actuate.metrics.export.Exporter;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;

/**
 * Configuration for the Console Metric Exporter.
 * 
 * @author Dennis Schulte
 */
public class ConsoleMetricsExporter implements Exporter {

	private ConsoleReporter reporter;
	
	public ConsoleMetricsExporter(MetricRegistry metricRegistry) {
		reporter = ConsoleReporter.forRegistry(metricRegistry)
				.convertRatesTo(TimeUnit.SECONDS)
				.convertDurationsTo(TimeUnit.MILLISECONDS).build();
	}

	@Override
	public void export() {
		reporter.report();
	}

}
