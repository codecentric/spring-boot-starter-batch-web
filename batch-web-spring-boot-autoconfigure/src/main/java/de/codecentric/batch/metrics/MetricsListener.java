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

import java.util.Arrays;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.listener.StepExecutionListenerSupport;
import org.springframework.core.Ordered;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.ImmutableTag;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.search.Search;

/**
 * This listener exports all metrics with the prefix 'counter.batch.{jobName}.{jobExecutionId}.{stepName} and all gauges
 * with the prefix 'gauge.batch.{jobName}.{stepName}' to the Step- ExecutionContext without the prefix. All metrics and
 * gauges are logged as well. For overriding the default format of the logging a component implementing
 * {@link MetricsOutputFormatter} may be added to the ApplicationContext.
 *
 * Counters are cumulated over several StepExecutions belonging to one Step in one JobInstance, important for restarted
 * jobs.
 *
 * @author Tobias Flohre
 * @author Dennis Schulte
 */
public class MetricsListener extends StepExecutionListenerSupport implements Ordered, JobExecutionListener {

	private static final Log LOGGER = LogFactory.getLog(MetricsListener.class);

	public static final String METRIC_NAME = "batch.metrics";

	private MeterRegistry meterRegistry;

	private MetricsOutputFormatter metricsOutputFormatter = new SimpleMetricsOutputFormatter();

	public MetricsListener(MeterRegistry meterRegistry) {
		this.meterRegistry = meterRegistry;
	}

	@Override
	public void beforeJob(JobExecution jobExecution) {
		// no action
	}

	@Override
	public ExitStatus afterStep(StepExecution stepExecution) {
		// Calculate step execution time
		// Why is stepExecution.getEndTime().getTime() not available here? (see AbstractStep)
		long stepDuration = System.currentTimeMillis() - stepExecution.getStartTime().getTime();
		meterRegistry.gauge(METRIC_NAME, Arrays.asList(//
				new ImmutableTag("context", getStepExecutionIdentifier(stepExecution)), //
				new ImmutableTag("name", "duration")//
		), stepDuration);
		long itemCount = stepExecution.getWriteCount() + stepExecution.getSkipCount();
		meterRegistry.gauge(METRIC_NAME, Arrays.asList(//
				new ImmutableTag("context", getStepExecutionIdentifier(stepExecution)), //
				new ImmutableTag("name", "item.count")//
		), itemCount);
		// Calculate execution time per item
		long durationPerItem = 0;
		if (itemCount > 0) {
			durationPerItem = stepDuration / itemCount;
		}
		meterRegistry.gauge(METRIC_NAME, Arrays.asList(//
				new ImmutableTag("context", getStepExecutionIdentifier(stepExecution)), //
				new ImmutableTag("name", "item.duration")//
		), durationPerItem);
		// Export metrics from StepExecution to MetricRepositories
		Set<Entry<String, Object>> metrics = stepExecution.getExecutionContext().entrySet();
		for (Entry<String, Object> metric : metrics) {
			if (metric.getValue() instanceof Number) {
				meterRegistry.gauge(METRIC_NAME, Arrays.asList(//
						new ImmutableTag("context", getStepExecutionIdentifier(stepExecution)), //
						new ImmutableTag("name", metric.getKey())//
				), (Number) metric.getValue());
			}
		}
		return null;
	}

	@Override
	public void afterJob(JobExecution jobExecution) {
		long jobDuration = jobExecution.getEndTime().getTime() - jobExecution.getStartTime().getTime();
		meterRegistry.gauge(METRIC_NAME, Arrays.asList(//
				new ImmutableTag("context", jobExecution.getJobInstance().getJobName()), //
				new ImmutableTag("name", "duration")//
		), jobDuration);
		// What the f*** is that Thread.sleep doing here? ;-)
		// Metrics are written asynchronously to Spring Boot's repository. In our tests we experienced
		// that sometimes batch execution was so fast that this listener couldn't export the metrics
		// because they hadn't been written. It all happened in the same millisecond. So we added
		// a Thread.sleep of 100 milliseconds which gives us enough safety and doesn't hurt anyone.
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		// Export Metrics to Console
		Search search = meterRegistry.find(METRIC_NAME);
		LOGGER.info(metricsOutputFormatter.format(search.gauges(), search.timers()));
	}

	// tag::contains[]
	private static class SimpleMetricsOutputFormatter implements MetricsOutputFormatter {

		@Override
		public String format(Collection<Gauge> gauges, Collection<Timer> timers) {
			StringBuilder builder = new StringBuilder("\n########## Metrics Start ##########\n");
			gauges.stream().forEach(gauge -> {
				builder.append("Gauge [" + gauge.getId() + "]: ");
				builder.append(gauge.value() + "\n");
			});
			timers.stream().forEach(timer -> {
				builder.append("Timer [" + timer.getId() + "]: ");
				builder.append(
						"totalTime=" + timer.totalTime(timer.baseTimeUnit()) + " " + timer.baseTimeUnit() + "\n");
			});
			builder.append("########## Metrics End ############");
			return builder.toString();
		}

	}
	// end::contains[]

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE - 1;
	}

	private String getStepExecutionIdentifier(StepExecution stepExecution) {
		return stepExecution.getJobExecution().getJobInstance().getJobName() + "." + stepExecution.getStepName();
	}

	public void setMetricsOutputFormatter(MetricsOutputFormatter metricsOutputFormatter) {
		this.metricsOutputFormatter = metricsOutputFormatter;
	}
}
