package de.codecentric.batch.simple.item;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;

/**
 * Dummy {@link ItemProcessor} which only logs data it receives.
 */
public class LogItemProcessor implements ItemProcessor<String, String> {

	private static final Logger LOGGER = LoggerFactory.getLogger(LogItemProcessor.class);

	@Override
	public String process(String item) throws Exception {
		LOGGER.info(item);
		return item;
	}

}
