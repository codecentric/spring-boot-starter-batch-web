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
package de.codecentric.batch;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.metrics.Metric;
import org.springframework.boot.actuate.metrics.rich.RichGauge;
import org.springframework.boot.actuate.metrics.writer.CompositeMetricWriter;
import org.springframework.boot.actuate.metrics.writer.MetricWriter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import de.codecentric.batch.configuration.BaseConfiguration;
import de.codecentric.batch.metrics.ListenerMetricsAspect;
import de.codecentric.batch.metrics.MetricsOutputFormatter;

/**
 * Application for integration testing.
 * 
 * @author Tobias Flohre
 */
@Configuration
@EnableAutoConfiguration
@Import(TestListenerConfiguration.class)
public class MetricsTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(MetricsTestApplication.class, args);
    }
  
    @Autowired
    private BaseConfiguration baseConfig;
    
    @Bean
    public ListenerMetricsAspect listenerMetricsAspect(){
    	return new ListenerMetricsAspect(baseConfig.gaugeService());
    }
    
    @Bean
    public MetricsOutputFormatter metricsOutputFormatter(){
    	return new MetricsOutputFormatter(){

			@Override
			public String format(List<RichGauge> gauges, List<Metric<?>> metrics) {
				StringBuilder builder = new StringBuilder("\n########## Personal Header for metrics! #####\n########## Metrics Start ##########\n");
				for (RichGauge gauge: gauges){
					builder.append(gauge.toString()+"\n");
				}
				for (Metric<?> metric: metrics){
					builder.append(metric.toString()+"\n");
				}
				builder.append("########## Metrics End ############");
				return builder.toString();
			}
    		
    	};
    }

	@Bean(name = "primaryMetricWriter")
	@Primary
	static public MetricWriter primaryMetricWriter(List<MetricWriter> writers) {
		//Normally the Metrics are written asynchronously to Spring Boot's repository. In tests we need to do it synchronously to be able to verify the correct output.
		MetricWriter compositeMetricWriter = new CompositeMetricWriter(writers);
		return compositeMetricWriter;
	}

}
