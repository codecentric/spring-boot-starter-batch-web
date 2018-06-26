package de.codecentric.batch.simple.item;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemWriter;

/**
 * Dummy {@link ItemWriter} which only logs data it receives.
 */
public class LogItemWriter implements ItemWriter<Object> {

	private static final Logger LOGGER = LoggerFactory.getLogger(LogItemWriter.class);

	/**
	 * @see ItemWriter#write(java.util.List)
	 */
	@Override
	public void write(List<? extends Object> data) throws Exception {
		LOGGER.info("{}", data);
	}

}
