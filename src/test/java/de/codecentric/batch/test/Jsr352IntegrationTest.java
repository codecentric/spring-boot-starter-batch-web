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
package de.codecentric.batch.test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.junit4.SpringRunner;

import de.codecentric.batch.TestApplication;

/**
 * This test class starts a JSR-352 type batch job and tests several endpoints.
 *
 * @author Tobias Flohre
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
public class Jsr352IntegrationTest {

	private TestRestTemplate restTemplate = new TestRestTemplate();

	@Autowired
	private JobExplorer jobExplorer;

	@Value("${local.server.port}")
	int port;

	@Test
	public void testRunJob() throws InterruptedException{
		Long executionId = restTemplate.postForObject("http://localhost:"+port+"/batch/operations/jobs/simpleJsr352Job", "",Long.class);
		while (!restTemplate.getForObject("http://localhost:"+port+"/batch/operations/jobs/executions/{executionId}", String.class, executionId).equals("COMPLETED")){
			Thread.sleep(1000);
		}
		String log = restTemplate.getForObject("http://localhost:"+port+"/batch/operations/jobs/executions/{executionId}/log", String.class, executionId);
		assertThat(log.length()>20,is(true));
		JobExecution jobExecution = jobExplorer.getJobExecution(executionId);
		assertThat(jobExecution.getStatus(),is(BatchStatus.COMPLETED));
		String jobExecutionString = restTemplate.getForObject("http://localhost:"+port+"/batch/monitoring/jobs/executions/{executionId}",String.class,executionId);
		assertThat(jobExecutionString.contains("COMPLETED"),is(true));
	}

	@Test
	public void testGetJobNames(){
		@SuppressWarnings("unchecked")
		List<String> jobNames = restTemplate.getForObject("http://localhost:"+port+"/batch/monitoring/jobs", List.class);
		assertThat(jobNames.contains("simpleJsr352Job"), is(true));
	}

}
