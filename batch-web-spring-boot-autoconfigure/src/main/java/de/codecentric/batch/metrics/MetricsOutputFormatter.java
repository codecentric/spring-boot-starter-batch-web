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

import java.util.List;

import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.rich.RichGauge;

/**
 * Used in {@link MetricsListener} to define the format of the metrics log. A component
 * implementing this interface may be added to the ApplicationContext to override the
 * default behaviour.
 * 
 * @author Tobias Flohre
 */
public interface MetricsOutputFormatter {
	
	public String format(List<RichGauge> gauges, List<Metric<?>> metrics);

}
