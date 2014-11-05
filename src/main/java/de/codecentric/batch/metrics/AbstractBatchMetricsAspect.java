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

import org.aspectj.lang.ProceedingJoinPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.actuate.metrics.GaugeService;
import org.springframework.util.ClassUtils;
import org.springframework.util.StopWatch;

import de.codecentric.batch.listener.LoggingListener;

/**
 * This is a helper class for implementing method level profiling. See {@link ListenerMetricsAspect} for an 
 * aspect extending this class. All calls to an adviced method are tracked in a RichGauge, so you'll see average duration time,
 * maximum / minimum time, number of method calls and so on. For the name of the metric a special naming scheme is used so that
 * our {@link MetricsListener} picks up the gauge and writes it to the ExecutionContext of the StepExecution and to the log.
 * 
 * Job configurations need to enable auto-proxying so that aspects may be applied. In JavaConfig just add 
 * @EnableAspectJAutoProxy(proxyTargetClass=true) as a class level annotation. In xml add
 * <aop:aspectj-autoproxy proxy-target-class="true"/> to the xml configuration file. This needs to be done because jobs reside
 * in child application contexts and don't inherit this kind of configuration from the parent. proxyTargetClass=true means using
 * CGLIB as proxy mechanism which allows us to proxy classes without interfaces.
 * 
 * @author Tobias Flohre
 */
public abstract class AbstractBatchMetricsAspect {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractBatchMetricsAspect.class);

	private GaugeService gaugeService;

	public AbstractBatchMetricsAspect(GaugeService gaugeService) {
		this.gaugeService = gaugeService;
	}

	protected Object profileMethod(ProceedingJoinPoint pjp) throws Throwable {
		StopWatch stopWatch = startStopWatch();
		try {
			return pjp.proceed();
		} finally {
			gaugeService.submit(MetricsListener.GAUGE_PREFIX + getStepIdentifier() + "." + ClassUtils.getShortName(pjp.getTarget().getClass())+ "."+pjp.getSignature().getName(),
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