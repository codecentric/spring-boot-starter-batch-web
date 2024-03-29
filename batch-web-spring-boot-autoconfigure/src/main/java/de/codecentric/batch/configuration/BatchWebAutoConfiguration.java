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

import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.converter.DefaultJobParametersConverter;
import org.springframework.batch.core.converter.JobParametersConverter;
import org.springframework.batch.core.job.AbstractJob;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.Ordered;

import de.codecentric.batch.listener.AddListenerToJobService;
import de.codecentric.batch.listener.LoggingAfterJobListener;
import de.codecentric.batch.listener.LoggingListener;
import de.codecentric.batch.listener.ProtocolListener;
import de.codecentric.batch.listener.RunningExecutionTrackerListener;
import de.codecentric.batch.monitoring.RunningExecutionTracker;

/**
 * This configuration class will be picked up by Spring Boot's auto configuration capabilities as soon as it's on the
 * classpath.
 * <p>
 * It enables batch processing, imports the batch infrastructure configuration ({@link TaskExecutorBatchConfiguration}
 * and imports the web endpoint configuration ({@link WebConfig}.<br>
 * It also imports {@link AutomaticJobRegistrarConfiguration} which looks for jobs in a modular fashion, meaning that
 * every job configuration file gets its own Child-ApplicationContext. Configuration files can be XML files in the
 * location /META-INF/spring/batch/jobs, overridable via property batch.config.path.xml, and JavaConfig classes in the
 * package spring.batch.jobs, overridable via property batch.config.package.javaconfig.<br>
 * In addition to collecting jobs a number of default listeners is added to each job. The
 * {@link de.codecentric.batch.listener.ProtocolListener} adds a protocol to the log. It is activated by default and can
 * be deactivated by setting the property batch.defaultprotocol.enabled to false.<br>
 * {@link de.codecentric.batch.listener.LoggingListener} and
 * {@link de.codecentric.batch.listener.LoggingAfterJobListener} add a log file separation per job run, are activated by
 * default and can be deactivated by setting the property batch.logfileseparation.enabled to false. The
 * {@link de.codecentric.batch.listener.RunningExecutionTrackerListener} is needed for knowing which JobExecutions are
 * currently running on this node.
 *
 * @author Tobias Flohre
 */
@Configuration
@EnableBatchProcessing(modular = true)
@PropertySource("classpath:batch-web-spring-boot-autoconfigure.properties")
@AutoConfigureAfter({ MetricsAutoConfiguration.class })
@Import({ WebConfig.class, TaskExecutorBatchConfiguration.class, AutomaticJobRegistrarConfiguration.class,
        Jsr352BatchConfiguration.class, MetricsConfiguration.class, TaskExecutorConfiguration.class })
@EnableConfigurationProperties({ BatchConfigurationProperties.class })
public class BatchWebAutoConfiguration implements ApplicationListener<ContextRefreshedEvent>, Ordered {

    @Autowired
    private BatchConfigurationProperties batchConfig;

    @Autowired
    private JobRegistry jobRegistry;

    // ################### Listeners automatically added to each job #################################

    @Bean
    public LoggingListener loggingListener() {
        return new LoggingListener();
    }

    @Bean
    public LoggingAfterJobListener loggingAfterJobListener() {
        return new LoggingAfterJobListener();
    }

    @Bean
    public ProtocolListener protocolListener() {
        return new ProtocolListener();
    }

    @Bean
    public RunningExecutionTracker runningExecutionTracker() {
        return new RunningExecutionTracker();
    }

    @Bean
    public RunningExecutionTrackerListener runningExecutionTrackerListener() {
        return new RunningExecutionTrackerListener(runningExecutionTracker());
    }

    @Bean
    public AddListenerToJobService addListenerToJobService() {
        boolean addProtocolListener = batchConfig.getDefaultProtocol().isEnabled();
        boolean addLoggingListener = batchConfig.getLogfileSeparation().isEnabled();
        return new AddListenerToJobService(addProtocolListener, addLoggingListener, protocolListener(),
                runningExecutionTrackerListener(), loggingListener(), loggingAfterJobListener());
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        jobRegistry.getJobNames().forEach(jobName -> {
            try {
                AbstractJob job = (AbstractJob) jobRegistry.getJob(jobName);
                this.addListenerToJobService().addListenerToJob(job);
            } catch (NoSuchJobException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Bean
    @ConditionalOnMissingBean
    public JobParametersConverter jobParametersConverter() {
        return new DefaultJobParametersConverter();
    }

}
