package de.codecentric.batch;

import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

import de.codecentric.batch.configuration.AutomaticJobRegistrarConfiguration;
import de.codecentric.batch.configuration.BatchWebAutoConfiguration;

@Configuration
@Import({ BatchWebAutoConfiguration.class, DataSourceAutoConfiguration.class, BatchAutoConfiguration.class,
		PropertyPlaceholderAutoConfiguration.class, AutomaticJobRegistrarConfiguration.class })
@PropertySource("test.properties")
public class TestConfiguration {

}
