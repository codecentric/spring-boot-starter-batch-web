package de.codecentric.batch.metrics;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.actuate.metrics.GaugeService;
import org.springframework.util.ClassUtils;
import org.springframework.util.StopWatch;

import de.codecentric.batch.listener.LoggingListener;

/**
 * Central aspect-configuration to profile Spring Batch Jobs with AspectJ and Spring Boot Metrics.
 * 
 * @author Dennis Schulte
 */
@Aspect
public class BatchMetricsAspects {

	private static final Logger LOG = LoggerFactory.getLogger(BatchMetricsAspects.class);

	private GaugeService gaugeService;

	public BatchMetricsAspects(GaugeService gaugeService) {
		this.gaugeService = gaugeService;
	}

	@Around("execution(* org.springframework.batch.item.ItemReader.read(..))")
	public Object profileReadMethods(ProceedingJoinPoint pjp) throws Throwable {
		StopWatch stopWatch = startStopWatch();
		try {
			return pjp.proceed();
		} finally {
			gaugeService.submit(MetricsListener.GAUGE_PREFIX + getStepIdentifier() + "." + ClassUtils.getShortName(pjp.getTarget().getClass())+ ".read",
					getTotalTimeMillis(stopWatch));
		}
	}

	@Around("execution(* org.springframework.batch.item.ItemProcessor.process(..))")
	public Object profileProcessMethods(ProceedingJoinPoint pjp) throws Throwable {
		StopWatch stopWatch = startStopWatch();
		try {
			return pjp.proceed();
		} finally {
			gaugeService.submit(MetricsListener.GAUGE_PREFIX + getStepIdentifier() + "." + ClassUtils.getShortName(pjp.getTarget().getClass())+ ".process",
					getTotalTimeMillis(stopWatch));
		}
	}

	@Around("execution(* org.springframework.batch.item.ItemWriter.write(..))")
	public Object profileWriteMethods(ProceedingJoinPoint pjp) throws Throwable {
		StopWatch stopWatch = startStopWatch();
		try {
			return pjp.proceed();
		} finally {
			gaugeService.submit(MetricsListener.GAUGE_PREFIX + getStepIdentifier() + "." + ClassUtils.getShortName(pjp.getTarget().getClass())+ ".write",
					getTotalTimeMillis(stopWatch));
		}
	}

	private long getTotalTimeMillis(StopWatch stopWatch) {
		stopWatch.stop();
		long duration = stopWatch.getTotalTimeMillis();
		return duration;
	}

	private StopWatch startStopWatch() {
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		return stopWatch;
	}

	private String getStepIdentifier() {
		String stepIdentifier = MDC.get(LoggingListener.STEP_EXECUTION_IDENTIFIER);
		if (stepIdentifier == null) {
			LOG.warn("Step identifier could not be read from MDC.");
			stepIdentifier = "unknown";
		}
		return stepIdentifier;
	}
}