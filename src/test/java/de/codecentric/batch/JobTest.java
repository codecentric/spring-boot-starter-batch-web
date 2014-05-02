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

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@ContextConfiguration(classes = { TestConfiguration.class })
@RunWith(SpringJUnit4ClassRunner.class)
public class JobTest {

	@Autowired
	private JobOperator jobOperator;

	@Autowired
	private JobRepository jobRepository;

	@Test
	public void runJavaConfigJob() throws Exception {
		File file = new File("target/out-javaconfig.txt");
		file.delete();
		jobOperator.start("job", "");
		while (jobRepository.getLastJobExecution("job", new JobParameters()).getStatus().isRunning()) {
			Thread.sleep(100);
		}
		assertEquals(BatchStatus.COMPLETED, jobRepository.getLastJobExecution("job", new JobParameters()).getStatus());
		assertEquals(3, FileUtils.readLines(file).size());
	}

	@Test
	public void runXmlJob() throws Exception {
		File file = new File("target/out-xmlconfig.txt");
		file.delete();
		jobOperator.start("jobxml", "");
		while (jobRepository.getLastJobExecution("jobxml", new JobParameters()).getStatus().isRunning()) {
			Thread.sleep(100);
		}
		assertEquals(BatchStatus.COMPLETED, jobRepository.getLastJobExecution("jobxml", new JobParameters()).getStatus());
		assertEquals(10, FileUtils.readLines(file).size());
	}

}
