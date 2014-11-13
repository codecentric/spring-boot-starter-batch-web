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
package de.codecentric.batch.metrics;


/**
 * Interface for business metrics.
 * 
 * All metric names get the prefix 'batch.{jobName}.{jobExecutionId}.' in addition to other prefixes. 
 * For example, when using the {@link org.springframework.boot.actuate.metrics.writer.DefaultCounterService}
 * the complete prefix will be 'counter.batch.{jobName}.{jobExecutionId}.'. When written to the 
 * Step-ExecutionContext, the complete prefix is omitted.
 * 
 * There are two types of methods: non-transactional methods and transactional methods. The methods that
 * don't include 'NonTransactional' in their name are by default transaction-aware. That means that
 * their execution is delayed after a successful commit of the current transaction. If there's no 
 * current transaction, the method is executed immediately. The non-transactional methods are
 * executed directly, no matter if there's a transaction or not.
 * 
 * How do you use this component in a batch environment?
 * 
 * You may inject this component into any batch artifact, a reader, processor, writer, a listener and so on.
 * Counter and gauges are held in memory as long as the job runs and will be written to the Step-ExecutionContext
 * once the step finishes, successful or not. They are written to the log file as well.
 * If the job execution is a restart of a previously failed run, the counters are the sum of both executions.
 * Gauges are the ones from the last run.
 * 
 * The tricky part is to decide whether the transactional or the non-transactional method should be used for
 * correct counting. This blog post series might help for better understanding how transactions in Spring Batch
 * work: https://blog.codecentric.de/en/2012/03/transactions-in-spring-batch-part-1-the-basics/.
 * 
 * The thing is that Spring Batch's transactional behaviour is smart, but gets in your way if you do the metrics
 * in a naive way. When using skip functionality, for example, it may happen that a process - method of a 
 * ItemProcessor gets executed several times for one successful processed item, because the item was together
 * in a chunk with a bad item, and the chunk was rolled back. If your counting doesn't get rolled back as well,
 * the metric is wrong. That's why it's so essential to have transactional metrics in Spring Batch.
 * 
 * In general you should always use the transactional methods, because when there's no transaction they behave
 * like the non-transactional methods, and when there's a transaction they are only executed when the transaction
 * is successful. However, there are exceptions. With a job configured the default way you have a cache for the
 * read items. When there's a rollback of a chunk, the ItemReader is not re-executed. So for correct countings
 * you have to use the non-transactional methods in the ItemReader and the ItemReadListener. If you set
 * reader-transactional-queue to true for your job, the cache is not used and you have to use the transactional
 * methods in ItemReader and ItemReadListener as well. If you set processor-transactional to false for your job
 * there will be a cache for the processed items as well, so you have to use the non-transactional methods in the
 * ItemProcessor and the ItemProcessListener. And, of course, if you are counting errors in the onError-methods
 * of ItemListeners, you have to use the non-transactional methods because a rollback is going to happen afterwards.
 * If you do a filter in an ItemProcessor you have to use the non-transactional methods, because filtered items
 * are removed from the item cache as well and will never be reprocessed. When using the AsyncItemProcessor there 
 * is no filtering possible at all, because a Future-Object is always returned from the ItemProcessor. 
 * 
 * @author Tobias Flohre
 */
public interface BatchMetrics {

	/**
	 * Increment the specified counter by 1. Transaction-aware.
	 * @param metricName the name of the counter
	 */
	void increment(String metricName);

	/**
	 * Increment the specified counter by the given value. Transaction-aware.
	 * @param metricName the name of the counter
	 * @param value the amount to increment by
	 */
	void increment(String metricName, Long value);

	/**
	 * Decrement the specified counter by 1. Transaction-aware.
	 * @param metricName the name of the counter
	 */
	void decrement(String metricName);

	/**
	 * Reset the specified counter. Transaction-aware.
	 * @param metricName the name of the counter
	 */
	void reset(String metricName);

	/**
	 * Set the specified gauge value. Transaction-aware.
	 * @param metricName the name of the gauge to set
	 * @param value the value of the gauge
	 */
	void submit(String metricName, double value);

	/**
	 * Increment the specified counter by 1.
	 * @param metricName the name of the counter
	 */
	void incrementNonTransactional(String metricName);

	/**
	 * Increment the specified counter by the given value.
	 * @param metricName the name of the counter
	 * @param value the amount to increment by
	 */
	void incrementNonTransactional(String metricName, Long value);

	/**
	 * Decrement the specified counter by 1.
	 * @param metricName the name of the counter
	 */
	void decrementNonTransactional(String metricName);

	/**
	 * Decrement the specified counter by the given value.
	 * @param metricName the name of the counter
	 * @param value the amount to decrement by
	 */
	void decrementNonTransactional(String metricName, Long value);
	
	/**
	 * Reset the specified counter.
	 * @param metricName the name of the counter
	 */
	void resetNonTransactional(String metricName);

	/**
	 * Set the specified gauge value
	 * @param metricName the name of the gauge to set
	 * @param value the value of the gauge
	 */
	void submitNonTransactional(String metricName, double value);
}
