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
package li.strolch.persistence.postgresql;

import java.sql.Connection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import li.strolch.agent.api.StrolchRealm;
import li.strolch.persistence.api.AbstractTransaction;
import li.strolch.persistence.api.ActivityDao;
import li.strolch.persistence.api.AuditDao;
import li.strolch.persistence.api.OrderDao;
import li.strolch.persistence.api.PersistenceHandler;
import li.strolch.persistence.api.ResourceDao;
import li.strolch.privilege.model.Certificate;
import li.strolch.runtime.privilege.PrivilegeHandler;

public class PostgreSqlStrolchTransaction extends AbstractTransaction {

	private static final Logger logger = LoggerFactory.getLogger(PostgreSqlStrolchTransaction.class);
	private PostgreSqlPersistenceHandler persistenceHandler;

	private PostgreSqlOrderDao orderDao;
	private PostgreSqlResourceDao resourceDao;
	private PostgreSqlActivityDao activityDao;
	private AuditDao auditDao;
	private Connection connection;

	public PostgreSqlStrolchTransaction(PrivilegeHandler privilegeHandler, StrolchRealm realm, Certificate certificate,
			String action, PostgreSqlPersistenceHandler persistenceHandler) {
		super(privilegeHandler, realm, certificate, action);
		this.persistenceHandler = persistenceHandler;
	}

	@Override
	protected void writeChanges() throws Exception {

		// first perform DAOs
		if (this.orderDao != null)
			this.orderDao.flush();
		if (this.resourceDao != null)
			this.resourceDao.flush();
		if (this.activityDao != null)
			this.activityDao.flush();

		// don't commit the connection, this is done in postCommit when we close the connection
	}

	@Override
	protected void rollback() throws Exception {
		if (this.connection != null) {
			try {
				this.connection.rollback();
			} finally {
				try {
					this.connection.close();
				} catch (Exception e) {
					logger.error("Failed to close connection due to " + e.getMessage(), e); //$NON-NLS-1$
				}
			}
		}
	}

	@Override
	protected void commit() throws Exception {
		if (this.connection != null) {
			this.connection.commit();
			this.connection.close();
		}
	}

	OrderDao getOrderDao() {
		if (this.orderDao == null)
			this.orderDao = new PostgreSqlOrderDao(this);
		return this.orderDao;
	}

	ResourceDao getResourceDao() {
		if (this.resourceDao == null)
			this.resourceDao = new PostgreSqlResourceDao(this);
		return this.resourceDao;
	}

	ActivityDao getActivityDao() {
		if (this.activityDao == null)
			this.activityDao = new PostgreSqlActivityDao(this);
		return this.activityDao;
	}

	public AuditDao getAuditDao() {
		if (this.auditDao == null)
			this.auditDao = new PostgreSqlAuditDao(this);
		return this.auditDao;
	}

	Connection getConnection() {
		if (this.connection == null) {
			this.connection = this.persistenceHandler.getConnection(getRealm().getRealm());
		}
		return this.connection;
	}

	@Override
	public PersistenceHandler getPersistenceHandler() {
		return this.persistenceHandler;
	}
}
