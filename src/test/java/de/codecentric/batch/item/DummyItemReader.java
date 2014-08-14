package de.codecentric.batch.item;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.item.ItemReader;

/**
 * {@link ItemReader} with hard-coded input data.
 */
public class DummyItemReader implements ItemReader<String> {
	
	private static final Log log = LogFactory.getLog(DummyItemReader.class);
	
	private String[] input = {"Good", "morning!","This","is","your","ItemReader","speaking!"};
	
	private int index = 0;
	
	/**
	 * Reads next record from input
	 */
	public String read() throws Exception {
		String item = null;
		if (index < input.length) {
			item = input[index++];
			log.info(item);
			return item;
		}
		else {
			return null;
		}
		
	}

}
