/*
 * Copyright 2012-2014 the original author or authors.
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

package de.codecentric.batch.monitoring;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RunningExecutionTracker {
	
	private Map<Long,String> runningExecutions = new ConcurrentHashMap<Long,String>();
	
	public void addRunningExecution(String jobName, Long executionId){
		runningExecutions.put(executionId, jobName);
	}
	
	public void removeRunningExecution(Long executionId){
		runningExecutions.remove(executionId);
	}
	
	public Set<Long> getAllRunningExecutionIds(){
		return new HashSet<Long>(runningExecutions.keySet());
	}
	
	public Set<Long> getRunningExecutionIdsForJobName(String jobName){
		Set<Long> runningExecutionIds = new HashSet<Long>();
		for (Entry<Long,String> entry:runningExecutions.entrySet()){
			if (entry.getValue().equals(jobName)){
				runningExecutionIds.add(entry.getKey());
			}
		}
		return runningExecutionIds;
	}

}
