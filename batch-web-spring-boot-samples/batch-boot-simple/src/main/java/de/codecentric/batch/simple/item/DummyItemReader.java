package de.codecentric.batch.simple.item;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemReader;

/**
 * {@link ItemReader} with hard-coded input data.
 */
public class DummyItemReader implements ItemReader<String> {

	private static final Logger LOGGER = LoggerFactory.getLogger(DummyItemReader.class);

	private String[] input = { "Good", "morning!", "This", "is", "your", "ItemReader", "speaking!" };

	private int index = 0;

	/**
	 * Reads next record from input
	 */
	@Override
	public String read() throws Exception {
		String item = null;
		if (index < input.length) {
			item = input[index++];
			LOGGER.info(item);
			return item;
		} else {
			return null;
		}

	}

}
