package de.codecentric.batch.configuration;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

import metrics_influxdb.Influxdb;
import metrics_influxdb.InfluxdbReporter;

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

	@Autowired
	private Environment env;

	@Bean
	@ConditionalOnProperty("batch.metrics.export.console.enabled")
	@ConditionalOnClass(MetricRegistry.class)
	public ScheduledReporter consoleReporter(MetricRegistry metricRegistry) {
		ConsoleReporter reporter = ConsoleReporter.forRegistry(metricRegistry).convertRatesTo(TimeUnit.SECONDS)
				.convertDurationsTo(TimeUnit.MILLISECONDS).build();
		reporter.start(env.getProperty("batch.metrics.export.console.interval", Integer.class, 10000), TimeUnit.MILLISECONDS);
		return reporter;
	}

	@Bean
	@ConditionalOnProperty("batch.metrics.export.graphite.enabled")
	@ConditionalOnClass(GraphiteReporter.class)
	public ScheduledReporter graphiteReporter(MetricRegistry metricRegistry) {
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
		reporter.start(env.getProperty("batch.metrics.export.graphite.interval", Integer.class, 10000), TimeUnit.MILLISECONDS);
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
		reporter.start(env.getProperty("batch.metrics.export.influxdb.interval", Integer.class, 10000), TimeUnit.MILLISECONDS);
		return reporter;
	}

}
