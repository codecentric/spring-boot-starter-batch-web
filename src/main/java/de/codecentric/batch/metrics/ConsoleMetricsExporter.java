package de.codecentric.batch.metrics;

import java.util.concurrent.TimeUnit;

import org.springframework.boot.actuate.metrics.export.Exporter;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;

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
