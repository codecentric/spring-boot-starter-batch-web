package de.codecentric.batch.filetodb.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.repository.ExecutionContextSerializer;
import org.springframework.batch.core.repository.dao.Jackson2ExecutionContextStringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

@Configuration
public class DataSourceConfiguration {

	@Bean
	public DataSource dataSourcePartner() {
		return new EmbeddedDatabaseBuilder()//
				.setType(EmbeddedDatabaseType.HSQL)//
				.ignoreFailedDrops(true)//
				.addScripts("classpath:org/springframework/batch/core/schema-drop-hsqldb.sql",
						"classpath:org/springframework/batch/core/schema-hsqldb.sql", "classpath:schema-partner.sql")//
				.build();
	}

	@Bean
	public ExecutionContextSerializer serializer() {
		return new CustomSerializer();
	}

	private static class CustomSerializer extends Jackson2ExecutionContextStringSerializer {

		private static final Logger LOGGER = LoggerFactory.getLogger(CustomSerializer.class);

		@Override
		public void serialize(Map<String, Object> context, OutputStream out) throws IOException {
			LOGGER.info("I will do serialization");
			super.serialize(context, out);
		}

		@Override
		public Map<String, Object> deserialize(InputStream in) throws IOException {
			LOGGER.info("I will do deserialization");
			return super.deserialize(in);
		}

	}

}
