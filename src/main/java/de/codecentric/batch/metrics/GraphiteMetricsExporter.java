package de.codecentric.batch.metrics;

import java.net.InetSocketAddress;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.springframework.boot.actuate.metrics.export.Exporter;
import org.springframework.boot.actuate.metrics.reader.MetricReader;

import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;

public class GraphiteMetricsExporter implements Exporter {

	private ScheduledReporter reporter;

	private Date lastExport = new Date();

	public GraphiteMetricsExporter(MetricRegistry metricRegistry,final MetricReader metricReader, String server, Integer port, String environment) {
		Graphite graphite = new Graphite(new InetSocketAddress(
				server, port));
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
				.prefixedWith(environment).convertRatesTo(TimeUnit.SECONDS)
				.convertDurationsTo(TimeUnit.MILLISECONDS)
				.filter(filter).build(graphite);
	}

	@Override
	public void export() {
		reporter.report();
		lastExport = new Date();
	}

}
