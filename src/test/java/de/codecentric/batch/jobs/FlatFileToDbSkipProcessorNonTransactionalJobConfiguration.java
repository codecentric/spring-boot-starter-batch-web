/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package de.codecentric.batch.jobs;

import javax.sql.DataSource;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.database.BeanPropertyItemSqlParameterSourceProvider;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

import de.codecentric.batch.metrics.Item;
import de.codecentric.batch.metrics.MetricsTestException;
import de.codecentric.batch.metrics.business.BatchMetrics;
import de.codecentric.batch.metrics.item.MetricsTestSkipListener;
import de.codecentric.batch.metrics.item.MetricsTestChunkListener;
import de.codecentric.batch.metrics.item.MetricsTestItemProcessListener;
import de.codecentric.batch.metrics.item.MetricsTestItemProcessor;
import de.codecentric.batch.metrics.item.MetricsTestItemReadListener;
import de.codecentric.batch.metrics.item.MetricsTestItemReader;
import de.codecentric.batch.metrics.item.MetricsTestItemWriteListener;
import de.codecentric.batch.metrics.item.MetricsTestItemWriter;

/**
 * @author Tobias Flohre
 */
@Configuration
@ConditionalOnProperty("batch.metrics.enabled")
public class FlatFileToDbSkipProcessorNonTransactionalJobConfiguration {

	private static final String OVERRIDDEN_BY_EXPRESSION = null;
	
	@Autowired
	private JobBuilderFactory jobBuilders;
	
	@Autowired
	private StepBuilderFactory stepBuilders;
	
	@Autowired
	private BatchMetrics businessMetrics;
	
	@Autowired
	private DataSource dataSource;
	
	@Bean
	public Job flatFileToDbNoSkipJob(){
		return jobBuilders.get("flatFileToDbSkipProcessorNonTransactionalJob")
				.start(step())
				.build();
	}
	
	@Bean
	public Step step(){
		return stepBuilders.get("step")
				.<Item,Item>chunk(3)
    			.reader(reader())
				.processor(processor())
				.writer(writer())
				.listener(readListener())
				.listener(processListener())
				.listener(writeListener())
				.faultTolerant()
				.processorNonTransactional()
				.skip(MetricsTestException.class)
				.skipLimit(4)
				.listener(skipListener())
				.listener(chunkListener())
				.build();
	}
	
	@Bean
	@StepScope
	public MetricsTestItemReader reader(){
		return new MetricsTestItemReader(flatFileItemReader(OVERRIDDEN_BY_EXPRESSION), businessMetrics, false);
	}
	
	@Bean
	@StepScope
	public FlatFileItemReader<Item> flatFileItemReader(@Value("#{jobParameters[pathToFile]}") String pathToFile){
		FlatFileItemReader<Item> itemReader = new FlatFileItemReader<Item>();
		itemReader.setLineMapper(lineMapper());
		itemReader.setResource(new FileSystemResource("src/test/resources/"+pathToFile));
		return itemReader;
	}
	
	@Bean
	public LineMapper<Item> lineMapper(){
		DefaultLineMapper<Item> lineMapper = new DefaultLineMapper<Item>();
		DelimitedLineTokenizer lineTokenizer = new DelimitedLineTokenizer();
		lineTokenizer.setNames(new String[]{"id","description","firstAction","secondAction"});
		lineTokenizer.setIncludedFields(new int[]{0,1,2,3});
		BeanWrapperFieldSetMapper<Item> fieldSetMapper = new BeanWrapperFieldSetMapper<Item>();
		fieldSetMapper.setTargetType(Item.class);
		lineMapper.setLineTokenizer(lineTokenizer);
		lineMapper.setFieldSetMapper(fieldSetMapper);
		return lineMapper;
	}
	
	@Bean
	public MetricsTestItemProcessor processor(){
		return new MetricsTestItemProcessor(businessMetrics, false);
	}
	
	@Bean
	public MetricsTestItemWriter writer(){
		return new MetricsTestItemWriter(jdbcBatchItemWriter(), businessMetrics);
	}
	
	@Bean
	public JdbcBatchItemWriter<Item> jdbcBatchItemWriter(){
		JdbcBatchItemWriter<Item> itemWriter = new JdbcBatchItemWriter<Item>();
		itemWriter.setSql("INSERT INTO ITEM (ID, DESCRIPTION) VALUES (:id,:description)");
		itemWriter.setDataSource(dataSource);
		itemWriter.setItemSqlParameterSourceProvider(new BeanPropertyItemSqlParameterSourceProvider<Item>());
		return itemWriter;
	}
	
	@Bean
	public MetricsTestItemReadListener readListener(){
		return new MetricsTestItemReadListener(businessMetrics, false);
	}
	
	@Bean
	public MetricsTestItemProcessListener processListener(){
		return new MetricsTestItemProcessListener(businessMetrics, false);
	}
	
	@Bean
	public MetricsTestItemWriteListener writeListener(){
		return new MetricsTestItemWriteListener(businessMetrics);
	}
	
	@Bean
	public MetricsTestChunkListener chunkListener(){
		return new MetricsTestChunkListener(businessMetrics);
	}
	
	@Bean
	public MetricsTestSkipListener skipListener(){
		return new MetricsTestSkipListener(businessMetrics);
	}
	
}
