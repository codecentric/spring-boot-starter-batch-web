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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contains metric actions.
 * 
 * @author Tobias Flohre
 */
public class MetricContainer {
	
	List<String> incrementations = new ArrayList<String>();
	
	List<String> decrementations = new ArrayList<String>();
	
	List<String> resets = new ArrayList<String>();
	
	Map<String, Double> gauges = new HashMap<String, Double>();

}
