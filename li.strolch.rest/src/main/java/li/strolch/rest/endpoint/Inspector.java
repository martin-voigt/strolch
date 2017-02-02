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
package li.strolch.rest.endpoint;

import java.io.StringReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.InputSource;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import li.strolch.agent.api.ActivityMap;
import li.strolch.agent.api.ComponentContainer;
import li.strolch.agent.api.OrderMap;
import li.strolch.agent.api.ResourceMap;
import li.strolch.exception.StrolchException;
import li.strolch.model.Order;
import li.strolch.model.Resource;
import li.strolch.model.Tags;
import li.strolch.model.activity.Activity;
import li.strolch.model.json.OrderToJsonVisitor;
import li.strolch.model.json.ResourceToJsonVisitor;
import li.strolch.model.xml.OrderToXmlStringVisitor;
import li.strolch.model.xml.ResourceToXmlStringVisitor;
import li.strolch.model.xml.SimpleStrolchElementListener;
import li.strolch.model.xml.XmlModelSaxReader;
import li.strolch.persistence.api.StrolchPersistenceException;
import li.strolch.persistence.api.StrolchTransaction;
import li.strolch.privilege.model.Certificate;
import li.strolch.rest.RestfulStrolchComponent;
import li.strolch.rest.StrolchRestfulConstants;
import li.strolch.rest.model.Result;
import li.strolch.service.UpdateOrderService;
import li.strolch.service.UpdateOrderService.UpdateOrderArg;
import li.strolch.service.UpdateResourceService;
import li.strolch.service.UpdateResourceService.UpdateResourceArg;
import li.strolch.service.api.ServiceResult;
import li.strolch.utils.iso8601.ISO8601FormatFactory;

/**
 * @author Robert von Burg <eitch@eitchnet.ch>
 */
@Path("strolch/inspector")
public class Inspector {

	private StrolchTransaction openTx(Certificate certificate, String realm) {
		return RestfulStrolchComponent.getInstance().getContainer().getRealm(realm).openTx(certificate,
				Inspector.class);
	}

	/**
	 * <p>
	 * Root path of the inspector
	 * </p>
	 * 
	 * <p>
	 * Returns the root element, which is an overview of the configured realms
	 * </p>
	 * 
	 * @return the root element, which is an overview of the configured realms
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAgent(@Context HttpServletRequest request) {
		try {

			Certificate cert = (Certificate) request.getAttribute(StrolchRestfulConstants.STROLCH_CERTIFICATE);

			JsonObject agentOverview = new JsonObject();
			JsonArray realmsArr = new JsonArray();
			agentOverview.add(Tags.Json.REALMS, realmsArr);

			ComponentContainer container = RestfulStrolchComponent.getInstance().getContainer();
			Set<String> realmNames = container.getRealmNames();
			for (String realmName : realmNames) {

				JsonObject realmJ = new JsonObject();

				try (StrolchTransaction tx = openTx(cert, realmName)) {
					long size = 0;
					size += tx.getResourceMap().querySize(tx);
					size += tx.getOrderMap().querySize(tx);

					realmJ.addProperty(Tags.Json.NAME, realmName);
					realmJ.addProperty(Tags.Json.SIZE, size);

					realmsArr.add(realmJ);
				}
			}

			return Response.ok().entity(agentOverview.toString()).build();

		} catch (Exception e) {
			throw e;
		}
	}

	/**
	 * <p>
	 * Realm inspector
	 * </p>
	 * 
	 * <p>
	 * Returns the overview of a specific relam
	 * </p>
	 * 
	 * @param realm
	 *            the realm for which the overview is to be returned
	 * 
	 * @return the overview of a specific relam
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("{realm}")
	public Response getRealm(@PathParam("realm") String realm, @Context HttpServletRequest request) {

		Certificate cert = (Certificate) request.getAttribute(StrolchRestfulConstants.STROLCH_CERTIFICATE);

		JsonObject realmDetailJ = new JsonObject();
		JsonArray elementMapsArr = new JsonArray();
		realmDetailJ.add(Tags.Json.ELEMENT_MAPS, elementMapsArr);

		try (StrolchTransaction tx = openTx(cert, realm)) {

			{
				ResourceMap resourceMap = tx.getResourceMap();
				JsonObject elementMapJ = new JsonObject();
				elementMapJ.addProperty(Tags.Json.OBJECT_TYPE, Tags.Json.RESOURCE);
				elementMapJ.addProperty(Tags.Json.NR_OF_ELEMENTS, resourceMap.querySize(tx));
				JsonArray typesJ = new JsonArray();
				resourceMap.getTypes(tx).forEach(type -> typesJ.add(new JsonPrimitive(type)));
				elementMapJ.add(Tags.Json.TYPES, typesJ);

				elementMapsArr.add(elementMapJ);
			}

			{
				OrderMap orderMap = tx.getOrderMap();
				JsonObject elementMapJ = new JsonObject();
				elementMapJ.addProperty(Tags.Json.OBJECT_TYPE, Tags.Json.ORDER);
				elementMapJ.addProperty(Tags.Json.NR_OF_ELEMENTS, orderMap.querySize(tx));
				JsonArray typesJ = new JsonArray();
				orderMap.getTypes(tx).forEach(type -> typesJ.add(new JsonPrimitive(type)));
				elementMapJ.add(Tags.Json.TYPES, typesJ);

				elementMapsArr.add(elementMapJ);
			}

			{
				ActivityMap activityMap = tx.getActivityMap();
				JsonObject elementMapJ = new JsonObject();
				elementMapJ.addProperty(Tags.Json.OBJECT_TYPE, Tags.Json.ACTIVITY);
				elementMapJ.addProperty(Tags.Json.NR_OF_ELEMENTS, activityMap.querySize(tx));
				JsonArray typesJ = new JsonArray();
				activityMap.getTypes(tx).forEach(type -> typesJ.add(new JsonPrimitive(type)));
				elementMapJ.add(Tags.Json.TYPES, typesJ);

				elementMapsArr.add(elementMapJ);
			}
		}

		return Response.ok().entity(realmDetailJ.toString()).build();
	}

	/**
	 * <p>
	 * Resource inspector
	 * </p>
	 * <p>
	 * Returns an overview of the {@link Resource Resources}. This is a list of all the types and the size each type has
	 * </p>
	 * 
	 * @param realm
	 *            the realm for which the resource overview is to be returned
	 * 
	 * @return an overview of the {@link Resource Resources}. This is a list of all the types and the size each type has
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("{realm}/resources")
	public Response getResourcesOverview(@PathParam("realm") String realm, @Context HttpServletRequest request) {

		Certificate cert = (Certificate) request.getAttribute(StrolchRestfulConstants.STROLCH_CERTIFICATE);

		JsonObject mapOverview = new JsonObject();

		try (StrolchTransaction tx = openTx(cert, realm)) {
			ResourceMap resourceMap = tx.getResourceMap();

			mapOverview.addProperty(Tags.Json.OBJECT_TYPE, Tags.Json.RESOURCE);
			mapOverview.addProperty(Tags.Json.SIZE, resourceMap.querySize(tx));

			JsonArray typeArrJ = new JsonArray();
			mapOverview.add(Tags.Json.TYPES, typeArrJ);

			List<String> types = new ArrayList<>();
			Collections.sort(types);
			resourceMap.getTypes(tx).forEach(type -> {

				JsonObject typeJ = new JsonObject();
				typeJ.addProperty(Tags.Json.TYPE, type);
				typeJ.addProperty(Tags.Json.SIZE, resourceMap.querySize(tx, type));

				typeArrJ.add(typeJ);
			});
		}

		return Response.ok().entity(mapOverview.toString()).build();
	}

	/**
	 * <p>
	 * Order inspector
	 * </p>
	 * <p>
	 * Returns an overview of the {@link Order Orders}. This is a list of all the types and the size each type has
	 * </p>
	 * 
	 * @param realm
	 *            the realm for which the order overview is to be returned
	 * 
	 * @return an overview of the {@link Order Orders}. This is a list of all the types and the size each type has
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("{realm}/orders")
	public Response getOrdersOverview(@PathParam("realm") String realm, @Context HttpServletRequest request) {

		Certificate cert = (Certificate) request.getAttribute(StrolchRestfulConstants.STROLCH_CERTIFICATE);

		JsonObject mapOverview = new JsonObject();

		try (StrolchTransaction tx = openTx(cert, realm)) {
			OrderMap orderMap = tx.getOrderMap();

			mapOverview.addProperty(Tags.Json.OBJECT_TYPE, Tags.Json.ORDER);
			mapOverview.addProperty(Tags.Json.SIZE, orderMap.querySize(tx));

			JsonArray typeArrJ = new JsonArray();
			mapOverview.add(Tags.Json.TYPES, typeArrJ);

			List<String> types = new ArrayList<>();
			Collections.sort(types);
			orderMap.getTypes(tx).forEach(type -> {

				JsonObject typeJ = new JsonObject();
				typeJ.addProperty(Tags.Json.TYPE, type);
				typeJ.addProperty(Tags.Json.SIZE, orderMap.querySize(tx, type));

				typeArrJ.add(typeJ);
			});
		}

		return Response.ok().entity(mapOverview.toString()).build();
	}

	/**
	 * <p>
	 * Activity inspector
	 * </p>
	 * <p>
	 * Returns an overview of the {@link Activity Activities}. This is a list of all the types and the size each type
	 * has
	 * </p>
	 * 
	 * @param realm
	 *            the realm for which the activity overview is to be returned
	 * 
	 * @return an overview of the {@link Activity Activities}. This is a list of all the types and the size each type
	 *         has
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("{realm}/activities")
	public Response getActivitiesOverview(@PathParam("realm") String realm, @Context HttpServletRequest request) {

		Certificate cert = (Certificate) request.getAttribute(StrolchRestfulConstants.STROLCH_CERTIFICATE);

		JsonObject mapOverview = new JsonObject();

		try (StrolchTransaction tx = openTx(cert, realm)) {
			ActivityMap activityMap = tx.getActivityMap();

			mapOverview.addProperty(Tags.Json.OBJECT_TYPE, Tags.Json.ACTIVITY);
			mapOverview.addProperty(Tags.Json.SIZE, activityMap.querySize(tx));

			JsonArray typeArrJ = new JsonArray();
			mapOverview.add(Tags.Json.TYPES, typeArrJ);

			List<String> types = new ArrayList<>();
			Collections.sort(types);
			activityMap.getTypes(tx).forEach(type -> {

				JsonObject typeJ = new JsonObject();
				typeJ.addProperty(Tags.Json.TYPE, type);
				typeJ.addProperty(Tags.Json.SIZE, activityMap.querySize(tx, type));

				typeArrJ.add(typeJ);
			});
		}

		return Response.ok().entity(mapOverview.toString()).build();
	}

	// TODO for the get element type details, we should not simply query all objects, but rather find a solution to query only the id, name, type and date, state for the order

	/**
	 * <p>
	 * Resource type inspector
	 * </p>
	 * <p>
	 * Returns an overview of the {@link Resource Resources} with the given type. This is a list of overviews of the
	 * resources
	 * </p>
	 * 
	 * @param realm
	 *            the realm for which the resource type overview is to be returned
	 * @param type
	 * 
	 * @return an overview of the {@link Resource Resources} with the given type. This is a list of overviews of the
	 *         resources
	 * 
	 * @see TypeDetail
	 * @see StrolchElementOverview
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("{realm}/resources/{type}")
	public Response getResourceTypeDetails(@PathParam("realm") String realm, @PathParam("type") String type,
			@Context HttpServletRequest request) {

		Certificate cert = (Certificate) request.getAttribute(StrolchRestfulConstants.STROLCH_CERTIFICATE);

		JsonObject typeDetailJ = new JsonObject();
		typeDetailJ.addProperty(Tags.Json.OBJECT_TYPE, Tags.Json.RESOURCE);
		typeDetailJ.addProperty(Tags.Json.TYPE, type);

		JsonArray elementsJ = new JsonArray();
		typeDetailJ.add(Tags.Json.ELEMENTS, elementsJ);

		try (StrolchTransaction tx = openTx(cert, realm)) {

			List<Resource> byType = tx.getResourceMap().getElementsBy(tx, type);
			for (Resource resource : byType) {

				JsonObject elementJ = new JsonObject();
				elementJ.addProperty(Tags.Json.ID, resource.getId());
				elementJ.addProperty(Tags.Json.NAME, resource.getName());
				elementJ.addProperty(Tags.Json.TYPE, resource.getType());

				elementsJ.add(elementJ);
			}
		}

		return Response.ok().entity(typeDetailJ.toString()).build();
	}

	/**
	 * <p>
	 * Order type inspector
	 * </p>
	 * <p>
	 * Returns an overview of the {@link Order Orders} with the given type. This is a list of overviews of the orders
	 * </p>
	 * 
	 * @param realm
	 *            the realm for which the order type overview is to be returned
	 * @param type
	 * 
	 * @return an overview of the {@link Order Orders} with the given type. This is a list of overviews of the orders
	 * 
	 * @see TypeDetail
	 * @see StrolchElementOverview
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("{realm}/orders/{type}")
	public Response getOrderTypeDetails(@PathParam("realm") String realm, @PathParam("type") String type,
			@Context HttpServletRequest request) {

		Certificate cert = (Certificate) request.getAttribute(StrolchRestfulConstants.STROLCH_CERTIFICATE);

		JsonObject typeDetailJ = new JsonObject();
		typeDetailJ.addProperty(Tags.Json.OBJECT_TYPE, Tags.Json.ORDER);
		typeDetailJ.addProperty(Tags.Json.TYPE, type);

		JsonArray elementsJ = new JsonArray();
		typeDetailJ.add(Tags.Json.ELEMENTS, elementsJ);

		try (StrolchTransaction tx = openTx(cert, realm)) {

			List<Order> byType = tx.getOrderMap().getElementsBy(tx, type);
			for (Order order : byType) {

				JsonObject elementJ = new JsonObject();
				elementJ.addProperty(Tags.Json.ID, order.getId());
				elementJ.addProperty(Tags.Json.NAME, order.getName());
				elementJ.addProperty(Tags.Json.TYPE, order.getType());
				elementJ.addProperty(Tags.Json.STATE, order.getState().getName());
				elementJ.addProperty(Tags.Json.DATE, ISO8601FormatFactory.getInstance().formatDate(order.getDate()));

				elementsJ.add(elementJ);
			}
		}

		return Response.ok().entity(typeDetailJ.toString()).build();
	}

	/**
	 * <p>
	 * Order type inspector
	 * </p>
	 * <p>
	 * Returns an overview of the {@link Order Orders} with the given type. This is a list of overviews of the orders
	 * </p>
	 * 
	 * @param realm
	 *            the realm for which the order type overview is to be returned
	 * @param type
	 * 
	 * @return an overview of the {@link Order Orders} with the given type. This is a list of overviews of the orders
	 * 
	 * @see TypeDetail
	 * @see StrolchElementOverview
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("{realm}/activities/{type}")
	public Response getActivities(@PathParam("realm") String realm, @PathParam("type") String type,
			@Context HttpServletRequest request) {

		Certificate cert = (Certificate) request.getAttribute(StrolchRestfulConstants.STROLCH_CERTIFICATE);

		JsonObject typeDetailJ = new JsonObject();
		typeDetailJ.addProperty(Tags.Json.OBJECT_TYPE, Tags.Json.ACTIVITY);
		typeDetailJ.addProperty(Tags.Json.TYPE, type);

		JsonArray elementsJ = new JsonArray();
		typeDetailJ.add(Tags.Json.ELEMENTS, elementsJ);

		try (StrolchTransaction tx = openTx(cert, realm)) {

			List<Activity> byType = tx.getActivityMap().getElementsBy(tx, type);
			for (Activity activity : byType) {

				JsonObject elementJ = new JsonObject();
				elementJ.addProperty(Tags.Json.ID, activity.getId());
				elementJ.addProperty(Tags.Json.NAME, activity.getName());
				elementJ.addProperty(Tags.Json.TYPE, activity.getType());
				elementJ.addProperty(Tags.Json.STATE, activity.getState().getName());
				elementJ.addProperty(Tags.Json.TIME_ORDERING, activity.getTimeOrdering().getName());

				elementsJ.add(elementJ);
			}
		}

		return Response.ok().entity(typeDetailJ.toString()).build();
	}

	/**
	 * <p>
	 * Resource inspector
	 * </p>
	 * 
	 * <p>
	 * Returns the resource with the given id
	 * </p>
	 * 
	 * @param realm
	 *            the realm for which the resource is to be returned
	 * @param type
	 *            the type of the resource
	 * @param id
	 *            the id of the resource
	 * 
	 * @return the resource with the given id
	 * 
	 * @see ResourceDetail
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("{realm}/resources/{type}/{id}")
	public Response getResourceAsJson(@PathParam("realm") String realm, @PathParam("type") String type,
			@PathParam("id") String id, @Context HttpServletRequest request) {

		Certificate cert = (Certificate) request.getAttribute(StrolchRestfulConstants.STROLCH_CERTIFICATE);

		Resource resource;
		try (StrolchTransaction tx = openTx(cert, realm)) {
			resource = tx.getResourceMap().getBy(tx, type, id);
		}
		if (resource == null) {
			throw new StrolchException(MessageFormat.format("No Resource exists for {0}/{1}", type, id)); //$NON-NLS-1$
		}

		return Response.ok().entity(ResourceToJsonVisitor.toJsonString(resource)).build();
	}

	@GET
	@Produces(MediaType.APPLICATION_XML)
	@Path("{realm}/Resource/{type}/{id}")
	public Response getResourceAsXml(@PathParam("realm") String realm, @PathParam("type") String type,
			@PathParam("id") String id, @Context HttpServletRequest request) {

		Certificate cert = (Certificate) request.getAttribute(StrolchRestfulConstants.STROLCH_CERTIFICATE);

		Resource resource;
		try (StrolchTransaction tx = openTx(cert, realm)) {
			resource = tx.getResourceMap().getBy(tx, type, id);
		}
		if (resource == null) {
			throw new StrolchException(MessageFormat.format("No Resource exists for {0}/{1}", type, id)); //$NON-NLS-1$
		}

		String asXml = new ResourceToXmlStringVisitor().visit(resource);
		return Response.ok().type(MediaType.APPLICATION_XML).entity(asXml).build();
	}

	@PUT
	@Produces(MediaType.APPLICATION_XML)
	@Consumes(MediaType.APPLICATION_XML)
	@Path("{realm}/resources/{type}/{id}")
	public Response updateResourceAsXml(@PathParam("realm") String realm, @PathParam("type") String type,
			@PathParam("id") String id, String data, @Context HttpServletRequest request) {

		Certificate cert = (Certificate) request.getAttribute(StrolchRestfulConstants.STROLCH_CERTIFICATE);

		Resource resource;
		try {
			SimpleStrolchElementListener listener = new SimpleStrolchElementListener();
			SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
			parser.parse(new InputSource(new StringReader(data)), new XmlModelSaxReader(listener));

			if (listener.getResources().size() == 0)
				throw new StrolchPersistenceException(
						MessageFormat.format("No Resources parsed from xml value for {0} / {1}", id, type));
			if (listener.getResources().size() > 1)
				throw new StrolchPersistenceException(
						MessageFormat.format("Multiple Resources parsed from xml value for {0} / {1}", id, type));

			resource = listener.getResources().get(0);

		} catch (Exception e) {
			throw new StrolchPersistenceException(
					MessageFormat.format("Failed to extract Resources from xml value for {0} / {1}", id, type), e);
		}

		UpdateResourceService svc = new UpdateResourceService();
		UpdateResourceArg arg = new UpdateResourceArg();
		arg.resource = resource;
		arg.realm = realm;

		ServiceResult result = RestfulStrolchComponent.getInstance().getServiceHandler().doService(cert, svc, arg);
		if (result.isOk()) {
			String asXml = new ResourceToXmlStringVisitor().visit(resource);
			return Response.ok().type(MediaType.APPLICATION_XML).entity(asXml).build();
		}

		return Result.toResponse(result);
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("{realm}/orders/{type}/{id}")
	public Response getOrderAsJson(@PathParam("realm") String realm, @PathParam("type") String type,
			@PathParam("id") String id, @Context HttpServletRequest request) {

		Certificate cert = (Certificate) request.getAttribute(StrolchRestfulConstants.STROLCH_CERTIFICATE);

		Order order;
		try (StrolchTransaction tx = openTx(cert, realm)) {
			order = tx.getOrderMap().getBy(tx, type, id);
		}
		if (order == null) {
			throw new StrolchException(MessageFormat.format("No Order exists for {0}/{1}", type, id)); //$NON-NLS-1$
		}

		return Response.ok().entity(OrderToJsonVisitor.toJsonString(order)).build();
	}

	@GET
	@Produces(MediaType.APPLICATION_XML)
	@Path("{realm}/orders/{type}/{id}")
	public Response getOrderAsXml(@PathParam("realm") String realm, @PathParam("type") String type,
			@PathParam("id") String id, @Context HttpServletRequest request) {

		Certificate cert = (Certificate) request.getAttribute(StrolchRestfulConstants.STROLCH_CERTIFICATE);

		Order order;
		try (StrolchTransaction tx = openTx(cert, realm)) {
			order = tx.getOrderMap().getBy(tx, type, id);
		}
		if (order == null) {
			throw new StrolchException(MessageFormat.format("No Order exists for {0}/{1}", type, id)); //$NON-NLS-1$
		}

		String asXml = new OrderToXmlStringVisitor().visit(order);
		return Response.ok().type(MediaType.APPLICATION_XML).entity(asXml).build();
	}

	@PUT
	@Produces(MediaType.APPLICATION_XML)
	@Consumes(MediaType.APPLICATION_XML)
	@Path("{realm}/orders/{type}/{id}")
	public Response updateOrderAsXml(@PathParam("realm") String realm, @PathParam("type") String type,
			@PathParam("id") String id, String data, @Context HttpServletRequest request) {

		Certificate cert = (Certificate) request.getAttribute(StrolchRestfulConstants.STROLCH_CERTIFICATE);

		Order order;
		try {
			SimpleStrolchElementListener listener = new SimpleStrolchElementListener();
			SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
			parser.parse(new InputSource(new StringReader(data)), new XmlModelSaxReader(listener));

			if (listener.getOrders().size() == 0)
				throw new StrolchPersistenceException(
						MessageFormat.format("No Orders parsed from xml value for {0} / {1}", id, type));
			if (listener.getOrders().size() > 1)
				throw new StrolchPersistenceException(
						MessageFormat.format("Multiple Orders parsed from xml value for {0} / {1}", id, type));

			order = listener.getOrders().get(0);

		} catch (Exception e) {
			throw new StrolchPersistenceException(
					MessageFormat.format("Failed to extract Order from xml value for {0} / {1}", id, type), e);
		}

		UpdateOrderService svc = new UpdateOrderService();
		UpdateOrderArg arg = new UpdateOrderArg();
		arg.order = order;
		arg.realm = realm;

		ServiceResult result = RestfulStrolchComponent.getInstance().getServiceHandler().doService(cert, svc, arg);
		if (result.isOk()) {
			String asXml = new OrderToXmlStringVisitor().visit(order);
			return Response.ok().type(MediaType.APPLICATION_XML).entity(asXml).build();
		}

		return Result.toResponse(result);
	}
}
