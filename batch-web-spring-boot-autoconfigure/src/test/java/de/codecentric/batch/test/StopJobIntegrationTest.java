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

import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * This test class starts a batch job that needs some time to finish. It tests the endpoints monitoring running jobs and
 * then stops the job.
 *
 * @author Tobias Flohre
 */
@SpringBootTest(classes = TestApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)
public class StopJobIntegrationTest {

	private TestRestTemplate restTemplate = new TestRestTemplate();

	@Autowired
	private JobExplorer jobExplorer;

	@Value("${local.server.port}")
	int port;

	@Test
	public void testRunJob() throws InterruptedException {
		Long executionId = restTemplate.postForObject("http://localhost:" + port + "/batch/operations/jobs/delayJob",
				"", Long.class);
		Thread.sleep(500);
		String runningExecutions = restTemplate
				.getForObject("http://localhost:" + port + "/batch/monitoring/jobs/runningexecutions", String.class);
		assertThat(runningExecutions.contains(executionId.toString()), is(true));
		String runningExecutionsForDelayJob = restTemplate.getForObject(
				"http://localhost:" + port + "/batch/monitoring/jobs/runningexecutions/delayJob", String.class);
		assertThat(runningExecutionsForDelayJob.contains(executionId.toString()), is(true));
		restTemplate.delete("http://localhost:" + port + "/batch/operations/jobs/executions/{executionId}",
				executionId);
		Thread.sleep(1500);

		JobExecution jobExecution = jobExplorer.getJobExecution(executionId);
		assertThat(jobExecution.getStatus(), is(BatchStatus.STOPPED));
		String jobExecutionString = restTemplate.getForObject(
				"http://localhost:" + port + "/batch/monitoring/jobs/executions/{executionId}", String.class,
				executionId);
		assertThat(jobExecutionString.contains("STOPPED"), is(true));
	}

	@Test
	public void testGetJobNames() {
		@SuppressWarnings("unchecked")
		List<String> jobNames = restTemplate.getForObject("http://localhost:" + port + "/batch/monitoring/jobs",
				List.class);
		assertThat(jobNames.contains("delayJob"), is(true));
	}

}
