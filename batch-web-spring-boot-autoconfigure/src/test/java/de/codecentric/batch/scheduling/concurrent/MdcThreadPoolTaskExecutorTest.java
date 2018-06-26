package de.codecentric.batch.scheduling.concurrent;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;

import org.junit.Test;
import org.slf4j.MDC;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

public class MdcThreadPoolTaskExecutorTest {

	@Test
	public void run() throws Exception {
		// Given
		ThreadPoolTaskExecutor taskExecutor = new ThreadPoolTaskExecutor();
		taskExecutor.afterPropertiesSet();

		final ThreadPoolTaskExecutor innerTaskExecutor = new MdcThreadPoolTaskExecutor();
		innerTaskExecutor.afterPropertiesSet();

		// Simple task which returns always the key from the MDC
		final FutureTask<String> innerTask = new FutureTask<String>(new Callable<String>() {

			@Override
			public String call() throws Exception {
				return MDC.get("key");
			}
		});

		taskExecutor.execute(new Runnable() {

			@Override
			public void run() {
				MDC.put("key", "1");
				innerTaskExecutor.execute(innerTask);
			}
		});
		// Wait for Thread and verify that it contains the right value of key
		assertThat(innerTask.get(), is(equalTo("1")));

		// Wait 1sec. for outer thread
		Thread.sleep(1000);

		// Simple task which returns always the key from the MDC
		final FutureTask<String> innerTask2 = new FutureTask<String>(new Callable<String>() {

			@Override
			public String call() throws Exception {
				return MDC.get("key");
			}
		});

		taskExecutor.execute(new Runnable() {

			@Override
			public void run() {
				MDC.put("key", "2");
				innerTaskExecutor.execute(innerTask2);
			}
		});

		// Wait for Thread and verify that it contains the right value of key
		assertThat(innerTask2.get(), is(equalTo("2")));

	}
}
