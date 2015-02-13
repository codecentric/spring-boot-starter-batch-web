package de.codecentric.batch.metrics;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import metrics_influxdb.Influxdb;
import metrics_influxdb.InfluxdbReporter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.export.Exporter;
import org.springframework.core.env.Environment;

import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;

public class InfluxdbMetricsExporter implements Exporter {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(InfluxdbMetricsExporter.class);

	@Autowired
	private Environment env;

	private ScheduledReporter reporter;


	public InfluxdbMetricsExporter(MetricRegistry metricRegistry) throws Exception {
		if (!env.containsProperty("batch.metrics.export.graphite.server")) {
			LOGGER.warn("The hostname for the Graphite server is missing (batch.metrics.export.graphite.server).");
		}
		Influxdb influxdb = new Influxdb(
				env.getProperty("batch.metrics.export.influxdb.server"), env.getProperty(
						"batch.metrics.export.influxdb.port", Integer.class, 8086),
				env.getProperty("batch.metrics.export.influxdb.db", "db1"),
				env.getProperty("batch.metrics.export.influxdb.username", "root"),
				env.getProperty("batch.metrics.export.influxdb.password", "root"));
		influxdb.debugJson = true;
		reporter = InfluxdbReporter.forRegistry(metricRegistry)
				.prefixedWith(env.getProperty("batch.metrics.export.environment",
						getShortHostname())).convertRatesTo(TimeUnit.SECONDS)
				.convertDurationsTo(TimeUnit.MILLISECONDS).filter(MetricFilter.ALL)
				.build(influxdb);
	}

	@Override
	public void export() {
		reporter.report();
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
