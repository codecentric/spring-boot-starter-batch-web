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

/**
 * @author Tobias Flohre
 */
public enum Action {
	FAIL_ON_READ, FAIL_ON_AFTER_READ, FAIL_ON_READ_ERROR,
	FAIL_ON_BEFORE_PROCESS, FAIL_ON_PROCESS, FAIL_ON_AFTER_PROCESS, FAIL_ON_PROCESS_ERROR,
	FAIL_ON_BEFORE_WRITE, FAIL_ON_WRITE, FAIL_ON_AFTER_WRITE, FAIL_ON_WRITE_ERROR,
	FAIL_ON_SKIP_IN_PROCESS, FAIL_ON_SKIP_IN_WRITE, FILTER
}
