package de.codecentric.batch.simplejsr.item;

import javax.batch.api.chunk.ItemProcessor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dummy {@link ItemProcessor} which only logs data it receives.
 */
public class LogItemProcessor implements ItemProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(LogItemProcessor.class);

	@Override
	public Object processItem(Object item) throws Exception {
		LOGGER.info("ItemProcessor: {}", item);
		return item;
	}

}
