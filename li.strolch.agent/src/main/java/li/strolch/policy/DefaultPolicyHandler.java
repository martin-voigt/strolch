/*
 * Copyright 2015 Robert von Burg <eitch@eitchnet.ch>
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
package li.strolch.policy;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.Map;

import li.strolch.agent.api.ComponentContainer;
import li.strolch.agent.api.StrolchComponent;
import li.strolch.exception.StrolchPolicyException;
import li.strolch.model.policy.JavaPolicyDef;
import li.strolch.model.policy.KeyPolicyDef;
import li.strolch.model.policy.PolicyDef;
import li.strolch.model.policy.PolicyDefVisitor;
import li.strolch.persistence.api.StrolchTransaction;
import li.strolch.policy.StrolchPolicyFileParser.PolicyModel;
import li.strolch.policy.StrolchPolicyFileParser.PolicyType;
import li.strolch.runtime.configuration.ComponentConfiguration;
import li.strolch.utils.collections.MapOfMaps;
import li.strolch.utils.helper.XmlHelper;

/**
 * <p>
 * This is the default Strolch {@link PolicyHandler} which implements {@link PolicyDefVisitor}. This
 * {@link PolicyHandler} parses a policy configuration file to handle the look-up of {@link KeyPolicyDef}
 * </p>
 * 
 * This {@link StrolchComponent} uses two configuration properties:
 * <ul>
 * <li>{@link #PROP_READ_POLICY_FILE} - Default is false. If false then no configuration file is read. Useful if all
 * policy definitions are {@link JavaPolicyDef}</li>
 * <li>{@link #PROP_POLICY_CONFIG} - Default is {@link #DEF_STROLCH_POLICIES_XML}. If {@link #PROP_READ_POLICY_FILE} is
 * true, then this property is used to determine which configuration file to parse for the policy key mappings for
 * {@link KeyPolicyDef}</li>
 * </ul>
 * 
 * @author Robert von Burg <eitch@eitchnet.ch>
 */
public class DefaultPolicyHandler extends StrolchComponent implements PolicyHandler, PolicyDefVisitor {

	private static final String PROP_POLICY_CONFIG = "policyConfig";
	private static final String PROP_READ_POLICY_FILE = "readPolicyFile";
	private static final String DEF_STROLCH_POLICIES_XML = "StrolchPolicies.xml";

	private MapOfMaps<String, String, Class<? extends StrolchPolicy>> classByTypeMap;

	public DefaultPolicyHandler(ComponentContainer container, String componentName) {
		super(container, componentName);
	}

	@Override
	public void initialize(ComponentConfiguration configuration) throws Exception {

		if (configuration.getBoolean(PROP_READ_POLICY_FILE, Boolean.FALSE)) {
			File policyFile = configuration.getConfigFile(PROP_POLICY_CONFIG, DEF_STROLCH_POLICIES_XML,
					configuration.getRuntimeConfiguration());
			parsePolicyFile(policyFile);
		} else {
			logger.warn("Not loading Policy configuration file, as disabled by config");
		}

		super.initialize(configuration);
	}

	@Override
	public <T extends StrolchPolicy> T getPolicy(PolicyDef policyDef, StrolchTransaction tx) {
		try {

			Class<T> clazz = policyDef.accept(this);
			Constructor<T> constructor = clazz.getConstructor(ComponentContainer.class, StrolchTransaction.class);
			return constructor.newInstance(getContainer(), tx);

		} catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException
				| IllegalArgumentException | InvocationTargetException | ClassNotFoundException e) {
			throw new StrolchPolicyException(
					MessageFormat.format("Failed to instantiate policy {0} due to {1}", policyDef, e.getMessage()), e);
		}
	}

	@Override
	public <T> Class<T> visit(JavaPolicyDef policyDef) throws ClassNotFoundException {
		String value = policyDef.getValue();
		@SuppressWarnings("unchecked")
		Class<T> clazz = (Class<T>) Class.forName(value);
		return clazz;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> Class<T> visit(KeyPolicyDef policyDef) throws ClassNotFoundException {
		Class<?> clazz = this.classByTypeMap.getElement(policyDef.getType(), policyDef.getValue());
		if (clazz == null)
			throw new StrolchPolicyException(MessageFormat.format("No policy is configured for {0}", policyDef));
		return (Class<T>) clazz;
	}

	@SuppressWarnings("unchecked")
	private void parsePolicyFile(File policyFile) {

		// first we parse the file
		StrolchPolicyFileParser xmlHandler = new StrolchPolicyFileParser();
		XmlHelper.parseDocument(policyFile, xmlHandler);
		PolicyModel policyModel = xmlHandler.getPolicyModel();

		// then we iterate the parsed model, validating that we can:
		// - access the API class
		// - instantiate the class
		// - assign the class to the API
		Map<String, PolicyType> policyTypes = policyModel.getPolicyTypes();
		this.classByTypeMap = new MapOfMaps<>();
		for (PolicyType policyType : policyTypes.values()) {

			String type = policyType.getType();
			String api = policyType.getApi();

			try {

				// get the API class
				Class<?> apiClass = Class.forName(api);

				// now iterate all configured policies for this API
				Map<String, String> policyByKeyMap = policyType.getPolicyByKeyMap();
				for (String key : policyByKeyMap.keySet()) {
					String className = policyByKeyMap.get(key);

					try {

						// get the class
						Class<?> implClass = Class.forName(className);

						// assert API is a Policy
						if (!StrolchPolicy.class.isAssignableFrom(implClass)) {
							throw new StrolchPolicyException("Invalid " + StrolchPolicyFileParser.POLICY
									+ " configuration for Type=" + type + " Key=" + key + " as " + className
									+ " is not a " + StrolchPolicy.class.getName());
						}

						if (!apiClass.isAssignableFrom(implClass)) {
							throw new StrolchPolicyException(
									"Invalid " + StrolchPolicyFileParser.POLICY + " configuration for Type=" + type
											+ " Key=" + key + " as " + className + " is not assignable from " + api);
						}

						// store the implementation class
						logger.info("Loaded Policy " + type + " / " + key + " / " + className);
						this.classByTypeMap.addElement(type, key, (Class<? extends StrolchPolicy>) implClass);

					} catch (ClassNotFoundException e) {
						throw new StrolchPolicyException("Invalid " + StrolchPolicyFileParser.POLICY
								+ " configuration for Type=" + type + " Key=" + key + " due to " + e.getMessage(), e);
					}
				}
			} catch (ClassNotFoundException e) {
				throw new StrolchPolicyException("Invalid " + StrolchPolicyFileParser.POLICY_TYPE
						+ " configuration for Type=" + type + " due to " + e.getMessage(), e);
			}
		}
	}
}
