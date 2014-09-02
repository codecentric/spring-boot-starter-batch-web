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

import org.springframework.boot.actuate.autoconfigure.MetricRepositoryAutoConfiguration;
import org.springframework.boot.autoconfigure.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

import de.codecentric.batch.configuration.AutomaticJobRegistrarConfiguration;
import de.codecentric.batch.configuration.BatchWebAutoConfiguration;

@Configuration
@Import({ BatchWebAutoConfiguration.class, DataSourceAutoConfiguration.class, BatchAutoConfiguration.class,
		PropertyPlaceholderAutoConfiguration.class, AutomaticJobRegistrarConfiguration.class, MetricRepositoryAutoConfiguration.class,AopAutoConfiguration.class })
@PropertySource("application.properties")
public class TestConfiguration {

}
