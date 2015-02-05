package de.codecentric.batch.metrics;

import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.scope.context.StepContext;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.writer.Delta;
import org.springframework.boot.actuate.metrics.writer.MetricWriter;

/**
 * This {@link MetricWriter} saves all relevant metrics in the StepExecutionContext of the current step. Relevant means, that only the metrics
 * will be stored, which are associated with a job step. Application metrics will not be stored within the StepExecutionContext.
 * 
 * @author Dennis Schulte
 */
public class StepExecutionMetricWriter implements MetricWriter {

	public static final String GAUGE_PREFIX = "gauge.batch.";

	public static final String COUNTER_PREFIX = "counter.batch.";

	@Override
	public void increment(Delta<?> delta) {
		StepExecution stepExecution = getStepExecution();
		if (isRelevant(delta.getName())) {
			Long oldValue = 0L;
			String metricName = removeIdentifier(delta.getName());
			if (stepExecution.getExecutionContext().containsKey(metricName)) {
				oldValue = stepExecution.getExecutionContext().getLong(metricName);
			}
			Long newValue = (Long) delta.getValue() + oldValue;
			stepExecution.getExecutionContext().put(metricName, newValue);
		}
	}

	@Override
	public void set(Metric<?> value) {
		StepExecution stepExecution = getStepExecution();
		if (isRelevant(value.getName())) {
			String metricName = removeIdentifier(value.getName());
			stepExecution.getExecutionContext().put(metricName, value.getValue());
		}
	}

	@Override
	public void reset(String metricName) {
		StepExecution stepExecution = getStepExecution();
		if (isRelevant(metricName)) {
			stepExecution.getExecutionContext().remove(removeIdentifier(metricName));
		}
	}

	private StepExecution getStepExecution() {
		if (StepSynchronizationManager.getContext() != null) {
			return StepSynchronizationManager.getContext().getStepExecution();
		}
		return null;
	}

	private String getStepExecutionIdentifier() {
		StepContext stepContext = StepSynchronizationManager.getContext();
		if (stepContext != null) {
			StepExecution stepExecution = StepSynchronizationManager.getContext().getStepExecution();
			return stepContext.getJobName() + "." + stepExecution.getStepName();
		}
		return null;
	}

	private boolean isRelevant(String metricName) {
		String stepExecutionIdentifier = getStepExecutionIdentifier();
		// Only metrics with a step identifier will be exported to the StepExecutionContext
		if (stepExecutionIdentifier != null) {
			if (metricName.startsWith(COUNTER_PREFIX + stepExecutionIdentifier)) {
				if (metricName.length() == (COUNTER_PREFIX + stepExecutionIdentifier).length()) {
					return false;
				}
				return true;
			}
			if (metricName.startsWith(GAUGE_PREFIX + stepExecutionIdentifier)) {
				if (metricName.length() == (GAUGE_PREFIX + stepExecutionIdentifier).length()) {
					return false;
				}
				return true;
			}
		}
		return false;
	}

	private String removeIdentifier(String metricName) {
		String stepExecutionIdentifier = getStepExecutionIdentifier();
		// Prefix (e.g. "counter.batch."+ stepExecutionIdentifier) is removed from the key before insertion in Step-ExecutionContext
		if (metricName.startsWith(COUNTER_PREFIX + stepExecutionIdentifier)) {
			return metricName.substring((COUNTER_PREFIX + stepExecutionIdentifier).length() + 1);
		}
		if (metricName.startsWith(GAUGE_PREFIX + stepExecutionIdentifier)) {
			return metricName.substring((GAUGE_PREFIX + stepExecutionIdentifier).length() + 1);
		}
		return metricName;
	}

}
