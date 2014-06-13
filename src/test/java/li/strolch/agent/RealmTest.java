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
package li.strolch.agent;

import static li.strolch.agent.ComponentContainerTest.PATH_REALM_CONTAINER;
import static li.strolch.agent.ComponentContainerTest.PATH_REALM_RUNTIME;
import static li.strolch.agent.ComponentContainerTest.destroyContainer;
import static li.strolch.agent.ComponentContainerTest.logger;
import static li.strolch.agent.ComponentContainerTest.startContainer;
import static li.strolch.agent.ComponentContainerTest.testContainer;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import li.strolch.agent.api.ComponentContainer;
import li.strolch.agent.api.StrolchAgent;
import li.strolch.agent.impl.DataStoreMode;

import org.junit.Test;

/**
 * @author Robert von Burg <eitch@eitchnet.ch>
 */
@SuppressWarnings("nls")
public class RealmTest {

	@Test
	public void shouldStartRealmTestContainer() {

		try {
			StrolchAgent agent = startContainer(PATH_REALM_RUNTIME, PATH_REALM_CONTAINER);
			testContainer(agent);

			ComponentContainer container = agent.getContainer();
			Set<String> realmNames = container.getRealmNames();
			assertEquals(6, realmNames.size());

			Set<String> expectedRealmNames = new HashSet<>(Arrays.asList("defaultRealm", "myRealm", "otherRealm",
					"cachedRealm", "transactionalRealm", "emptyRealm"));
			assertEquals(expectedRealmNames, realmNames);

			assertEquals(DataStoreMode.TRANSIENT, container.getRealm("defaultRealm").getMode());
			assertEquals(DataStoreMode.TRANSIENT, container.getRealm("myRealm").getMode());
			assertEquals(DataStoreMode.TRANSIENT, container.getRealm("otherRealm").getMode());
			assertEquals(DataStoreMode.CACHED, container.getRealm("cachedRealm").getMode());
			assertEquals(DataStoreMode.TRANSACTIONAL, container.getRealm("transactionalRealm").getMode());
			assertEquals(DataStoreMode.EMPTY, container.getRealm("emptyRealm").getMode());

			destroyContainer(agent);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			throw e;
		}
	}
}
