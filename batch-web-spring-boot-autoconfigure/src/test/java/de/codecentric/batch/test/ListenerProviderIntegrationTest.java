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
package de.codecentric.batch.test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import de.codecentric.batch.TestApplication;
import de.codecentric.batch.listener.TestListener;

/**
 * This test class starts a batch job configured in JavaConfig and tests the ListenerProvider concept.
 *
 * @author Tobias Flohre
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestApplication.class, webEnvironment = WebEnvironment.RANDOM_PORT)

public class ListenerProviderIntegrationTest {

	private TestRestTemplate restTemplate = new TestRestTemplate();

	@Autowired
	private TestListener testListener;

	@Value("${local.server.port}")
	int port;

	@Test
	public void testRunJob() throws InterruptedException {
		MultiValueMap<String, Object> requestMap = new LinkedMultiValueMap<>();
		requestMap.add("jobParameters", "param1=value1");
		Long executionId = restTemplate.postForObject("http://localhost:" + port + "/batch/operations/jobs/simpleJob",
				requestMap, Long.class);
		while (!restTemplate
				.getForObject("http://localhost:" + port + "/batch/operations/jobs/executions/{executionId}",
						String.class, executionId)
				.equals("COMPLETED")) {
			Thread.sleep(1000);
		}
		assertThat(testListener.getCounter() > 1, is(true));
	}

}
