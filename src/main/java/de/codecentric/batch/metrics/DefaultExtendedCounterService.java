package de.codecentric.batch.metrics;

import org.springframework.boot.actuate.metrics.writer.DefaultCounterService;
import org.springframework.boot.actuate.metrics.writer.Delta;
import org.springframework.boot.actuate.metrics.writer.MetricWriter;

public class DefaultExtendedCounterService implements ExtendedCounterService {

	private final MetricWriter writer;

	/**
	 * Create a {@link DefaultCounterService} instance.
	 * 
	 * @param writer
	 *            the underlying writer used to manage metrics
	 */
	public DefaultExtendedCounterService(MetricWriter writer) {
		this.writer = writer;
	}

	@Override
	public void increment(String metricName) {
		increment(metricName, 1L);
	}

	@Override
	public void increment(String metricName, Long value) {
		this.writer.increment(new Delta<Long>(wrap(metricName), value));
	}

	@Override
	public void decrement(String metricName) {
		decrement(metricName, 1L);
	}

	@Override
	public void decrement(String metricName, Long value) {
		this.writer.increment(new Delta<Long>(wrap(metricName), -value));
	}

	@Override
	public void reset(String metricName) {
		this.writer.reset(wrap(metricName));
	}

	private String wrap(String metricName) {
		if (metricName.startsWith("counter") || metricName.startsWith("meter")) {
			return metricName;
		}
		return "counter." + metricName;
	}

}
