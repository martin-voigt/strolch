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
package li.strolch.privilege.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.MessageFormat;
import java.util.Map;

import li.strolch.privilege.base.PrivilegeException;
import li.strolch.privilege.handler.DefaultPrivilegeHandler;
import li.strolch.privilege.handler.EncryptionHandler;
import li.strolch.privilege.handler.PersistenceHandler;
import li.strolch.privilege.handler.PrivilegeHandler;
import li.strolch.privilege.handler.UserChallengeHandler;
import li.strolch.privilege.model.internal.PrivilegeContainerModel;
import li.strolch.privilege.policy.PrivilegePolicy;
import li.strolch.privilege.xml.PrivilegeConfigSaxReader;
import li.strolch.utils.helper.ClassHelper;
import li.strolch.utils.helper.XmlHelper;

/**
 * This class implements the initializing of the {@link PrivilegeHandler} by loading an XML file containing the
 * configuration
 * 
 * @author Robert von Burg <eitch@eitchnet.ch>
 */
public class PrivilegeInitializationHelper {

	/**
	 * Initializes the {@link DefaultPrivilegeHandler} from the configuration file
	 * 
	 * @param privilegeXmlFile
	 *            a {@link File} reference to the XML file containing the configuration for Privilege
	 * 
	 * @return the initialized {@link PrivilegeHandler} where the {@link EncryptionHandler} and
	 *         {@link PersistenceHandler} are set and initialized as well
	 */
	public static PrivilegeHandler initializeFromXml(File privilegeXmlFile) {

		// make sure file exists
		if (!privilegeXmlFile.exists()) {
			String msg = "Privilege file does not exist at path {0}"; //$NON-NLS-1$
			msg = MessageFormat.format(msg, privilegeXmlFile.getAbsolutePath());
			throw new PrivilegeException(msg);
		}

		// delegate using input stream
		try (FileInputStream fin = new FileInputStream(privilegeXmlFile)) {
			return initializeFromXml(fin);
		} catch (Exception e) {
			String msg = "Failed to load configuration from {0}"; //$NON-NLS-1$
			msg = MessageFormat.format(msg, privilegeXmlFile.getAbsolutePath());
			throw new PrivilegeException(msg, e);
		}
	}

	/**
	 * Initializes the {@link PrivilegeHandler} by loading from the given input stream. This stream must be a valid XML
	 * source
	 * 
	 * @param privilegeConfigInputStream
	 *            the XML stream containing the privilege configuration
	 * 
	 * @return the initialized {@link PrivilegeHandler} where the {@link EncryptionHandler} and
	 *         {@link PersistenceHandler} are set and initialized as well
	 */
	public static PrivilegeHandler initializeFromXml(InputStream privilegeConfigInputStream) {

		// parse configuration file
		PrivilegeContainerModel containerModel = new PrivilegeContainerModel();
		PrivilegeConfigSaxReader xmlHandler = new PrivilegeConfigSaxReader(containerModel);
		XmlHelper.parseDocument(privilegeConfigInputStream, xmlHandler);

		return initializeFromXml(containerModel);
	}

	/**
	 * Initializes the {@link PrivilegeHandler} by initializing from the given {@link PrivilegeContainerModel}
	 * 
	 * @param containerModel
	 *            the configuration for the {@link PrivilegeHandler}
	 * 
	 * @return the initialized {@link PrivilegeHandler} where the {@link EncryptionHandler} and
	 *         {@link PersistenceHandler} are set and initialized as well
	 */
	public static PrivilegeHandler initializeFromXml(PrivilegeContainerModel containerModel) {

		// initialize encryption handler
		String encryptionHandlerClassName = containerModel.getEncryptionHandlerClassName();
		EncryptionHandler encryptionHandler = ClassHelper.instantiateClass(encryptionHandlerClassName);
		Map<String, String> parameterMap = containerModel.getEncryptionHandlerParameterMap();
		try {
			encryptionHandler.initialize(parameterMap);
		} catch (Exception e) {
			String msg = "EncryptionHandler {0} could not be initialized"; //$NON-NLS-1$
			msg = MessageFormat.format(msg, encryptionHandlerClassName);
			throw new PrivilegeException(msg, e);
		}

		// initialize persistence handler
		String persistenceHandlerClassName = containerModel.getPersistenceHandlerClassName();
		PersistenceHandler persistenceHandler = ClassHelper.instantiateClass(persistenceHandlerClassName);
		parameterMap = containerModel.getPersistenceHandlerParameterMap();
		try {
			persistenceHandler.initialize(parameterMap);
		} catch (Exception e) {
			String msg = "PersistenceHandler {0} could not be initialized"; //$NON-NLS-1$
			msg = MessageFormat.format(msg, persistenceHandlerClassName);
			throw new PrivilegeException(msg, e);
		}

		// initialize challenge handler
		UserChallengeHandler challengeHandler;
		String challengeHandlerClassName = containerModel.getUserChallengeHandlerClassName();
		challengeHandler = ClassHelper.instantiateClass(challengeHandlerClassName);
		parameterMap = containerModel.getUserChallengeHandlerParameterMap();
		try {
			challengeHandler.initialize(parameterMap);
		} catch (Exception e) {
			String msg = "UserChallengeHandler {0} could not be initialized"; //$NON-NLS-1$
			msg = MessageFormat.format(msg, persistenceHandlerClassName);
			throw new PrivilegeException(msg, e);
		}

		// initialize privilege handler
		DefaultPrivilegeHandler privilegeHandler = new DefaultPrivilegeHandler();
		parameterMap = containerModel.getParameterMap();
		Map<String, Class<PrivilegePolicy>> policyMap = containerModel.getPolicies();
		try {
			privilegeHandler.initialize(parameterMap, encryptionHandler, persistenceHandler, challengeHandler,
					policyMap);
		} catch (Exception e) {
			String msg = "PrivilegeHandler {0} could not be initialized"; //$NON-NLS-1$
			msg = MessageFormat.format(msg, privilegeHandler.getClass().getName());
			throw new PrivilegeException(msg, e);
		}

		return privilegeHandler;
	}
}
