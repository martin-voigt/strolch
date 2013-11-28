/*
 * Copyright (c) 2012, Robert von Burg
 *
 * All rights reserved.
 *
 * This file is part of the XXX.
 *
 *  XXX is free software: you can redistribute 
 *  it and/or modify it under the terms of the GNU General Public License as 
 *  published by the Free Software Foundation, either version 3 of the License, 
 *  or (at your option) any later version.
 *
 *  XXX is distributed in the hope that it will 
 *  be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with XXX.  If not, see 
 *  <http://www.gnu.org/licenses/>.
 */
package li.strolch.runtime.agent;

import java.io.File;
import java.text.MessageFormat;

import li.strolch.model.xml.XmlModelDefaultHandler;
import li.strolch.model.xml.XmlModelDefaultHandler.XmlModelStatistics;
import li.strolch.runtime.component.ComponentContainer;
import li.strolch.runtime.component.StrolchComponent;
import li.strolch.runtime.configuration.ComponentConfiguration;
import li.strolch.runtime.configuration.RuntimeConfiguration;
import ch.eitchnet.utils.helper.StringHelper;

/**
 * @author Robert von Burg <eitch@eitchnet.ch>
 * 
 */
public class TransientElementMapController extends StrolchComponent {

	private File modelFile;

	/**
	 * @param container
	 * @param componentName
	 */
	public TransientElementMapController(ComponentContainer container, String componentName) {
		super(container, componentName);
	}

	@Override
	public void initialize(ComponentConfiguration configuration) {

		RuntimeConfiguration runtimeConfiguration = configuration.getRuntimeConfiguration();
		File modelFile = runtimeConfiguration.getDataFile(StrolchAgent.PROP_DATA_STORE_FILE, null, runtimeConfiguration,
				true);
		this.modelFile = modelFile;

		super.initialize(configuration);
	}

	@Override
	public void start() {

		ResourceMap resourceMap = getContainer().getComponent(ResourceMap.class);
		OrderMap orderMap = getContainer().getComponent(OrderMap.class);

		InMemoryElementListener elementListener = new InMemoryElementListener(resourceMap, orderMap);
		XmlModelDefaultHandler handler = new XmlModelDefaultHandler(elementListener, this.modelFile);
		handler.parseFile();
		XmlModelStatistics statistics = handler.getStatistics();
		String durationS = StringHelper.formatNanoDuration(statistics.durationNanos);
		logger.info(MessageFormat.format("Loading XML Model file {0} took {1}.", this.modelFile.getName(), durationS)); //$NON-NLS-1$
		logger.info(MessageFormat.format("Loaded {0} Orders", statistics.nrOfOrders)); //$NON-NLS-1$
		logger.info(MessageFormat.format("Loaded {0} Resources", statistics.nrOfResources)); //$NON-NLS-1$

		super.start();
	}
}