package de.codecentric.batch.metrics;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.export.Exporter;
import org.springframework.boot.actuate.metrics.reader.MetricReader;
import org.springframework.core.env.Environment;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;

public class GraphiteMetricsExporter implements Exporter {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(GraphiteMetricsExporter.class);

	@Autowired
	private Environment env;

	private ScheduledReporter reporter;


	private Date lastExport = new Date();
	
	public GraphiteMetricsExporter(MetricRegistry metricRegistry,final MetricReader metricReader) {
		if (!env.containsProperty("batch.metrics.export.graphite.server")) {
			LOGGER.warn("The hostname for the Graphite server is missing (batch.metrics.export.graphite.server).");
		}
		Graphite graphite = new Graphite(new InetSocketAddress(
				env.getProperty("batch.metrics.export.graphite.server"), env.getProperty(
						"batch.metrics.export.graphite.port", Integer.class, 2003)));
		MetricFilter filter = new MetricFilter(){
			@Override
			public boolean matches(String name, Metric metric) {
				org.springframework.boot.actuate.metrics.Metric<?> bootMetric = metricReader.findOne(name);
				if(bootMetric.getTimestamp().after(lastExport)){
					return true;
				}
				return false;
			}
		};
		reporter = GraphiteReporter
				.forRegistry(metricRegistry)
				.prefixedWith(
						env.getProperty("batch.metrics.export.environment",
								getShortHostname())).convertRatesTo(TimeUnit.SECONDS)
				.convertDurationsTo(TimeUnit.MILLISECONDS).filter(MetricFilter.ALL)
				.filter(filter).build(graphite);
	}

	@Override
	public void export() {
		reporter.report();
		lastExport = new Date();
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
