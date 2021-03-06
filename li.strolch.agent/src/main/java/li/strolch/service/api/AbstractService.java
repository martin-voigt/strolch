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
package li.strolch.service.api;

import java.text.MessageFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import li.strolch.agent.api.ComponentContainer;
import li.strolch.agent.api.StrolchComponent;
import li.strolch.agent.api.StrolchRealm;
import li.strolch.exception.StrolchException;
import li.strolch.persistence.api.StrolchTransaction;
import li.strolch.privilege.base.PrivilegeException;
import li.strolch.privilege.handler.SystemAction;
import li.strolch.privilege.handler.SystemActionWithResult;
import li.strolch.privilege.model.Certificate;
import li.strolch.privilege.model.PrivilegeContext;
import li.strolch.runtime.StrolchConstants;
import li.strolch.runtime.configuration.RuntimeConfiguration;
import li.strolch.runtime.privilege.PrivilegeHandler;
import li.strolch.runtime.privilege.PrivilegedRunnable;
import li.strolch.runtime.privilege.PrivilegedRunnableWithResult;
import li.strolch.utils.dbc.DBC;
import li.strolch.utils.helper.StringHelper;

/**
 * @author Robert von Burg <eitch@eitchnet.ch>
 */
public abstract class AbstractService<T extends ServiceArgument, U extends ServiceResult> implements Service<T, U> {

	protected static final Logger logger = LoggerFactory.getLogger(AbstractService.class);
	private static final long serialVersionUID = 1L;

	private ComponentContainer container;
	private PrivilegeContext privilegeContext;

	/**
	 * Called by the {@link ServiceHandler} to set the {@link PrivilegeContext} before this service is performed
	 * 
	 * @param privilegeContext
	 *            the privilegeContext to set
	 */
	public final void setPrivilegeContext(PrivilegeContext privilegeContext) {
		DBC.PRE.assertNull("PrivilegeContext is already set!", this.privilegeContext); //$NON-NLS-1$
		this.privilegeContext = privilegeContext;
	}

	/**
	 * Return the {@link PrivilegeContext} to perform further privilege authorization validation
	 * 
	 * @return the privilegeContext
	 */
	public final PrivilegeContext getPrivilegeContext() {
		return this.privilegeContext;
	}

	/**
	 * Returns the {@link Certificate} of the user who is performing this service
	 * 
	 * @return the certificate
	 */
	protected final Certificate getCertificate() {
		return this.privilegeContext.getCertificate();
	}

	/**
	 * Called by the {@link ServiceHandler} to set a reference to the {@link ComponentContainer} to be used during
	 * service execution
	 * 
	 * @param container
	 *            the container to set
	 */
	public final void setContainer(ComponentContainer container) {
		this.container = container;
	}

	/**
	 * Returns the reference to the {@link ComponentContainer}
	 * 
	 * @return the container
	 */
	protected final ComponentContainer getContainer() {
		return this.container;
	}

	/**
	 * Returns the reference to the {@link PrivilegeHandler}
	 * 
	 * @return the privilege handler
	 */
	public PrivilegeHandler getPrivilegeHandler() throws IllegalArgumentException {
		return this.container.getPrivilegeHandler();
	}

	/**
	 * Returns the reference to the {@link StrolchComponent} with the given name, if it exists. If it does not exist, an
	 * {@link IllegalArgumentException} is thrown
	 * 
	 * @param clazz
	 * 
	 * @return the component with the given name
	 * 
	 * @throws IllegalArgumentException
	 *             if the component does not exist
	 */
	protected final <V> V getComponent(Class<V> clazz) {
		return this.container.getComponent(clazz);
	}

	/**
	 * Returns the Strolch {@link RuntimeConfiguration}
	 * 
	 * @return the Strolch {@link RuntimeConfiguration}
	 */
	protected final RuntimeConfiguration getRuntimeConfiguration() {
		return this.container.getAgent().getStrolchConfiguration().getRuntimeConfiguration();
	}

	/**
	 * Returns the {@link StrolchRealm} with the given name. If the realm does not exist, then a
	 * {@link StrolchException} is thrown
	 * 
	 * @param realm
	 *            the name of the {@link StrolchRealm} to return
	 * @return the {@link StrolchRealm} with the given name
	 * 
	 * @throws StrolchException
	 *             if the {@link StrolchRealm} does not exist with the given name
	 */
	protected final StrolchRealm getRealm(String realm) throws StrolchException {
		return this.container.getRealm(realm);
	}

	/**
	 * Opens a {@link StrolchTransaction} for the given realm, the action for the TX is this implementation's class
	 * name. This transaction should be used in a try-with-resource clause so it is properly closed
	 * 
	 * @param realm
	 *            the name of the realm to return
	 * 
	 * @return the open {@link StrolchTransaction}
	 * 
	 * @throws StrolchException
	 *             if the {@link StrolchRealm} does not exist with the given name
	 */
	protected StrolchTransaction openTx(String realm) throws StrolchException {
		return this.container.getRealm(realm).openTx(getCertificate(), getClass());
	}

	/**
	 * Opens a {@link StrolchTransaction} by evaluating if the given argument has a realm defined, if not, then the
	 * realm from the user certificate is used. The action for the TX is this implementation's class name. This
	 * transaction should be used in a try-with-resource clause so it is properly closed
	 * 
	 * @param arg
	 *            the {@link ServiceArgument}
	 * 
	 * @return the open {@link StrolchTransaction}
	 * 
	 * @throws StrolchException
	 *             if the {@link StrolchRealm} does not exist with the given name
	 */
	protected StrolchTransaction openArgOrUserTx(ServiceArgument arg) throws StrolchException {
		if (StringHelper.isEmpty(arg.realm))
			return openUserTx();
		return openTx(arg.realm);
	}

	/**
	 * Opens a {@link StrolchTransaction} by evaluating if the given argument has a realm defined, if not, then the
	 * realm from the user certificate is used. The action for the TX is this implementation's class name. This
	 * transaction should be used in a try-with-resource clause so it is properly closed
	 * 
	 * @param arg
	 *            the {@link ServiceArgument}
	 * @param action
	 *            the action to use for the opened TX
	 * 
	 * @return the open {@link StrolchTransaction}
	 * 
	 * @throws StrolchException
	 *             if the {@link StrolchRealm} does not exist with the given name
	 */
	protected StrolchTransaction openArgOrUserTx(ServiceArgument arg, String action) throws StrolchException {
		if (StringHelper.isEmpty(arg.realm))
			return openUserTx();
		return openTx(arg.realm, action);
	}

	/**
	 * Opens a {@link StrolchTransaction} for the given realm. This transaction should be used in a try-with-resource
	 * clause so it is properly closed
	 * 
	 * @param realm
	 *            the name of the realm
	 * @param action
	 *            the action to use for the opened TX
	 * 
	 * @return the open {@link StrolchTransaction}
	 * 
	 * @throws StrolchException
	 *             if the {@link StrolchRealm} does not exist with the given name
	 */
	protected StrolchTransaction openTx(String realm, String action) throws StrolchException {
		return this.container.getRealm(realm).openTx(getCertificate(), action);
	}

	/**
	 * Opens a {@link StrolchTransaction} where the realm retrieved using
	 * {@link ComponentContainer#getRealm(Certificate)}, the action for the TX is this implementation's class name. This
	 * transaction should be used in a try-with-resource clause so it is properly closed
	 * 
	 * @return the open {@link StrolchTransaction}
	 * 
	 * @throws StrolchException
	 *             if the {@link StrolchRealm} does not exist with the given name
	 */
	protected StrolchTransaction openUserTx() throws StrolchException {
		return this.container.getRealm(getCertificate()).openTx(getCertificate(), getClass());
	}

	/**
	 * Opens a {@link StrolchTransaction} where the realm retrieved using
	 * {@link ComponentContainer#getRealm(Certificate)}. This transaction should be used in a try-with-resource clause
	 * so it is properly closed
	 * 
	 * @param action
	 *            the action to use for the opened TX
	 * 
	 * @return the open {@link StrolchTransaction}
	 * 
	 * @throws StrolchException
	 *             if the {@link StrolchRealm} does not exist with the given name
	 */
	protected StrolchTransaction openUserTx(String action) throws StrolchException {
		return this.container.getRealm(getCertificate()).openTx(getCertificate(), action);
	}

	/**
	 * Performs the given {@link SystemAction} as a system user with the given username
	 * 
	 * @param username
	 *            the name of the system user to perform the action as
	 * @param action
	 *            the action to perform
	 * 
	 * @throws PrivilegeException
	 */
	protected void runAs(String username, SystemAction action) throws PrivilegeException {
		this.container.getPrivilegeHandler().runAs(username, action);
	}

	/**
	 * Performs the given {@link SystemAction} as a system user with the given username
	 * 
	 * @param username
	 *            the name of the system user to perform the action as
	 * @param action
	 *            the action to perform
	 * 
	 * @return the result
	 * 
	 * @throws PrivilegeException
	 */
	protected <V> V runWithResult(String username, SystemActionWithResult<V> action) throws PrivilegeException {
		return this.container.getPrivilegeHandler().runWithResult(username, action);
	}

	/**
	 * Performs the given {@link PrivilegedRunnable} as a system user with the given username
	 * 
	 * @param username
	 *            the name of the system user to perform the action as
	 * @param runnable
	 *            the runnable to perform
	 * 
	 * @throws PrivilegeException
	 */
	protected void runAs(String username, PrivilegedRunnable runnable) throws PrivilegeException {
		this.container.getPrivilegeHandler().runAs(username, runnable);
	}

	/**
	 * Performs the given {@link PrivilegedRunnableWithResult} as a system user with the given username
	 * 
	 * @param username
	 *            the name of the system user to perform the action as
	 * @param runnable
	 *            the runnable to perform
	 * 
	 * @return the result
	 * 
	 * @throws PrivilegeException
	 */
	protected <V> V runWithResult(String username, PrivilegedRunnableWithResult<V> runnable) throws PrivilegeException {
		return this.container.getPrivilegeHandler().runWithResult(username, runnable);
	}

	/**
	 * Performs the given {@link SystemAction} as the privileged system user {@link StrolchConstants#SYSTEM_USER_AGENT}
	 * 
	 * @param username
	 *            the name of the system user to perform the action as
	 * @param action
	 *            the action to perform
	 * 
	 * @throws PrivilegeException
	 */
	protected void runAsAgent(SystemAction action) throws PrivilegeException {
		this.container.getPrivilegeHandler().runAsAgent(action);
	}

	/**
	 * Performs the given {@link SystemAction} as the privileged system user {@link StrolchConstants#SYSTEM_USER_AGENT}
	 * 
	 * @param username
	 *            the name of the system user to perform the action as
	 * @param action
	 *            the action to perform
	 * 
	 * @return the result
	 * 
	 * @throws PrivilegeException
	 */
	protected <V> V runAsAgentWithResult(SystemActionWithResult<V> action) throws PrivilegeException {
		return this.container.getPrivilegeHandler().runAsAgentWithResult(action);
	}

	/**
	 * Performs the given {@link PrivilegedRunnable} as the privileged system user
	 * {@link StrolchConstants#SYSTEM_USER_AGENT}
	 * 
	 * @param action
	 *            the action to perform
	 * 
	 * @throws PrivilegeException
	 */
	protected void runAsAgent(PrivilegedRunnable runnable) throws PrivilegeException {
		this.container.getPrivilegeHandler().runAsAgent(runnable);
	}

	/**
	 * Performs the given {@link PrivilegedRunnableWithResult} as the privileged system user
	 * {@link StrolchConstants#SYSTEM_USER_AGENT}
	 * 
	 * @param action
	 *            the action to perform
	 * 
	 * @return the result
	 * 
	 * @throws PrivilegeException
	 */
	protected <V> V runAsAgentWithResult(PrivilegedRunnableWithResult<V> runnable) throws PrivilegeException {
		return this.container.getPrivilegeHandler().runAsAgentWithResult(runnable);
	}

	/**
	 * This method is final as it enforces that the argument is valid, and catches all exceptions and enforces that a
	 * service result is returned. A concrete implementation will implement the business logic in
	 * {@link #internalDoService(ServiceArgument)}
	 */
	@Override
	public final U doService(T argument) {

		if (isArgumentRequired() && argument == null) {

			String msg = "Failed to perform service {0} because no argument was passed although it is required!"; //$NON-NLS-1$
			msg = MessageFormat.format(msg, getClass());
			logger.error(msg);

			U result = getResultInstance();
			result.setState(ServiceResultState.FAILED);
			result.setMessage(msg);
			return result;
		}

		try {

			U serviceResult = internalDoService(argument);
			if (serviceResult == null) {
				String msg = "Service {0} is not properly implemented as it returned a null result!"; //$NON-NLS-1$
				msg = MessageFormat.format(msg, this.getClass().getName());
				throw new StrolchException(msg);
			}

			return serviceResult;

		} catch (Exception e) {
			U result = getResultInstance();
			result.setState(ServiceResultState.FAILED);
			result.setMessage(e.getMessage());
			result.setThrowable(e);
			return result;
		}
	}

	/**
	 * Returns true if this Service requires an argument
	 * 
	 * @return if true, then an argument must be set to execute the service. If the argument is missing, then the
	 *         service execution fails immediately
	 */
	protected boolean isArgumentRequired() {
		return true;
	}

	/**
	 * This method is called if the service execution fails and an instance of the expected {@link ServiceResult} is
	 * required to return to the caller
	 * 
	 * @return an instance of the {@link ServiceResult} returned by this implementation
	 */
	protected abstract U getResultInstance();

	/**
	 * Internal method to perform the {@link Service}. The implementor does not need to handle exceptions as this is
	 * done in the {@link #doService(ServiceArgument)} which calls this method
	 * 
	 * @param arg
	 *            the {@link ServiceArgument} containing the arguments to perform the concrete service
	 * 
	 * @return a {@link ServiceResult} which denotes the execution state of this {@link Service}
	 * 
	 * @throws Exception
	 *             if something went wrong. The caller will catch and handle the {@link ServiceResult}
	 */
	protected abstract U internalDoService(T arg) throws Exception;

	/**
	 * @see li.strolch.privilege.model.Restrictable#getPrivilegeName()
	 */
	@Override
	public String getPrivilegeName() {
		return Service.class.getName();
	}

	/**
	 * @see li.strolch.privilege.model.Restrictable#getPrivilegeValue()
	 */
	@Override
	public Object getPrivilegeValue() {
		return this.getClass().getName();
	}
}
