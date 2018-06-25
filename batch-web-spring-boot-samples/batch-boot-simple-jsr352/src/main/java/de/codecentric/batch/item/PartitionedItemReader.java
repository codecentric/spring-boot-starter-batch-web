package de.codecentric.batch.item;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.batch.api.BatchProperty;
import javax.batch.api.chunk.AbstractItemReader;
import javax.inject.Inject;

public class PartitionedItemReader extends AbstractItemReader {
	
	private static Map<String, String[]> data = new ConcurrentHashMap<>();
	static {
		data.put("key1", new String[]{"Good", "morning!","This","is","your","ItemReader","speaking!",null});
		data.put("key2", new String[]{"Eins", "zwei","Polizei","drei","vier","Grenadier",null});
		data.put("key3", new String[]{"Heja", "BVB","Heja","BVB","Heja","Heja","Heja","BVB",null});
	}

	private int index = 0;
	
	@Inject @BatchProperty(name="datakey")
	private String key;

	@Override
	public Object readItem() throws Exception {
		return data.get(key)[index++];
	}

}
