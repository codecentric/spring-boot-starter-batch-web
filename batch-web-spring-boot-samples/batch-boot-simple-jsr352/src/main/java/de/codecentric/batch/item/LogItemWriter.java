package de.codecentric.batch.item;

import java.util.List;

import javax.batch.api.chunk.AbstractItemWriter;
import javax.batch.api.chunk.ItemWriter;
import javax.inject.Inject;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

/**
 * Dummy {@link ItemWriter} which only logs data it receives.
 * 
 * It also serves as an example how to inject resources (Spring beans) from the parent context into batch artifacts even
 * if they are referenced by full class name in the batch job xml.
 */
public class LogItemWriter extends AbstractItemWriter {

	private static final Logger log = LoggerFactory.getLogger(LogItemWriter.class);

	@Inject
	private DataSource dataSource;

	@Override
	public void writeItems(List<Object> items) throws Exception {
		Assert.notNull(dataSource, "DataSource should not be null");
		log.info("ItemWriter: " + items.toString());
	}

}
