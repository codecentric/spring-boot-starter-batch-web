package de.codecentric.batch.metrics;

import org.springframework.boot.actuate.metrics.CounterService;

public interface ExtendedCounterService extends CounterService{

	/**
	 * Increment the specified counter by the given value.
	 * @param metricName the name of the counter
	 * @param value the amount to increment by
	 */
	void increment(String metricName, Long value);
	
	/**
	 * Decrement the specified counter by the given value.
	 * @param metricName the name of the counter
	 * @param value the amount to increment by
	 */
	void decrement(String metricName, Long value);
	
}
