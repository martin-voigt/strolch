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
package li.strolch.persistence.postgresql.dao.test;

import java.io.File;
import java.sql.SQLException;

import li.strolch.persistence.api.PersistenceHandler;
import li.strolch.persistence.api.StrolchTransaction;
import li.strolch.testbase.runtime.RuntimeMock;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Robert von Burg <eitch@eitchnet.ch>
 */
public class PostgresqlContainerTest extends RuntimeMock {

	protected static PersistenceHandler persistenceHandler;

	@BeforeClass
	public static void beforeClass() throws SQLException {

		File rootPath = new File(AbstractDaoImplTest.RUNTIME_PATH);
		File configSrc = new File(AbstractDaoImplTest.CONFIG_SRC);
		RuntimeMock.mockRuntime(rootPath, configSrc);
		new File(rootPath, AbstractDaoImplTest.DB_STORE_PATH_DIR).mkdir();
		RuntimeMock.startContainer(rootPath);

		// initialize the component configuration
		persistenceHandler = getContainer().getComponent(PersistenceHandler.class);
	}

	@Test
	public void shouldStartContainer() {
		try (StrolchTransaction tx = getPersistenceHandler().openTx()) {
			tx.getOrderDao().queryKeySet();
		}
	}

	@AfterClass
	public static void afterClass() {
		RuntimeMock.destroyRuntime();
	}
}
