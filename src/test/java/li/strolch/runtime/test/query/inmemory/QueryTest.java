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
package li.strolch.runtime.test.query.inmemory;

import static li.strolch.model.ModelGenerator.BAG_ID;
import static li.strolch.model.ModelGenerator.PARAM_STRING_ID;
import static li.strolch.model.ModelGenerator.createOrder;
import static li.strolch.model.ModelGenerator.createResource;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import li.strolch.agent.api.ComponentContainer;
import li.strolch.agent.api.StrolchAgent;
import li.strolch.model.Order;
import li.strolch.model.Resource;
import li.strolch.model.parameter.IntegerParameter;
import li.strolch.model.query.IdSelection;
import li.strolch.model.query.OrderQuery;
import li.strolch.model.query.ParameterSelection;
import li.strolch.model.query.ResourceQuery;
import li.strolch.model.query.Selection;
import li.strolch.model.query.StrolchTypeNavigation;
import li.strolch.persistence.api.StrolchTransaction;
import li.strolch.runtime.query.inmemory.InMemoryOrderQueryVisitor;
import li.strolch.runtime.query.inmemory.InMemoryQuery;
import li.strolch.runtime.query.inmemory.InMemoryResourceQueryVisitor;
import li.strolch.runtime.test.component.ComponentContainerTest;

import org.junit.Test;

/**
 * @author Robert von Burg <eitch@eitchnet.ch>
 * 
 */
@SuppressWarnings("nls")
public class QueryTest {

	public static final String PATH_EMPTY_RUNTIME = "target/QueryTest/"; //$NON-NLS-1$

	@Test
	public void shouldQueryResourceWithParamValue() {

		StrolchAgent agent = ComponentContainerTest.startContainer(PATH_EMPTY_RUNTIME,
				ComponentContainerTest.PATH_EMPTY_CONTAINER);
		ComponentContainer container = agent.getContainer();

		Resource res1 = createResource("@1", "Test Resource", "MyType");
		IntegerParameter iP = new IntegerParameter("nbOfBooks", "Number of Books", 33);
		res1.addParameter(BAG_ID, iP);
		try (StrolchTransaction tx = container.getDefaultRealm().openTx()) {
			tx.getResourceMap().add(tx, res1);
		}

		ResourceQuery query = new ResourceQuery(new StrolchTypeNavigation("MyType"));
		List<Selection> elementAndSelections = new ArrayList<>();
		elementAndSelections.add(new IdSelection("@1"));
		elementAndSelections.add(ParameterSelection.integerSelection(BAG_ID, "nbOfBooks", 33));
		query.and().with(elementAndSelections);

		InMemoryResourceQueryVisitor resourceQuery = new InMemoryResourceQueryVisitor();
		InMemoryQuery<Resource> inMemoryQuery = resourceQuery.visit(query);
		List<Resource> result;
		try (StrolchTransaction tx = container.getDefaultRealm().openTx()) {
			result = inMemoryQuery.doQuery(tx.getPersistenceHandler().getResourceDao(tx));
		}
		assertEquals(1, result.size());
		assertEquals("@1", result.get(0).getId());
	}

	@Test
	public void shouldQueryOrderWithParamValue() {

		StrolchAgent agent = ComponentContainerTest.startContainer(PATH_EMPTY_RUNTIME,
				ComponentContainerTest.PATH_EMPTY_CONTAINER);
		ComponentContainer container = agent.getContainer();

		Order o1 = createOrder("@1", "Test Order", "MyType");
		IntegerParameter iP = new IntegerParameter("nbOfBooks", "Number of Books", 33);
		o1.addParameter(BAG_ID, iP);
		try (StrolchTransaction tx = container.getDefaultRealm().openTx()) {
			tx.getOrderMap().add(tx, o1);
		}

		OrderQuery query = new OrderQuery(new StrolchTypeNavigation("MyType"));
		List<Selection> elementAndSelections = new ArrayList<>();
		elementAndSelections.add(new IdSelection("@1"));
		elementAndSelections.add(ParameterSelection.integerSelection(BAG_ID, "nbOfBooks", 33));
		query.and().with(elementAndSelections);

		InMemoryOrderQueryVisitor orderQuery = new InMemoryOrderQueryVisitor();
		InMemoryQuery<Order> inMemoryQuery = orderQuery.visit(query);
		List<Order> result;
		try (StrolchTransaction tx = container.getDefaultRealm().openTx()) {
			result = inMemoryQuery.doQuery(tx.getPersistenceHandler().getOrderDao(tx));
		}
		assertEquals(1, result.size());
		assertEquals("@1", result.get(0).getId());
	}

	@Test
	public void shouldQueryContainsString() {

		StrolchAgent agent = ComponentContainerTest.startContainer(PATH_EMPTY_RUNTIME,
				ComponentContainerTest.PATH_EMPTY_CONTAINER);
		ComponentContainer container = agent.getContainer();

		Resource res1 = createResource("@1", "Test Resource", "MyType");
		try (StrolchTransaction tx = container.getDefaultRealm().openTx()) {
			tx.getResourceMap().add(tx, res1);
		}

		ResourceQuery query = new ResourceQuery(new StrolchTypeNavigation("MyType"));
		query.and().with(ParameterSelection.stringSelection(BAG_ID, PARAM_STRING_ID, "olch").contains(true));
		List<Resource> result;
		try (StrolchTransaction tx = container.getDefaultRealm().openTx()) {
			result = tx.doQuery(query);
		}
		assertEquals(1, result.size());
		assertEquals("@1", result.get(0).getId());
	}

	@Test
	public void shouldNotQueryContainsString() {

		StrolchAgent agent = ComponentContainerTest.startContainer(PATH_EMPTY_RUNTIME,
				ComponentContainerTest.PATH_EMPTY_CONTAINER);
		ComponentContainer container = agent.getContainer();

		Resource res1 = createResource("@1", "Test Resource", "MyType");
		try (StrolchTransaction tx = container.getDefaultRealm().openTx()) {
			tx.getResourceMap().add(tx, res1);
		}

		ResourceQuery query = new ResourceQuery(new StrolchTypeNavigation("MyType"));
		query.and().with(ParameterSelection.stringSelection(BAG_ID, PARAM_STRING_ID, "str").contains(true));
		List<Resource> result;
		try (StrolchTransaction tx = container.getDefaultRealm().openTx()) {
			result = tx.doQuery(query);
		}
		assertEquals(0, result.size());
	}

	@Test
	public void shouldQueryCaseInsensitiveString() {

		StrolchAgent agent = ComponentContainerTest.startContainer(PATH_EMPTY_RUNTIME,
				ComponentContainerTest.PATH_EMPTY_CONTAINER);
		ComponentContainer container = agent.getContainer();

		Resource res1 = createResource("@1", "Test Resource", "MyType");
		try (StrolchTransaction tx = container.getDefaultRealm().openTx()) {
			tx.getResourceMap().add(tx, res1);
		}

		ResourceQuery query = new ResourceQuery(new StrolchTypeNavigation("MyType"));
		query.and().with(ParameterSelection.stringSelection(BAG_ID, PARAM_STRING_ID, "strolch").caseInsensitive(true));
		List<Resource> result;
		try (StrolchTransaction tx = container.getDefaultRealm().openTx()) {
			result = tx.doQuery(query);
		}
		assertEquals(1, result.size());
		assertEquals("@1", result.get(0).getId());
	}

	@Test
	public void shouldNotQueryCaseInsensitiveString() {

		StrolchAgent agent = ComponentContainerTest.startContainer(PATH_EMPTY_RUNTIME,
				ComponentContainerTest.PATH_EMPTY_CONTAINER);
		ComponentContainer container = agent.getContainer();

		Resource res1 = createResource("@1", "Test Resource", "MyType");
		try (StrolchTransaction tx = container.getDefaultRealm().openTx()) {
			tx.getResourceMap().add(tx, res1);
		}

		ResourceQuery query = new ResourceQuery(new StrolchTypeNavigation("MyType"));
		query.and().with(ParameterSelection.stringSelection(BAG_ID, PARAM_STRING_ID, "strolch"));
		List<Resource> result;
		try (StrolchTransaction tx = container.getDefaultRealm().openTx()) {
			result = tx.doQuery(query);
		}
		assertEquals(0, result.size());
	}

	@Test
	public void shouldQueryNot() {

		StrolchAgent agent = ComponentContainerTest.startContainer(PATH_EMPTY_RUNTIME,
				ComponentContainerTest.PATH_EMPTY_CONTAINER);
		ComponentContainer container = agent.getContainer();

		Resource res1 = createResource("@1", "Test Resource", "MyType");
		Resource res2 = createResource("@2", "Test Resource", "MyType");
		try (StrolchTransaction tx = container.getDefaultRealm().openTx()) {
			tx.getResourceMap().add(tx, res1);
			tx.getResourceMap().add(tx, res2);
		}

		{
			ResourceQuery query = new ResourceQuery(new StrolchTypeNavigation("MyType"));
			query.not(new IdSelection("@1"));
			List<Resource> result;
			try (StrolchTransaction tx = container.getDefaultRealm().openTx()) {
				result = tx.doQuery(query);
			}
			assertEquals(1, result.size());
			assertEquals("@2", result.get(0).getId());
		}

		{
			ResourceQuery query = new ResourceQuery(new StrolchTypeNavigation("MyType"));
			query.not(new IdSelection("@2"));
			List<Resource> result;
			try (StrolchTransaction tx = container.getDefaultRealm().openTx()) {
				result = tx.doQuery(query);
			}
			assertEquals(1, result.size());
			assertEquals("@1", result.get(0).getId());
		}

		{
			ResourceQuery query = new ResourceQuery(new StrolchTypeNavigation("MyType"));
			query.not(new IdSelection("@1", "@2"));
			List<Resource> result;
			try (StrolchTransaction tx = container.getDefaultRealm().openTx()) {
				result = tx.doQuery(query);
			}
			assertEquals(0, result.size());
		}
	}
}
