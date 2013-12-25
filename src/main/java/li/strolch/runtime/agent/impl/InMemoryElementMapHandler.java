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
package li.strolch.runtime.agent.impl;

import java.util.HashMap;

import li.strolch.persistence.api.PersistenceHandler;
import li.strolch.runtime.StrolchConstants;
import li.strolch.runtime.agent.api.StrolchAgent;
import li.strolch.runtime.configuration.ComponentConfiguration;
import li.strolch.runtime.configuration.RuntimeConfiguration;

/**
 * @author Robert von Burg <eitch@eitchnet.ch>
 */
public class InMemoryElementMapHandler extends AbstractElementMapHandler {

	/**
	 * @param container
	 * @param componentName
	 */
	public InMemoryElementMapHandler(ComponentContainerImpl container, String componentName) {
		super(container, componentName);
	}

	@Override
	public void initialize(ComponentConfiguration configuration) {

		RuntimeConfiguration runtimeConfiguration = configuration.getRuntimeConfiguration();
		String[] realms = runtimeConfiguration.getStringArray(StrolchAgent.PROP_REALMS, StrolchConstants.DEFAULT_REALM);

		this.realms = new HashMap<>();
		for (String realm : realms) {
			PersistenceHandler persistenceHandler = getContainer().getComponent(PersistenceHandler.class);
			TransactionalResourceMap resourceMap = new TransactionalResourceMap(realm, persistenceHandler);
			TransactionalOrderMap orderMap = new TransactionalOrderMap(realm, persistenceHandler);
			StrolchRealm strolchRealm = new StrolchRealm(realm, resourceMap, orderMap);
			this.realms.put(realm, strolchRealm);
		}

		super.initialize(configuration);
	}
}
