/*
 * Copyright 2018 the original author or authors.
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
package de.codecentric.batch.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import de.codecentric.batch.scheduling.concurrent.MdcThreadPoolTaskExecutor;

/**
 * This is the default configuration for a {@link org.springframework.core.task.TaskExecutor} used in the {@link org.springframework.batch.core.launch.support.SimpleJobLauncher}
 * for starting jobs asynchronously. Its core thread pool is configured to five threads by default, which can be changed
 * by setting the property batch.core.pool.size to a different number. 
 * 
 * Please note the following rules of the ThreadPoolExecutor:
 * If the number of threads is less than the corePoolSize, the executor creates a new thread to run a new task.
 * If the number of threads is equal (or greater than) the corePoolSize, the executor puts the task into the queue.
 * If the queue is full and the number of threads is less than the maxPoolSize, the executor creates a new thread to run a new task.
 * If the queue is full and the number of threads is greater than or equal to maxPoolSize, reject the task.
 * 
 * So with the default configuration there will be only 5 jobs/threads at the same time.
 * 
 * The {@link org.springframework.core.task.TaskExecutor} may also be used in job configurations
 * for multi-threaded job execution. In XML you can use it by name, which is taskExecutor. In JavaConfig,
 * you can either autowire {@link org.springframework.core.task.TaskExecutor} or, if you want to know
 * where it's configured, this class.
 * 
 * @author Dennis Schulte
 * 
 */
@Configuration
@ConditionalOnMissingBean(TaskExecutor.class)
public class TaskExecutorConfiguration {
	
	@Autowired
	private Environment env;
	
	@Bean
	public TaskExecutor taskExecutor() {
		ThreadPoolTaskExecutor taskExecutor = new MdcThreadPoolTaskExecutor();
		taskExecutor.setCorePoolSize(env.getProperty("batch.core.pool.size", Integer.class, 5));
		taskExecutor.setQueueCapacity(env.getProperty("batch.queue.capacity", Integer.class, Integer.MAX_VALUE));
		taskExecutor.setMaxPoolSize(env.getProperty("batch.max.pool.size", Integer.class, Integer.MAX_VALUE));
		taskExecutor.afterPropertiesSet();
		return taskExecutor;
	}

}
