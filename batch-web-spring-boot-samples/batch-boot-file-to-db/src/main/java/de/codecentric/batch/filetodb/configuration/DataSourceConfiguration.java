package de.codecentric.batch.filetodb.configuration;

import javax.sql.DataSource;

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
}
