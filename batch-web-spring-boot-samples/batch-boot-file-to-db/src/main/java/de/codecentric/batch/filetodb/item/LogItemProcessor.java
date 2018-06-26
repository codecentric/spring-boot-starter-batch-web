package de.codecentric.batch.filetodb.item;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;

/**
 * Dummy {@link ItemProcessor} which only logs data it receives.
 */
public class LogItemProcessor implements ItemProcessor<Object, Object> {

	private static final Logger LOGGER = LoggerFactory.getLogger(LogItemProcessor.class);

	private ExampleService exampleService;

	@Override
	public Object process(Object item) throws Exception {
		LOGGER.info("{}", exampleService.echo("test"));
		return item;
	}

	public void setExampleService(ExampleService exampleService) {
		this.exampleService = exampleService;
	}
}
