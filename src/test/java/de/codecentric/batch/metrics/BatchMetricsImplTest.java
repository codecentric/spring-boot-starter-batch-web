package de.codecentric.batch.metrics;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.MDC;
import org.springframework.boot.actuate.metrics.GaugeService;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

import de.codecentric.batch.listener.LoggingListener;

public class BatchMetricsImplTest {

	private ExtendedCounterService counterServiceMock;
	private GaugeService gaugeServiceMock;
	private BatchMetricsImpl batchMetrics;

	@Before
	public void beforeTest() {
		counterServiceMock = Mockito.mock(ExtendedCounterService.class);
		gaugeServiceMock = Mockito.mock(GaugeService.class);
		batchMetrics = new BatchMetricsImpl(counterServiceMock, gaugeServiceMock);
		MDC.put(LoggingListener.STEP_EXECUTION_IDENTIFIER, "jobname-1");
	}

	@Test
	public void incrementBy1Transactional() throws Exception {
		// Given
		TransactionSynchronizationManager.initSynchronization();
		// When
		batchMetrics.increment("counter.test", 1L);
		TransactionSynchronizationUtils.triggerAfterCompletion(TransactionSynchronization.STATUS_COMMITTED);
		// Then
		Mockito.verify(counterServiceMock).increment("batch.jobname-1.counter.test", 1L);
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
		Mockito.verify(counterServiceMock).decrement("batch.jobname-1.counter.test", 1L);
		TransactionSynchronizationManager.clearSynchronization();
	}

	@Test
	public void incrementBy1NonTransactional() throws Exception {
		// Given
		// No transaction
		// When
		batchMetrics.increment("counter.test", 1L);
		// Then
		Mockito.verify(counterServiceMock).increment("batch.jobname-1.counter.test", 1L);
	}

	@Test
	public void submitTransactional() throws Exception {
		// Given
		TransactionSynchronizationManager.initSynchronization();
		// When
		batchMetrics.submit("counter.test", 1L);
		TransactionSynchronizationUtils.triggerAfterCompletion(TransactionSynchronization.STATUS_COMMITTED);
		// Then
		Mockito.verify(gaugeServiceMock).submit("batch.jobname-1.counter.test", 1L);
		TransactionSynchronizationManager.clearSynchronization();
	}

	@Test
	public void submitNonTransactional() throws Exception {
		// Given
		// No transaction
		// When
		batchMetrics.submit("counter.test", 1L);
		// Then
		Mockito.verify(gaugeServiceMock).submit("batch.jobname-1.counter.test", 1L);
	}
}
