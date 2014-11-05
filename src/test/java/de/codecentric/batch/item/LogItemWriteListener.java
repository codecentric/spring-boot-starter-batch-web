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
package de.codecentric.batch.item;

import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.batch.core.ItemWriteListener;

/**
 * @author Tobias Flohre
 */
public class LogItemWriteListener implements ItemWriteListener<String> {
	
	private static final Log log = LogFactory.getLog(LogItemWriteListener.class);

	@Override
	public void beforeWrite(List<? extends String> items) {
		for (String item: items){
			log.debug("Item: " +item);
		}
	}

	@Override
	public void afterWrite(List<? extends String> items) {
		for (String item: items){
			log.debug("Item: " +item);
		}
	}

	@Override
	public void onWriteError(Exception exception, List<? extends String> items) {
		for (String item: items){
			log.debug("Item: " +item);
		}
	}

}
