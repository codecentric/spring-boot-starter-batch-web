package de.codecentric.batch.configuration;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

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
	public ScheduledReporter consoleReporter(MetricRegistry metricRegistry) {
		ConsoleReporter reporter = ConsoleReporter.forRegistry(metricRegistry).convertRatesTo(TimeUnit.SECONDS)
				.convertDurationsTo(TimeUnit.MILLISECONDS).build();
		reporter.start(env.getProperty("batch.metrics.export.console.interval", Integer.class, 10), TimeUnit.MINUTES);
		return reporter;
	}

	@Bean
	@ConditionalOnProperty("batch.metrics.export.graphite.enabled")
	@ConditionalOnClass(GraphiteReporter.class)
	public ScheduledReporter graphiteReporter(MetricRegistry metricRegistry) {
		Graphite graphite = new Graphite(new InetSocketAddress(env.getProperty("batch.metrics.export.graphite.server"), env.getProperty(
				"batch.metrics.v2.export.graphite.server", Integer.class, 2003)));
		String hostname;
		try {
			hostname = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			hostname = "unknown";
		}
		GraphiteReporter reporter = GraphiteReporter.forRegistry(metricRegistry).prefixedWith(hostname).convertRatesTo(TimeUnit.SECONDS)
				.convertDurationsTo(TimeUnit.MILLISECONDS).filter(MetricFilter.ALL).build(graphite);
		reporter.start(env.getProperty("batch.metrics.export.graphite.interval", Integer.class, 10), TimeUnit.MINUTES);
		return reporter;
	}

	// @PostConstruct
	// public void configureReporter() throws Exception {
	// final Influxdb influxdb = new Influxdb("192.168.59.103", 8086, "mydata", "root", "root");
	// influxdb.debugJson = true; // to print json on System.err
	// // influxdb.jsonBuilder = new MyJsonBuildler(); // to use MyJsonBuilder to create json
	// final InfluxdbReporter reporter = InfluxdbReporter.forRegistry(metricRegistry)
	// // .prefixedWith("test")
	// .convertRatesTo(TimeUnit.SECONDS).convertDurationsTo(TimeUnit.MILLISECONDS).filter(MetricFilter.ALL).build(influxdb);
	// reporter.start(10, TimeUnit.SECONDS);
	// }
}
