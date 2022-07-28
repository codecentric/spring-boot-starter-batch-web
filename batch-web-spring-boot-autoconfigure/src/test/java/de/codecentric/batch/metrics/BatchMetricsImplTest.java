package de.codecentric.batch.metrics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

public class BatchMetricsImplTest {

	private BatchMetricsImpl batchMetrics;

	@BeforeEach
	public void beforeTest() {
		batchMetrics = new BatchMetricsImpl();
		StepSynchronizationManager
				.register(new StepExecution("step", new JobExecution(new JobInstance(1L, "jobname"), 1L, null, null)));
	}

	@Test
	public void incrementBy1Transactional() throws Exception {
		// Given
		TransactionSynchronizationManager.initSynchronization();
		// When
		batchMetrics.increment("counter.test", 1L);
		TransactionSynchronizationUtils.triggerAfterCompletion(TransactionSynchronization.STATUS_COMMITTED);
		// Then
		TransactionSynchronizationManager.clearSynchronization();
	}

	@Test
	public void decrementBy1Transactional() throws Exception {
		// Given
		TransactionSynchronizationManager.initSynchronization();
		// When
		batchMetrics.decrement("counter.test", 1L);
		TransactionSynchronizationUtils.triggerAfterCompletion(TransactionSynchronization.STATUS_COMMITTED);
		// Then
		TransactionSynchronizationManager.clearSynchronization();
	}

	@Test
	public void incrementBy1NonTransactional() throws Exception {
		// Given
		// No transaction
		// When
		batchMetrics.increment("counter.test", 1L);
		// Then
	}

	@Test
	public void submitTransactional() throws Exception {
		// Given
		TransactionSynchronizationManager.initSynchronization();
		// When
		batchMetrics.submit("counter.test", 1L);
		TransactionSynchronizationUtils.triggerAfterCompletion(TransactionSynchronization.STATUS_COMMITTED);
		// Then
		TransactionSynchronizationManager.clearSynchronization();
	}

	@Test
	public void submitNonTransactional() throws Exception {
		// Given
		// No transaction
		// When
		batchMetrics.submit("counter.test", 1L);
		// Then
	}
}
