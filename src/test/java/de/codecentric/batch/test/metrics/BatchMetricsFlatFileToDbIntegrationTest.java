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

import java.io.File;
import java.io.IOException;
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
import org.springframework.util.FileCopyUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import de.codecentric.batch.MetricsTestApplication;
import de.codecentric.batch.metrics.MetricNames;

/**
 * This test class includes several tests that start a batch job configured in JavaConfig testing different metrics use cases.
 * 
 * @author Tobias Flohre
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes=MetricsTestApplication.class)
@WebAppConfiguration
@IntegrationTest({"server.port=8091","batch.metrics.enabled=true","batch.metrics.deletemetricsonstepfinish=false","batch.metrics.profiling.readprocesswrite.enabled=true"})
public class BatchMetricsFlatFileToDbIntegrationTest {
	
	RestTemplate restTemplate = new TestRestTemplate();
	
	@Autowired
	private JobExplorer jobExplorer;
	@Autowired
	private MetricRepository metricRepository;
	@Autowired
	private DataSource dataSource;
	
	private JdbcTemplate jdbcTemplate;
	
	@Before
	public void setUp() throws ScriptException, SQLException{
		jdbcTemplate = new JdbcTemplate(dataSource);
		try {
			ScriptUtils.executeSqlScript(dataSource.getConnection(), new ClassPathResource("metrics/create-schema.sql"));
		} catch (Exception e){
			// if table exist, error is okay.
		}
	}
	
	@After
	public void tearDown(){
		jdbcTemplate.execute("DELETE FROM ITEM");
	}
	
	@Test
	public void testRunFlatFileToDbNoSkipJob_Success() throws InterruptedException{
		JobExecution jobExecution = runJob("flatFileToDbNoSkipJob","metrics/flatFileToDbNoSkipJob_Success.csv");
		assertThat(jobExecution.getStatus(),is(BatchStatus.COMPLETED));
		ExecutionContext executionContext = jobExecution.getStepExecutions().iterator().next().getExecutionContext();
		long writeCount = 5L;
		MetricValidator validator = MetricValidatorBuilder.metricValidator().withExecutionContext(executionContext)
				.withBeforeChunkCount(2L).withStreamOpenCount(1L).withStreamUpdateCount(3L).withStreamCloseCount(0L)
				.withBeforeReadCount(6L).withReadCount(6L).withAfterReadCount(5L).withReadErrorCount(0L).withBeforeProcessCount(5L)
				.withProcessCount(5L).withAfterProcessCount(5L).withProcessErrorCount(0L).withBeforeWriteCount(5L)
				.withWriteCount(writeCount).withAfterWriteCount(5L).withAfterChunkCount(2L).withChunkErrorCount(0L)
				.withSkipInReadCount(0L).withSkipInProcessCount(0L).withSkipInWriteCount(0L).build();
		validator.validate();
		// if one is correct, all will be in the MetricRepository, so I check just one
		assertThat((Long)metricRepository.findOne("counter.batch.flatFileToDbNoSkipJob."+jobExecution.getStepExecutions().iterator().next().getId()+".step."+MetricNames.PROCESS_COUNT.getName()).getValue(),is(5l));
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ITEM", Long.class),is(writeCount));
	}

	@Test
	public void testRunFlatFileToDbNoSkipJob_Failed() throws InterruptedException{
		JobExecution jobExecution = runJob("flatFileToDbNoSkipJob","metrics/flatFileToDbNoSkipJob_Failed.csv");
		assertThat(jobExecution.getStatus(),is(BatchStatus.FAILED));
		ExecutionContext executionContext = jobExecution.getStepExecutions().iterator().next().getExecutionContext();
		long writeCount = 3L;
		MetricValidator validator = MetricValidatorBuilder.metricValidator().withExecutionContext(executionContext)
				.withBeforeChunkCount(1L).withStreamOpenCount(1L).withStreamUpdateCount(2L).withStreamCloseCount(0L)
				.withBeforeReadCount(6L).withReadCount(6L).withAfterReadCount(5L).withReadErrorCount(0L).withBeforeProcessCount(3L)
				.withProcessCount(3L).withAfterProcessCount(3L).withProcessErrorCount(1L).withBeforeWriteCount(3L)
				.withWriteCount(writeCount).withAfterWriteCount(3L).withAfterChunkCount(1L).withChunkErrorCount(1L)
				.withSkipInReadCount(0L).withSkipInProcessCount(0L).withSkipInWriteCount(0L).build();
		validator.validate();
		// if one is correct, all will be in the MetricRepository, so I check just one
		assertThat((Long)metricRepository.findOne("counter.batch.flatFileToDbNoSkipJob."+jobExecution.getStepExecutions().iterator().next().getId()+".step."+MetricNames.PROCESS_COUNT.getName()).getValue(),is(3l));
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ITEM", Long.class),is(writeCount));
	}

	@Test
	public void testRunFlatFileToDbNoSkipJob_Restart() throws InterruptedException, IOException{
		FileCopyUtils.copy(new File("src/test/resources/metrics/flatFileToDbNoSkipJob_Restart_FirstRun.csv"), new File("src/test/resources/metrics/flatFileToDbNoSkipJob_Restart.csv"));
		JobExecution jobExecution = runJob("flatFileToDbNoSkipJob","metrics/flatFileToDbNoSkipJob_Restart.csv");
		assertThat(jobExecution.getStatus(),is(BatchStatus.FAILED));
		ExecutionContext executionContext = jobExecution.getStepExecutions().iterator().next().getExecutionContext();
		long writeCount = 3L;
		MetricValidator validator = MetricValidatorBuilder.metricValidator().withExecutionContext(executionContext)
				.withBeforeChunkCount(1L).withStreamOpenCount(1L).withStreamUpdateCount(2L).withStreamCloseCount(0L)
				.withBeforeReadCount(6L).withReadCount(6L).withAfterReadCount(5L).withReadErrorCount(0L).withBeforeProcessCount(3L)
				.withProcessCount(3L).withAfterProcessCount(3L).withProcessErrorCount(1L).withBeforeWriteCount(3L)
				.withWriteCount(writeCount).withAfterWriteCount(3L).withAfterChunkCount(1L).withChunkErrorCount(1L)
				.withSkipInReadCount(0L).withSkipInProcessCount(0L).withSkipInWriteCount(0L).build();
		validator.validate();
		// if one is correct, all will be in the MetricRepository, so I check just one
		assertThat((Long)metricRepository.findOne("counter.batch.flatFileToDbNoSkipJob."+jobExecution.getStepExecutions().iterator().next().getId()+".step."+MetricNames.PROCESS_COUNT.getName()).getValue(),is(3l));
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ITEM", Long.class),is(writeCount));

		FileCopyUtils.copy(new File("src/test/resources/metrics/flatFileToDbNoSkipJob_Restart_SecondRun.csv"), new File("src/test/resources/metrics/flatFileToDbNoSkipJob_Restart.csv"));
		jobExecution = runJob("flatFileToDbNoSkipJob","metrics/flatFileToDbNoSkipJob_Restart.csv");
		assertThat(jobExecution.getStatus(),is(BatchStatus.COMPLETED));
		executionContext = jobExecution.getStepExecutions().iterator().next().getExecutionContext();
		writeCount = 8L;
		validator = MetricValidatorBuilder.metricValidator().withExecutionContext(executionContext).withValidateGauge(false)
				.withBeforeChunkCount(3L).withStreamOpenCount(2L).withStreamUpdateCount(5L).withStreamCloseCount(0L)
				.withBeforeReadCount(12L).withReadCount(12L).withAfterReadCount(10L).withReadErrorCount(0L).withBeforeProcessCount(8L)
				.withProcessCount(8L).withAfterProcessCount(8L).withProcessErrorCount(1L).withBeforeWriteCount(8L)
				.withWriteCount(writeCount).withAfterWriteCount(8L).withAfterChunkCount(3L).withChunkErrorCount(1L)
				.withSkipInReadCount(0L).withSkipInProcessCount(0L).withSkipInWriteCount(0L).build();
		validator.validate();
		// if one is correct, all will be in the MetricRepository, so I check just one
		// processCount is 5 for second run, metrics aren't cumulated
		assertThat((Long)metricRepository.findOne("counter.batch.flatFileToDbNoSkipJob."+jobExecution.getStepExecutions().iterator().next().getId()+".step."+MetricNames.PROCESS_COUNT.getName()).getValue(),is(5l));
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ITEM", Long.class),is(writeCount));
		new File("src/test/resources/metrics/flatFileToDbNoSkipJob_Restart.csv").delete();
	}

	@Test
	public void testRunFlatFileToDbSkipJob_SkipInProcess() throws InterruptedException{
		JobExecution jobExecution = runJob("flatFileToDbSkipJob","metrics/flatFileToDbSkipJob_SkipInProcess.csv");
		assertThat(jobExecution.getStatus(),is(BatchStatus.COMPLETED));
		ExecutionContext executionContext = jobExecution.getStepExecutions().iterator().next().getExecutionContext();
		long writeCount = 7L;
		MetricValidator validator = MetricValidatorBuilder.metricValidator().withExecutionContext(executionContext)
				.withBeforeChunkCount(3L).withStreamOpenCount(1L).withStreamUpdateCount(4L).withStreamCloseCount(0L)
				.withBeforeReadCount(9L).withReadCount(9L).withAfterReadCount(8L).withReadErrorCount(0L).withBeforeProcessCount(7L)
				.withProcessCount(7L).withAfterProcessCount(7L).withProcessErrorCount(1L).withBeforeWriteCount(7L)
				.withWriteCount(writeCount).withAfterWriteCount(7L).withAfterChunkCount(3L).withChunkErrorCount(1L)
				.withSkipInReadCount(0L).withSkipInProcessCount(1L).withSkipInWriteCount(0L).build();
		validator.validate();
		// if one is correct, all will be in the MetricRepository, so I check just one
		assertThat((Long)metricRepository.findOne("counter.batch.flatFileToDbSkipJob."+jobExecution.getStepExecutions().iterator().next().getId()+".step."+MetricNames.PROCESS_COUNT.getName()).getValue(),is(7l));
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ITEM", Long.class),is(writeCount));
	}

	@Test
	public void testRunFlatFileToDbSkipJob_SkipInProcess_Failed() throws InterruptedException{
		JobExecution jobExecution = runJob("flatFileToDbSkipJob","metrics/flatFileToDbSkipJob_SkipInProcess_Failed.csv");
		assertThat(jobExecution.getStatus(),is(BatchStatus.FAILED));
		ExecutionContext executionContext = jobExecution.getStepExecutions().iterator().next().getExecutionContext();
		long writeCount = 7L;
		MetricValidator validator = MetricValidatorBuilder.metricValidator().withExecutionContext(executionContext)
				.withBeforeChunkCount(3L).withStreamOpenCount(1L).withStreamUpdateCount(4L).withStreamCloseCount(0L)
				.withBeforeReadCount(12L).withReadCount(12L).withAfterReadCount(12L).withReadErrorCount(0L).withBeforeProcessCount(7L)
				.withProcessCount(7L).withAfterProcessCount(7L).withProcessErrorCount(5L).withBeforeWriteCount(7L)
				.withWriteCount(writeCount).withAfterWriteCount(7L).withAfterChunkCount(3L).withChunkErrorCount(6L)
				.withSkipInReadCount(0L).withSkipInProcessCount(2L).withSkipInWriteCount(0L).build();
		validator.validate();
		// if one is correct, all will be in the MetricRepository, so I check just one
		assertThat((Long)metricRepository.findOne("counter.batch.flatFileToDbSkipJob."+jobExecution.getStepExecutions().iterator().next().getId()+".step."+MetricNames.PROCESS_COUNT.getName()).getValue(),is(7l));
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ITEM", Long.class),is(writeCount));
	}

	@Test
	public void testRunFlatFileToDbSkipJob_SkipInWrite() throws InterruptedException{
		JobExecution jobExecution = runJob("flatFileToDbSkipJob","metrics/flatFileToDbSkipJob_SkipInWrite.csv");
		assertThat(jobExecution.getStatus(),is(BatchStatus.COMPLETED));
		ExecutionContext executionContext = jobExecution.getStepExecutions().iterator().next().getExecutionContext();
		long writeCount = 7L;
		MetricValidator validator = MetricValidatorBuilder.metricValidator().withExecutionContext(executionContext)
				.withBeforeChunkCount(4L).withStreamOpenCount(1L).withStreamUpdateCount(4L).withStreamCloseCount(0L)
				.withBeforeReadCount(9L).withReadCount(9L).withAfterReadCount(8L).withReadErrorCount(0L).withBeforeProcessCount(7L)
				.withProcessCount(7L).withAfterProcessCount(7L).withProcessErrorCount(0L).withBeforeWriteCount(5L)
				.withWriteCount(writeCount).withAfterWriteCount(7L).withWriteErrorCount(4L).withAfterChunkCount(4L).withChunkErrorCount(2L)
				.withSkipInReadCount(0L).withSkipInProcessCount(0L).withSkipInWriteCount(1L).build();
		validator.validate();
		// if one is correct, all will be in the MetricRepository, so I check just one
		assertThat((Long)metricRepository.findOne("counter.batch.flatFileToDbSkipJob."+jobExecution.getStepExecutions().iterator().next().getId()+".step."+MetricNames.PROCESS_COUNT.getName()).getValue(),is(7l));
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ITEM", Long.class),is(writeCount));
	}

	@Test
	public void testRunFlatFileToDbSkipJob_SkipInRead() throws InterruptedException{
		JobExecution jobExecution = runJob("flatFileToDbSkipJob","metrics/flatFileToDbSkipJob_SkipInRead.csv");
		assertThat(jobExecution.getStatus(),is(BatchStatus.COMPLETED));
		ExecutionContext executionContext = jobExecution.getStepExecutions().iterator().next().getExecutionContext();
		long writeCount = 7L;
		MetricValidator validator = MetricValidatorBuilder.metricValidator().withExecutionContext(executionContext)
				.withBeforeChunkCount(3L).withStreamOpenCount(1L).withStreamUpdateCount(4L).withStreamCloseCount(0L)
				.withBeforeReadCount(9L).withReadCount(9L).withAfterReadCount(7L).withReadErrorCount(1L).withBeforeProcessCount(7L)
				.withProcessCount(7L).withAfterProcessCount(7L).withProcessErrorCount(0L).withBeforeWriteCount(7L)
				.withWriteCount(writeCount).withAfterWriteCount(7L).withWriteErrorCount(0L).withAfterChunkCount(3L).withChunkErrorCount(0L)
				.withSkipInReadCount(1L).withSkipInProcessCount(0L).withSkipInWriteCount(0L).build();
		validator.validate();
		// if one is correct, all will be in the MetricRepository, so I check just one
		assertThat((Long)metricRepository.findOne("counter.batch.flatFileToDbSkipJob."+jobExecution.getStepExecutions().iterator().next().getId()+".step."+MetricNames.PROCESS_COUNT.getName()).getValue(),is(7l));
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ITEM", Long.class),is(writeCount));
	}

	@Test
	public void testRunFlatFileToDbSkipJob_SkipInProcess_ProcessorNonTransactional() throws InterruptedException{
		JobExecution jobExecution = runJob("flatFileToDbSkipProcessorNonTransactionalJob","metrics/flatFileToDbSkipJob_SkipInProcess.csv");
		assertThat(jobExecution.getStatus(),is(BatchStatus.COMPLETED));
		ExecutionContext executionContext = jobExecution.getStepExecutions().iterator().next().getExecutionContext();
		long writeCount = 7L;
		MetricValidator validator = MetricValidatorBuilder.metricValidator().withExecutionContext(executionContext)
				.withBeforeChunkCount(3L).withStreamOpenCount(1L).withStreamUpdateCount(4L).withStreamCloseCount(0L)
				.withBeforeReadCount(9L).withReadCount(9L).withAfterReadCount(8L).withReadErrorCount(0L).withBeforeProcessCount(8L)
				.withProcessCount(8L).withAfterProcessCount(7L).withProcessErrorCount(1L).withBeforeWriteCount(7L)
				.withWriteCount(writeCount).withAfterWriteCount(7L).withAfterChunkCount(3L).withChunkErrorCount(1L)
				.withSkipInReadCount(0L).withSkipInProcessCount(1L).withSkipInWriteCount(0L).build();
		validator.validate();
		// if one is correct, all will be in the MetricRepository, so I check just one
		assertThat((Long)metricRepository.findOne("counter.batch.flatFileToDbSkipProcessorNonTransactionalJob."+jobExecution.getStepExecutions().iterator().next().getId()+".step."+MetricNames.PROCESS_COUNT.getName()).getValue(),is(8l));
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ITEM", Long.class),is(writeCount));
	}

	@Test
	public void testRunFlatFileToDbSkipJob_SkipInWrite_ProcessorNonTransactional() throws InterruptedException{
		JobExecution jobExecution = runJob("flatFileToDbSkipProcessorNonTransactionalJob","metrics/flatFileToDbSkipJob_SkipInWrite.csv");
		assertThat(jobExecution.getStatus(),is(BatchStatus.COMPLETED));
		ExecutionContext executionContext = jobExecution.getStepExecutions().iterator().next().getExecutionContext();
		long writeCount = 7L;
		MetricValidator validator = MetricValidatorBuilder.metricValidator().withExecutionContext(executionContext)
				.withBeforeChunkCount(4L).withStreamOpenCount(1L).withStreamUpdateCount(4L).withStreamCloseCount(0L)
				.withBeforeReadCount(9L).withReadCount(9L).withAfterReadCount(8L).withReadErrorCount(0L).withBeforeProcessCount(8L)
				.withProcessCount(8L).withAfterProcessCount(8L).withProcessErrorCount(0L).withBeforeWriteCount(5L)
				.withWriteCount(writeCount).withAfterWriteCount(7L).withWriteErrorCount(4L).withAfterChunkCount(4L).withChunkErrorCount(2L)
				.withSkipInReadCount(0L).withSkipInProcessCount(0L).withSkipInWriteCount(1L).build();
		// TODO Bug in beforeWrite listener in Spring Batch?
		validator.validate();
		// if one is correct, all will be in the MetricRepository, so I check just one
		assertThat((Long)metricRepository.findOne("counter.batch.flatFileToDbSkipProcessorNonTransactionalJob."+jobExecution.getStepExecutions().iterator().next().getId()+".step."+MetricNames.PROCESS_COUNT.getName()).getValue(),is(8l));
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ITEM", Long.class),is(writeCount));
	}

	/*
	 * To be honest this isn't really a good example to test a transactional reader because in fact the reader is not transactional.
	 * So when the error occurs in the second chunk it is rolled back, and instead of reprocessing the cached read items,
	 * the job reads new items, because they would have been put back into the queue. In this case, we just continue reading the
	 * file, forgetting completely about the failed chunk. That's why there are written five items to the DB.
	 * We should write a test with a real transactional reader.
	 */
	@Test
	public void testRunFlatFileToDbSkipJob_SkipInProcess_ReaderTransactional() throws InterruptedException{
		JobExecution jobExecution = runJob("flatFileToDbSkipReaderTransactionalJob","metrics/flatFileToDbSkipJob_SkipInProcess.csv");
		assertThat(jobExecution.getStatus(),is(BatchStatus.COMPLETED));
		ExecutionContext executionContext = jobExecution.getStepExecutions().iterator().next().getExecutionContext();
		long writeCount = 5L;
		MetricValidator validator = MetricValidatorBuilder.metricValidator().withExecutionContext(executionContext)
				.withBeforeChunkCount(2L).withStreamOpenCount(1L).withStreamUpdateCount(3L).withStreamCloseCount(0L)
				.withBeforeReadCount(6L).withReadCount(6L).withAfterReadCount(5L).withReadErrorCount(0L).withBeforeProcessCount(5L)
				.withProcessCount(5L).withAfterProcessCount(5L).withProcessErrorCount(1L).withBeforeWriteCount(5L)
				.withWriteCount(writeCount).withAfterWriteCount(5L).withAfterChunkCount(2L).withChunkErrorCount(1L)
				.withSkipInReadCount(0L).withSkipInProcessCount(0L).withSkipInWriteCount(0L).build();
		validator.validate();
		// if one is correct, all will be in the MetricRepository, so I check just one
		assertThat((Long)metricRepository.findOne("counter.batch.flatFileToDbSkipReaderTransactionalJob."+jobExecution.getStepExecutions().iterator().next().getId()+".step."+MetricNames.PROCESS_COUNT.getName()).getValue(),is(5l));
		assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM ITEM", Long.class),is(writeCount));
	}

	private JobExecution runJob(String jobName, String pathToFile) throws InterruptedException {
		MultiValueMap<String, Object> requestMap = new LinkedMultiValueMap<>();
		requestMap.add("jobParameters", "pathToFile="+pathToFile);
		Long executionId = restTemplate.postForObject("http://localhost:8091/batch/operations/jobs/"+jobName, requestMap,Long.class);
		while (!restTemplate.getForObject("http://localhost:8091/batch/operations/jobs/executions/{executionId}", String.class, executionId).equals("COMPLETED") &&
				!restTemplate.getForObject("http://localhost:8091/batch/operations/jobs/executions/{executionId}", String.class, executionId).equals("FAILED")){
			Thread.sleep(1000);
		}
		JobExecution jobExecution = jobExplorer.getJobExecution(executionId);
		return jobExecution;
	}
	
}
