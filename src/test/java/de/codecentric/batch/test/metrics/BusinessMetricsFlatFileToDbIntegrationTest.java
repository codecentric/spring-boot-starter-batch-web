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

import java.sql.SQLException;

import javax.sql.DataSource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.metrics.repository.MetricRepository;
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptException;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import de.codecentric.batch.TestApplication;
import de.codecentric.batch.metrics.MetricNames;

/**
 * This test class includes several tests that start a batch job configured in JavaConfig testing different metrics use cases.
 * 
 * @author Tobias Flohre
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes=TestApplication.class)
@WebAppConfiguration
@IntegrationTest({"batch.metrics.enabled=true","batch.metrics.deletemetricsonstepfinish=false"})
public class BusinessMetricsFlatFileToDbIntegrationTest {

	RestTemplate restTemplate = new TestRestTemplate();
	
	@Autowired
	private JobExplorer jobExplorer;
	@Autowired
	private MetricRepository metricRepository;
	@Autowired
	private DataSource dataSource;
	
	@Before
	public void setUp() throws ScriptException, SQLException{
		try {
			ScriptUtils.executeSqlScript(dataSource.getConnection(), new ClassPathResource("metrics/create-schema.sql"));
		} catch (Exception e){
			// if table exist, error is okay.
		}
	}
	
	@After
	public void tearDown(){
		new JdbcTemplate(dataSource).execute("DELETE FROM ITEM");
	}
	
	@Test
	public void testRunFlatFileToDbNoSkipJob_Success() throws InterruptedException{
		JobExecution jobExecution = runJob("flatFileToDbNoSkipJob","metrics/flatFileToDbNoSkipJob_Success.csv");
		assertThat(jobExecution.getStatus(),is(BatchStatus.COMPLETED));
		ExecutionContext executionContext = jobExecution.getStepExecutions().iterator().next().getExecutionContext();
		MetricValidator validator = MetricValidatorBuilder.metricValidator().withExecutionContext(executionContext)
				.withBeforeChunkCount(2L).withStreamOpenCount(1L).withStreamUpdateCount(3L).withStreamCloseCount(0L)
				.withBeforeReadCount(6L).withReadCount(6L).withAfterReadCount(5L).withReadErrorCount(0L).withBeforeProcessCount(5L)
				.withProcessCount(5L).withAfterProcessCount(5L).withProcessErrorCount(0L).withBeforeWriteCount(5L)
				.withWriteCount(5L).withAfterWriteCount(5L).withAfterChunkCount(2L).withChunkErrorCount(0L)
				.withSkipInReadCount(0L).withSkipInProcessCount(0L).withSkipInWriteCount(0L).build();
		validator.validate();
		// if one is correct, all will be in the MetricRepository, so I check just one
		assertThat((Long)metricRepository.findOne("counter.batch.flatFileToDbNoSkipJob.0.step."+MetricNames.PROCESS_COUNT.getName()).getValue(),is(5l));
	}

	@Test
	public void testRunFlatFileToDbNoSkipJob_Failed() throws InterruptedException{
		JobExecution jobExecution = runJob("flatFileToDbNoSkipJob","metrics/flatFileToDbNoSkipJob_Failed.csv");
		assertThat(jobExecution.getStatus(),is(BatchStatus.FAILED));
		ExecutionContext executionContext = jobExecution.getStepExecutions().iterator().next().getExecutionContext();
		MetricValidator validator = MetricValidatorBuilder.metricValidator().withExecutionContext(executionContext)
				.withBeforeChunkCount(1L).withStreamOpenCount(1L).withStreamUpdateCount(2L).withStreamCloseCount(0L)
				.withBeforeReadCount(6L).withReadCount(6L).withAfterReadCount(5L).withReadErrorCount(0L).withBeforeProcessCount(3L)
				.withProcessCount(3L).withAfterProcessCount(3L).withProcessErrorCount(1L).withBeforeWriteCount(3L)
				.withWriteCount(3L).withAfterWriteCount(3L).withAfterChunkCount(1L).withChunkErrorCount(1L)
				.withSkipInReadCount(0L).withSkipInProcessCount(0L).withSkipInWriteCount(0L).build();
		validator.validate();
		// if one is correct, all will be in the MetricRepository, so I check just one
		assertThat((Long)metricRepository.findOne("counter.batch.flatFileToDbNoSkipJob.0.step."+MetricNames.PROCESS_COUNT.getName()).getValue(),is(3l));
	}

	private JobExecution runJob(String jobName, String pathToFile) throws InterruptedException {
		MultiValueMap<String, Object> requestMap = new LinkedMultiValueMap<>();
		requestMap.add("jobParameters", "pathToFile="+pathToFile);
		Long executionId = restTemplate.postForObject("http://localhost:8090/batch/operations/jobs/"+jobName, requestMap,Long.class);
		while (!restTemplate.getForObject("http://localhost:8090/batch/operations/jobs/executions/{executionId}", String.class, executionId).equals("COMPLETED") &&
				!restTemplate.getForObject("http://localhost:8090/batch/operations/jobs/executions/{executionId}", String.class, executionId).equals("FAILED")){
			Thread.sleep(1000);
		}
		JobExecution jobExecution = jobExplorer.getJobExecution(executionId);
		return jobExecution;
	}
	
}
