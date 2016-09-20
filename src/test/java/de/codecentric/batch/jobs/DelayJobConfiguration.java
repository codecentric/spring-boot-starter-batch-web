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

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import de.codecentric.batch.item.DelayItemProcessor;
import de.codecentric.batch.item.DummyItemReader;
import de.codecentric.batch.item.LogItemWriter;

@Configuration
public class DelayJobConfiguration {
	
	@Autowired
	private JobBuilderFactory jobBuilderFactory;
	
	@Autowired
	private StepBuilderFactory stepBuilderFactory;
	
	@Bean
	public Job delayJob(){
		return jobBuilderFactory.get("delayJob")
				.start(step())
				.build();
	}
	
	@Bean
	public Step step(){
		return stepBuilderFactory.get("step")
				.<String,String>chunk(1)
				.reader(reader())
				.processor(processor())
				.writer(writer())
				.build();
	}

	@Bean
	public LogItemWriter writer() {
		return new LogItemWriter();
	}

	@Bean
	public DelayItemProcessor processor() {
		return new DelayItemProcessor();
	}

	@Bean
	public DummyItemReader reader() {
		return new DummyItemReader();
	}
	
}
