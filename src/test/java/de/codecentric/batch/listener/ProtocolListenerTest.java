package de.codecentric.batch.listener;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

import java.util.Date;

import org.junit.Rule;
import org.junit.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.boot.test.OutputCapture;

public class ProtocolListenerTest {

	@Rule
	public OutputCapture outputCapture = new OutputCapture();

	@Test
	public void createProtocol() throws Exception {
		// Given
		JobExecution jobExecution = new JobExecution(1L, new JobParametersBuilder().addString("test", "value").toJobParameters());
		jobExecution.setJobInstance(new JobInstance(1L, "test-job"));
		jobExecution.setCreateTime(new Date());
		jobExecution.setStartTime(new Date());
		jobExecution.setEndTime(new Date());
		jobExecution.setExitStatus(new ExitStatus("COMPLETED_WITH_ERRORS", "This is a default exit message"));
		jobExecution.getExecutionContext().put("jobCounter", 1);
		StepExecution stepExecution = jobExecution.createStepExecution("test-step-1");
		stepExecution.getExecutionContext().put("stepCounter", 1);
		ProtocolListener protocolListener = new ProtocolListener();
		// When
		protocolListener.afterJob(jobExecution);
		// Then
		String output = this.outputCapture.toString();
		assertThat(output, containsString("Protocol for test-job"));
		assertThat(output, containsString("COMPLETED_WITH_ERRORS"));
	}
}
