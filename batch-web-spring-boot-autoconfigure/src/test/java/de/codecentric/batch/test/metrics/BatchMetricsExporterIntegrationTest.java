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
package de.codecentric.batch.test.metrics;

import de.codecentric.batch.MetricsTestApplication;
import de.codecentric.batch.metrics.MetricsListener;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
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
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * This test class starts a batch job configured in JavaConfig and tests a simple metrics use case.
 *
 * @author Tobias Flohre
 */

@SpringBootTest(classes = MetricsTestApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT, properties = {
		"batch.metrics.enabled=true", "batch.metrics.profiling.readprocesswrite.enabled=true",
		"batch.metrics.export.console.enabled=true" })
public class BatchMetricsExporterIntegrationTest {

	private TestRestTemplate restTemplate = new TestRestTemplate();

	@Autowired
	private JobExplorer jobExplorer;

	@Autowired
	private MeterRegistry meterRegistry;

	@Value("${local.server.port}")
	int port;

	@Test
	public void testRunJob() throws InterruptedException {
		MultiValueMap<String, Object> requestMap = new LinkedMultiValueMap<>();
		requestMap.add("jobParameters", "run=2");
		Long executionId = restTemplate.postForObject(
				"http://localhost:" + port + "/batch/operations/jobs/simpleBatchMetricsJob", requestMap, Long.class);
		while (!restTemplate
				.getForObject("http://localhost:" + port + "/batch/operations/jobs/executions/{executionId}",
						String.class, executionId)
				.equals("COMPLETED")) {
			Thread.sleep(1000);
		}
		String log = restTemplate.getForObject(
				"http://localhost:" + port + "/batch/operations/jobs/executions/{executionId}/log", String.class,
				executionId);
		assertThat(log.length() > 20, is(true));
		JobExecution jobExecution = jobExplorer.getJobExecution(executionId);
		assertThat(jobExecution.getStatus(), is(BatchStatus.COMPLETED));
		String jobExecutionString = restTemplate.getForObject(
				"http://localhost:" + port + "/batch/monitoring/jobs/executions/{executionId}", String.class,
				executionId);
		assertThat(jobExecutionString.contains("COMPLETED"), is(true));
		Gauge gauge = meterRegistry.find(MetricsListener.METRIC_NAME)//
				.tag("context", "simpleBatchMetricsJob.simpleBatchMetricsStep")//
				.tag("name", "processor")//
				.gauge();
		assertThat(gauge, is(notNullValue()));
		assertThat((Double) gauge.value(), is(notNullValue()));
		assertThat((Double) gauge.value(), is(7.0));
	}

}
