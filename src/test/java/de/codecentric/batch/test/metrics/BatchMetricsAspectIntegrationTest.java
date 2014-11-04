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
package de.codecentric.batch.test.metrics;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.repository.MetricRepository;
import org.springframework.boot.actuate.metrics.rich.RichGauge;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.client.RestTemplate;

import de.codecentric.batch.MetricsTestApplication;

/**
 * This test class starts a batch job configured in JavaConfig and tests a simple metrics use case.
 * 
 * @author Tobias Flohre
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes=MetricsTestApplication.class)
@WebAppConfiguration
@IntegrationTest({"server.port=8091","batch.metrics.enabled=true","batch.metrics.deletemetricsonstepfinish=false","batch.metrics.aspectenabled=true"})
public class BatchMetricsAspectIntegrationTest {

	RestTemplate restTemplate = new TestRestTemplate();
	
	@Autowired
	private JobExplorer jobExplorer;
	@Autowired
	private MetricRepository metricRepository;
	
	@Test
	public void testRunJob() throws InterruptedException{
		Long executionId = restTemplate.postForObject("http://localhost:8091/batch/operations/jobs/simpleBatchMetricsJob", "",Long.class);
		while (!restTemplate.getForObject("http://localhost:8091/batch/operations/jobs/executions/{executionId}", String.class, executionId).equals("COMPLETED")){
			Thread.sleep(1000);
		}
		String log = restTemplate.getForObject("http://localhost:8091/batch/operations/jobs/executions/{executionId}/log", String.class, executionId);
		assertThat(log.length()>20,is(true));
		JobExecution jobExecution = jobExplorer.getJobExecution(executionId);
		assertThat(jobExecution.getStatus(),is(BatchStatus.COMPLETED));
		ExecutionContext stepExecutionContext = jobExecution.getStepExecutions().iterator().next().getExecutionContext();
		RichGauge readerGauge = (RichGauge) stepExecutionContext.get("DummyItemReader.read");
		RichGauge processorGauge = (RichGauge) stepExecutionContext.get("MetricsItemProcessor.process");
		RichGauge writerGauge = (RichGauge) stepExecutionContext.get("LogItemWriter.write");
		assertThat(readerGauge.getCount(),is(8L));
		assertThat(processorGauge.getCount(),is(7L));
		assertThat(writerGauge.getCount(),is(7L));
		String jobExecutionString = restTemplate.getForObject("http://localhost:8091/batch/monitoring/jobs/executions/{executionId}",String.class,executionId);
		assertThat(jobExecutionString.contains("COMPLETED"),is(true));
		
		assertThat((Long)metricRepository.findOne("counter.batch.simpleBatchMetricsJob."+jobExecution.getStepExecutions().iterator().next().getId()+".step.processor").getValue(),is(7l));
	}

}
