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

import java.io.File;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.mapping.PassThroughLineMapper;
import org.springframework.batch.item.file.transform.PassThroughLineAggregator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;

@Configuration
public class FlatFileJobConfiguration {

	@Autowired
	private JobBuilderFactory jobBuilderFactory;

	@Autowired
	private StepBuilderFactory stepBuilderFactory;

	@Bean
	public Job flatFileJob() {
		return jobBuilderFactory.get("flatFileJob").start(step()).build();
	}

	@Bean
	public Step step() {
		return stepBuilderFactory.get("step").<String, String> chunk(1).reader(reader()).writer(writer()).build();
	}

	@Bean
	public ItemReader<String> reader() {
		FlatFileItemReader<String> reader = new FlatFileItemReader<String>();
		reader.setResource(new ClassPathResource("in-javaconfig.txt"));
		reader.setLineMapper(new PassThroughLineMapper());
		return reader;
	}

	@Bean
	public ItemWriter<String> writer() {
		FlatFileItemWriter<String> writer = new FlatFileItemWriter<String>();
		writer.setResource(new FileSystemResource(new File("target/out-javaconfig.txt")));
		writer.setLineAggregator(new PassThroughLineAggregator<String>());
		return writer;
	}

}
