/*
 * Copyright 2018 the original author or authors.
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

import de.codecentric.batch.TestApplication;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * This test class starts a batch job configured in JavaConfig that uses a JobParametersIncrementer.
 *
 * @author Tobias Flohre
 */
@SpringBootTest(classes = TestApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
public class JobParametersIncrementerIntegrationTest {

	private TestRestTemplate restTemplate = new TestRestTemplate();

	@Autowired
	private JobExplorer jobExplorer;

	@Value("${local.server.port}")
	int port;

	@Test
	public void testRunJob() throws InterruptedException {
		MultiValueMap<String, Object> requestMap = new LinkedMultiValueMap<>();
		requestMap.add("jobParameters", "param1=value1");
		Long executionId = restTemplate.postForObject(
				"http://localhost:" + port + "/batch/operations/jobs/incrementerJob", requestMap, Long.class);
		while (!restTemplate
				.getForObject("http://localhost:" + port + "/batch/operations/jobs/executions/{executionId}",
						String.class, executionId)
				.equals("COMPLETED")) {
			Thread.sleep(1000);
		}
		JobExecution jobExecution = jobExplorer.getJobExecution(executionId);
		assertThat(jobExecution.getStatus(), is(BatchStatus.COMPLETED));
		assertThat(jobExecution.getJobParameters().getLong("run.id"), is(1l));
		assertThat(jobExecution.getJobParameters().getString("param1"), is("value1"));
		executionId = restTemplate.postForObject("http://localhost:" + port + "/batch/operations/jobs/incrementerJob",
				"", Long.class);
		while (!restTemplate
				.getForObject("http://localhost:" + port + "/batch/operations/jobs/executions/{executionId}",
						String.class, executionId)
				.equals("COMPLETED")) {
			Thread.sleep(1000);
		}
		jobExecution = jobExplorer.getJobExecution(executionId);
		assertThat(jobExecution.getStatus(), is(BatchStatus.COMPLETED));
		assertThat(jobExecution.getJobParameters().getLong("run.id"), is(2l));
		requestMap = new LinkedMultiValueMap<>();
		requestMap.add("jobParameters", "param1=value1,param2=value2");
		executionId = restTemplate.postForObject("http://localhost:" + port + "/batch/operations/jobs/incrementerJob",
				requestMap, Long.class);
		while (!restTemplate
				.getForObject("http://localhost:" + port + "/batch/operations/jobs/executions/{executionId}",
						String.class, executionId)
				.equals("COMPLETED")) {
			Thread.sleep(1000);
		}
		jobExecution = jobExplorer.getJobExecution(executionId);
		assertThat(jobExecution.getStatus(), is(BatchStatus.COMPLETED));
		assertThat(jobExecution.getJobParameters().getLong("run.id"), is(3l));
		assertThat(jobExecution.getJobParameters().getString("param1"), is("value1"));
		assertThat(jobExecution.getJobParameters().getString("param2"), is("value2"));
	}

}
