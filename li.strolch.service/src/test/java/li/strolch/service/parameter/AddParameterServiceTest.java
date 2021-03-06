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
package li.strolch.service.parameter;

import li.strolch.model.Locator;
import li.strolch.model.parameter.FloatParameter;
import li.strolch.service.parameter.AddParameterService;
import li.strolch.service.parameter.AddParameterService.AddParameterArg;
import li.strolch.service.test.AbstractRealmServiceTest;

import org.junit.Test;

/**
 * @author Robert von Burg <eitch@eitchnet.ch>
 */
public class AddParameterServiceTest extends AbstractRealmServiceTest {

	@Test
	public void runTest() {

		AddParameterArg arg = new AddParameterArg();
		arg.locator = Locator.valueOf("Resource/Ball/yellow/Bag/parameters");
		FloatParameter parameter = new FloatParameter("diameter", "Diameter", 22.0);
		parameter.setInterpretation("Dimension");
		parameter.setUom("cm");
		arg.parameter = parameter;

		runServiceInAllRealmTypes(AddParameterService.class, arg);
	}
}
