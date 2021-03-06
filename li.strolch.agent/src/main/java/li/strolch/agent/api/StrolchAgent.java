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
package li.strolch.agent.api;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import li.strolch.agent.impl.ComponentContainerImpl;
import li.strolch.runtime.configuration.ConfigurationParser;
import li.strolch.runtime.configuration.RuntimeConfiguration;
import li.strolch.runtime.configuration.StrolchConfiguration;
import li.strolch.utils.helper.StringHelper;

/**
 * @author Robert von Burg <eitch@eitchnet.ch>
 */
public class StrolchAgent {

	public static final String AGENT_VERSION_PROPERTIES = "/agentVersion.properties"; //$NON-NLS-1$
	private static final Logger logger = LoggerFactory.getLogger(StrolchAgent.class);

	private ComponentContainerImpl container;
	private StrolchConfiguration strolchConfiguration;
	private StrolchVersion appVersion;

	public StrolchAgent(StrolchVersion appVersion) {
		this.appVersion = appVersion;
	}

	public StrolchConfiguration getStrolchConfiguration() {
		return this.strolchConfiguration;
	}

	public ComponentContainer getContainer() {
		return this.container;
	}

	public String getApplicationName() {
		return this.strolchConfiguration.getRuntimeConfiguration().getApplicationName();
	}

	public void initialize() {
		if (this.container == null)
			throw new RuntimeException("Please call setup first!");
		this.container.initialize(this.strolchConfiguration);
	}

	public void start() {
		if (this.container == null)
			throw new RuntimeException("Please call setup first!");
		this.container.start();
	}

	public void stop() {
		if (this.container != null)
			this.container.stop();
	}

	public void destroy() {
		if (this.container != null)
			this.container.destroy();
		this.container = null;
	}

	/**
	 * <p>
	 * <b>Note:</b> Use {@link StrolchBootstrapper} instead of calling this method directly!
	 * </p>
	 * 
	 * <p>
	 * Sets up the agent by parsing the configuration file and initializes the given environment
	 * </p>
	 * 
	 * @param environment
	 * @param configPathF
	 * @param dataPathF
	 * @param tempPathF
	 */
	void setup(String environment, File configPathF, File dataPathF, File tempPathF) {

		String msg = "[{0}] Setting up Strolch Container using the following paths:"; //$NON-NLS-1$
		logger.info(MessageFormat.format(msg, environment));
		logger.info(" - Config: " + configPathF.getAbsolutePath());
		logger.info(" - Data: " + dataPathF.getAbsolutePath());
		logger.info(" - Temp: " + tempPathF.getAbsolutePath());

		this.strolchConfiguration = ConfigurationParser.parseConfiguration(environment, configPathF, dataPathF,
				tempPathF);

		ComponentContainerImpl container = new ComponentContainerImpl(this);
		container.setup(this.strolchConfiguration);

		this.container = container;

		RuntimeConfiguration config = this.strolchConfiguration.getRuntimeConfiguration();
		logger.info(MessageFormat.format("Setup Agent {0}:{1}", config.getApplicationName(), config.getEnvironment())); //$NON-NLS-1$
	}

	protected void assertContainerStarted() {
		if (this.container == null || this.container.getState() != ComponentState.STARTED) {
			String msg = "Container is not yet started!"; //$NON-NLS-1$
			throw new IllegalStateException(msg);
		}
	}

	/**
	 * @return Returns the pseudo unique Id to be used during object creation from external services.
	 */
	public static synchronized String getUniqueId() {
		return StringHelper.getUniqueId();
	}

	/**
	 * @return Returns the pseudo unique Id to be used during object creation from external services.
	 */
	public static synchronized Long getUniqueIdLong() {
		return StringHelper.getUniqueIdLong();
	}

	private VersionQueryResult versionQueryResult;

	public VersionQueryResult getVersion() {
		if (this.versionQueryResult == null) {

			VersionQueryResult queryResult = new VersionQueryResult();
			queryResult.setAppVersion(this.appVersion);

			Properties properties = new Properties();

			try (InputStream stream = getClass().getResourceAsStream(AGENT_VERSION_PROPERTIES);) {
				properties.load(stream);
				AgentVersion agentVersion = new AgentVersion(
						getStrolchConfiguration().getRuntimeConfiguration().getApplicationName(), properties);
				queryResult.setAgentVersion(agentVersion);
			} catch (IOException e) {
				String msg = MessageFormat.format("Failed to read version properties for agent: {0}", e.getMessage()); //$NON-NLS-1$
				queryResult.getErrors().add(msg);
				logger.error(msg, e);
			}

			Set<Class<?>> componentTypes = this.container.getComponentTypes();
			for (Class<?> componentType : componentTypes) {
				StrolchComponent component = (StrolchComponent) this.container.getComponent(componentType);
				try {
					ComponentVersion componentVersion = component.getVersion();
					queryResult.add(componentVersion);
				} catch (Exception e) {
					String msg = "Failed to read version properties for component {0} due to: {1}"; //$NON-NLS-1$
					msg = MessageFormat.format(msg, component.getName(), e.getMessage());
					queryResult.getErrors().add(msg);
					logger.error(msg, e);
				}
			}

			this.versionQueryResult = queryResult;
		}

		return this.versionQueryResult;
	}
}
