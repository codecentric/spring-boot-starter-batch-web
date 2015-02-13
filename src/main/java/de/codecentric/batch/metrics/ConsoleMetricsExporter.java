package de.codecentric.batch.metrics;

import java.util.concurrent.TimeUnit;

import org.springframework.boot.actuate.metrics.export.Exporter;
import org.springframework.boot.actuate.metrics.reader.MetricReader;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;

public class ConsoleMetricsExporter implements Exporter {

	private ConsoleReporter reporter;
	
	public ConsoleMetricsExporter(MetricRegistry metricRegistry,final MetricReader metricReader) {
		reporter = ConsoleReporter.forRegistry(metricRegistry)
				.convertRatesTo(TimeUnit.SECONDS)
				.convertDurationsTo(TimeUnit.MILLISECONDS).build();
	}

	@Override
	public void export() {
		reporter.report();
	}

}
