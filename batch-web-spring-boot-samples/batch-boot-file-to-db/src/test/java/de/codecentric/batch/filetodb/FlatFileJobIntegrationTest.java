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

package de.codecentric.batch.filetodb;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.batch.core.ExitStatus;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = WebEnvironment.DEFINED_PORT, properties = { "server.port=8090" })
public class FlatFileJobIntegrationTest {

	private RestTemplate restTemplate = new RestTemplateBuilder().build();

	@Test
	public void testLaunchJob() throws Exception {
		// Given
		String jobParameters = "pathToFile=classpath:partner-import.csv";
		// When
		ExitStatus exitStatus = runJobAndWaitForCompletion("localhost", "8090", "flatfileJob", jobParameters);
		// Then
		assertEquals(ExitStatus.COMPLETED.getExitCode(), exitStatus.getExitCode());
	}

	protected ExitStatus runJobAndWaitForCompletion(String hostname, String port, String jobName, String jobParameters)
			throws InterruptedException {
		MultiValueMap<String, String> parameters = new LinkedMultiValueMap<String, String>();
		parameters.add("jobParameters", jobParameters);
		String jobExecutionId = restTemplate.postForObject(
				"http://" + hostname + ":" + port + "/batch/operations/jobs/" + jobName + "", parameters, String.class);
		ExitStatus exitStatus = getStatus(hostname, port, jobExecutionId);
		// Wait for end of job
		while (exitStatus.isRunning()) {
			Thread.sleep(100);
			exitStatus = getStatus(hostname, port, jobExecutionId);
		}
		return exitStatus;
	}

	private ExitStatus getStatus(String hostname, String port, String jobExecutionId) {
		String jobstatus = restTemplate.getForObject(
				"http://" + hostname + ":" + port + "/batch/operations/jobs/executions/" + jobExecutionId,
				String.class);
		return new ExitStatus(jobstatus);
	}

}
