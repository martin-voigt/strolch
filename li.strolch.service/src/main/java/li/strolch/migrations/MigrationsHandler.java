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
package li.strolch.migrations;

import java.io.File;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import li.strolch.agent.api.ComponentContainer;
import li.strolch.agent.api.RealmHandler;
import li.strolch.agent.api.StrolchComponent;
import li.strolch.runtime.configuration.ComponentConfiguration;
import li.strolch.runtime.configuration.RuntimeConfiguration;
import li.strolch.runtime.privilege.PrivilegeHandler;
import ch.eitchnet.privilege.model.Certificate;
import ch.eitchnet.utils.Version;
import ch.eitchnet.utils.collections.MapOfLists;

/**
 * @author Robert von Burg <eitch@eitchnet.ch>
 */
public class MigrationsHandler extends StrolchComponent {

	private static final String PROP_VERBOSE = "verbose";
	private static final String PROP_POLL_MIGRATIONS = "pollMigrations";
	private static final String PROP_POLL_WAIT = "pollWait";
	private static final String PROP_RUN_MIGRATIONS_ON_START = "runMigrationsOnStart";
	private static final String PATH_MIGRATIONS = "migrations";

	private boolean runMigrationsOnStart;
	private boolean verbose;

	private Migrations migrations;
	private MapOfLists<String, Version> lastMigrations;
	private File migrationsPath;

	private Timer migrationTimer;
	private boolean pollMigrations;
	private int pollWait;

	public MigrationsHandler(ComponentContainer container, String componentName) {
		super(container, componentName);
	}

	public MapOfLists<String, Version> getLastMigrations() {
		if (this.lastMigrations == null)
			return new MapOfLists<>();
		return this.lastMigrations;
	}

	public Map<String, Version> getCurrentVersions(Certificate cert) {
		CurrentMigrationVersionQuery query = new CurrentMigrationVersionQuery(getContainer());
		query.doQuery(cert);
		return query.getCurrentVersions();
	}

	public MapOfLists<String, Version> queryMigrationsToRun(Certificate cert) {
		Map<String, Version> currentVersions = getCurrentVersions(cert);
		Migrations migrations = new Migrations(getContainer(), currentVersions);
		migrations.parseMigrations(this.migrationsPath);
		migrations.setVerbose(this.verbose);

		this.migrations = migrations;
		return this.migrations.getMigrationsToRun();
	}

	public void runMigrations(Certificate cert) {
		queryMigrationsToRun(cert);
		this.migrations.runMigrations(cert);
		this.lastMigrations = this.migrations.getMigrationsRan();
	}

	@Override
	public void initialize(ComponentConfiguration configuration) {

		this.runMigrationsOnStart = configuration.getBoolean(PROP_RUN_MIGRATIONS_ON_START, Boolean.FALSE);
		this.verbose = configuration.getBoolean(PROP_VERBOSE, Boolean.FALSE);
		this.pollMigrations = configuration.getBoolean(PROP_VERBOSE, Boolean.FALSE);
		this.pollWait = configuration.getInt(PROP_VERBOSE, 5);

		RuntimeConfiguration runtimeConf = configuration.getRuntimeConfiguration();
		this.migrationsPath = runtimeConf.getDataDir(MigrationsHandler.class.getName(), PATH_MIGRATIONS, false);
		if (this.runMigrationsOnStart && this.migrationsPath.exists()) {

			CurrentMigrationVersionQuery query = new CurrentMigrationVersionQuery(getContainer());
			PrivilegeHandler privilegeHandler = getContainer().getComponent(PrivilegeHandler.class);
			QueryCurrentVersionsAction action = new QueryCurrentVersionsAction(query);
			privilegeHandler.runAsSystem(RealmHandler.SYSTEM_USER_AGENT, action);
			Map<String, Version> currentVersions = query.getCurrentVersions();

			Migrations migrations = new Migrations(getContainer(), currentVersions);
			migrations.parseMigrations(this.migrationsPath);
			migrations.setVerbose(this.verbose);

			this.migrations = migrations;
		}

		if (this.pollMigrations) {
			this.migrationTimer = new Timer("MigrationTimer", true); //$NON-NLS-1$
			long checkInterval = TimeUnit.MINUTES.toMillis(pollWait);
			this.migrationTimer.schedule(new MigrationPollTask(), checkInterval, checkInterval);
		}

		super.initialize(configuration);
	}

	@Override
	public void start() {

		if (this.runMigrationsOnStart && this.migrations != null) {

			PrivilegeHandler privilegeHandler = getContainer().getComponent(PrivilegeHandler.class);
			RunMigrationsAction action = new RunMigrationsAction(this.migrations);

			privilegeHandler.runAsSystem(RealmHandler.SYSTEM_USER_AGENT, action);
			this.lastMigrations = this.migrations.getMigrationsRan();
		}

		super.start();
	}

	@Override
	public void stop() {

		if (this.migrationTimer != null) {
			this.migrationTimer.cancel();
		}

		this.migrationTimer = null;

		super.stop();
	}

	/**
	 * Simpler {@link TimerTask} to check for sessions which haven't been active for
	 * {@link DefaultStrolchSessionHandler#PARAM_SESSION_TTL_MINUTES} minutes.
	 * 
	 * @author Robert von Burg <eitch@eitchnet.ch>
	 */
	private class MigrationPollTask extends TimerTask {

		@Override
		public void run() {

			CurrentMigrationVersionQuery query = new CurrentMigrationVersionQuery(getContainer());
			PrivilegeHandler privilegeHandler = getContainer().getComponent(PrivilegeHandler.class);
			QueryCurrentVersionsAction queryAction = new QueryCurrentVersionsAction(query);
			privilegeHandler.runAsSystem(RealmHandler.SYSTEM_USER_AGENT, queryAction);
			Map<String, Version> currentVersions = query.getCurrentVersions();

			Migrations migrations = new Migrations(getContainer(), currentVersions);
			migrations.parseMigrations(MigrationsHandler.this.migrationsPath);
			migrations.setVerbose(MigrationsHandler.this.verbose);

			MigrationsHandler.this.migrations = migrations;

			if (migrations.getMigrationsToRun().isEmpty()) {
				logger.info("There are no migrations required at the moment!");
			} else {
				RunMigrationsAction runMigrationsAction = new RunMigrationsAction(MigrationsHandler.this.migrations);
				privilegeHandler.runAsSystem(RealmHandler.SYSTEM_USER_AGENT, runMigrationsAction);
				MigrationsHandler.this.lastMigrations = MigrationsHandler.this.migrations.getMigrationsRan();
			}
		}
	}
}