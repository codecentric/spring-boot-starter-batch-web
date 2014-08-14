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
import org.springframework.boot.test.IntegrationTest;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.TestRestTemplate;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.client.RestTemplate;

import de.codecentric.batch.TestApplication;

/**
 * This test class starts a batch job configured in XML and tests several endpoints.
 * 
 * @author Tobias Flohre
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes=TestApplication.class)
@WebAppConfiguration
@IntegrationTest
public class XmlIntegrationTest {

	RestTemplate restTemplate = new TestRestTemplate();
	
	@Autowired
	private JobExplorer jobExplorer;
	
	@Test
	public void testRunJob() throws InterruptedException{
		Long executionId = restTemplate.postForObject("http://localhost:8080/batch/operations/jobs/flatFile2JobXml", "",Long.class);
		while (!restTemplate.getForObject("http://localhost:8080/batch/operations/jobs/executions/{executionId}", String.class, executionId).equals("COMPLETED")){
			Thread.sleep(1000);
		}
		String log = restTemplate.getForObject("http://localhost:8080/batch/operations/jobs/executions/{executionId}/log", String.class, executionId);
		assertThat(log.length()>20,is(true));
		JobExecution jobExecution = jobExplorer.getJobExecution(executionId);
		assertThat(jobExecution.getStatus(),is(BatchStatus.COMPLETED));
		String jobExecutionString = restTemplate.getForObject("http://localhost:8080/batch/monitoring/jobs/executions/{executionId}",String.class,executionId);
		assertThat(jobExecutionString.contains("COMPLETED"),is(true));
	}

	@Test
	public void testGetJobNames(){
		@SuppressWarnings("unchecked")
		List<String> jobNames = restTemplate.getForObject("http://localhost:8080/batch/monitoring/jobs", List.class);
		assertThat(jobNames.contains("flatFile2JobXml"), is(true));
	}
	
}
