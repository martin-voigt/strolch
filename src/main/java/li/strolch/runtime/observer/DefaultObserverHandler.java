/*
 * Copyright 2013 Robert von Burg <eitch@eitchnet.ch>
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package li.strolch.runtime.observer;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import li.strolch.model.StrolchElement;
import li.strolch.runtime.agent.api.StrolchComponent;
import li.strolch.runtime.agent.impl.ComponentContainerImpl;
import li.strolch.runtime.configuration.ComponentConfiguration;

/**
 * @author Robert von Burg <eitch@eitchnet.ch>
 * 
 */
public class DefaultObserverHandler extends StrolchComponent implements ObserverHandler {

	private Map<String, List<Observer>> observerMap;

	@Override
	public void initialize(ComponentConfiguration configuration) {
		this.observerMap = new HashMap<>();
		super.initialize(configuration);
	}

	/**
	 * @param container
	 * @param componentName
	 */
	public DefaultObserverHandler(ComponentContainerImpl container, String componentName) {
		super(container, componentName);
	}

	@Override
	public void add(String key, List<StrolchElement> elements) {
		List<Observer> observerList = this.observerMap.get(key);
		if (observerList != null && !observerList.isEmpty()) {
			for (Observer observer : observerList) {
				try {
					observer.add(key, elements);
				} catch (Exception e) {
					String msg = "Failed to update observer {0} with {1} due to {2}"; //$NON-NLS-1$
					msg = MessageFormat.format(msg, key, observer, e.getMessage());
					logger.error(msg, e);
				}
			}
		}
	}

	@Override
	public void update(String key, List<StrolchElement> elements) {
		List<Observer> observerList = this.observerMap.get(key);
		if (observerList != null && !observerList.isEmpty()) {
			for (Observer observer : observerList) {
				try {
					observer.update(key, elements);
				} catch (Exception e) {
					String msg = "Failed to update observer {0} with {1} due to {2}"; //$NON-NLS-1$
					msg = MessageFormat.format(msg, key, observer, e.getMessage());
					logger.error(msg, e);
				}
			}
		}
	}

	@Override
	public void remove(String key, List<StrolchElement> elements) {
		List<Observer> observerList = this.observerMap.get(key);
		if (observerList != null && !observerList.isEmpty()) {
			for (Observer observer : observerList) {
				try {
					observer.remove(key, elements);
				} catch (Exception e) {
					String msg = "Failed to update observer {0} with {1} due to {2}"; //$NON-NLS-1$
					msg = MessageFormat.format(msg, key, observer, e.getMessage());
					logger.error(msg, e);
				}
			}
		}
	}

	@Override
	public void registerObserver(String key, Observer observer) {
		List<Observer> observerList = this.observerMap.get(key);
		if (observerList == null) {
			observerList = new ArrayList<>();
			this.observerMap.put(key, observerList);
		}
		observerList.add(observer);
		String msg = MessageFormat.format("Registered observer {0} with {1}", key, observer); //$NON-NLS-1$
		logger.info(msg);
	}

	@Override
	public void unregisterObserver(String key, Observer observer) {
		List<Observer> observerList = this.observerMap.get(key);
		if (observerList != null && observerList.remove(observer)) {
			String msg = MessageFormat.format("Unregistered observer {0} with {1}", key, observer); //$NON-NLS-1$
			logger.info(msg);
		}
	}
}
