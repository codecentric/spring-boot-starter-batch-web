package de.codecentric.batch.item;

import javax.batch.api.chunk.AbstractItemReader;

public class DummyItemReader extends AbstractItemReader {

	private String[] input = {"Good", "morning!","This","is","your","ItemReader","speaking!",null};
	
	private int index = 0;

	@Override
	public Object readItem() throws Exception {
		return input[index++];
	}

}
