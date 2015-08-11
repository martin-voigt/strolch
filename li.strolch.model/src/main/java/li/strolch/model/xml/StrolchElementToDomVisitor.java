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
package li.strolch.model.xml;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.SortedSet;

import li.strolch.model.AbstractStrolchElement;
import li.strolch.model.GroupedParameterizedElement;
import li.strolch.model.Order;
import li.strolch.model.ParameterBag;
import li.strolch.model.ParameterizedElement;
import li.strolch.model.Resource;
import li.strolch.model.StrolchModelConstants;
import li.strolch.model.Tags;
import li.strolch.model.activity.Action;
import li.strolch.model.activity.Activity;
import li.strolch.model.activity.IActivityElement;
import li.strolch.model.parameter.Parameter;
import li.strolch.model.timedstate.StrolchTimedState;
import li.strolch.model.timevalue.ITimeValue;
import li.strolch.model.timevalue.IValue;
import li.strolch.model.timevalue.IValueChange;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import ch.eitchnet.utils.iso8601.ISO8601FormatFactory;

/**
 * @author Robert von Burg <eitch@eitchnet.ch>
 */
public class StrolchElementToDomVisitor {

	protected Document document;

	public Document getDocument() {
		return this.document;
	}

	protected Element toDom(Order order) {

		Element asDom = document.createElement(Tags.ORDER);
		fillElement(asDom, order);

		asDom.setAttribute(Tags.DATE, ISO8601FormatFactory.getInstance().formatDate(order.getDate()));
		asDom.setAttribute(Tags.STATE, order.getState().name());

		return asDom;
	}

	protected Element toDom(Resource resource) {

		Element asDom = document.createElement(Tags.RESOURCE);
		fillElement(asDom, resource);

		if (resource.hasTimedStates()) {
			for (String stateKey : resource.getTimedStateKeySet()) {
				StrolchTimedState<IValue<?>> timedState = resource.getTimedState(stateKey);
				Element stateElement = toDom(timedState);
				asDom.appendChild(stateElement);
			}
		}

		return asDom;
	}

	protected Element toDom(Activity activity) {
		Element element = document.createElement(Tags.ACTIVITY);
		fillElement(element, activity);

		if (activity.hasElements()) {
			Iterator<Entry<String, IActivityElement>> iter = activity.elementIterator();
			while (iter.hasNext()) {
				IActivityElement activityElement = iter.next().getValue();
				if (activityElement instanceof Activity) {
					element.appendChild(toDom((Activity) activityElement));
				} else if (activityElement instanceof Action) {
					element.appendChild(toDom((Action) activityElement));
				} else {
					throw new IllegalArgumentException("Unhandled element " + activityElement.getClass());
				}
			}
		}

		return element;
	}

	protected Element toDom(Action action) {
		Element element = document.createElement(Tags.ACTION);
		fillElement(element, action);

		element.setAttribute(Tags.RESOURCE_ID, action.getResourceId());
		element.setAttribute(Tags.RESOURCE_TYPE, action.getResourceType());
		element.setAttribute(Tags.STATE, action.getState().name());

		if (action.hasChanges()) {
			Iterator<IValueChange<? extends IValue<?>>> iter = action.changesIterator();
			while (iter.hasNext()) {
				IValueChange<? extends IValue<?>> value = iter.next();
				Element valueChangeElement = toDom(value);
				element.appendChild(valueChangeElement);
			}
		}

		return element;
	}

	protected Element toDom(IValueChange<? extends IValue<?>> value) {
		Element element = document.createElement(Tags.VALUE_CHANGE);
		element.setAttribute(Tags.STATE_ID, value.getStateId());
		element.setAttribute(Tags.TIME, ISO8601FormatFactory.getInstance().formatDate(value.getTime()));
		element.setAttribute(Tags.VALUE, value.getValue().getValueAsString());
		element.setAttribute(Tags.TYPE, value.getValue().getType());
		return element;
	}

	protected Element toDom(StrolchTimedState<IValue<?>> timedState) {

		Element element = document.createElement(Tags.TIMED_STATE);
		fillElement(element, (AbstractStrolchElement) timedState);

		if (!timedState.getInterpretation().equals(StrolchModelConstants.INTERPRETATION_NONE)) {
			element.setAttribute(Tags.INTERPRETATION, timedState.getInterpretation());
		}
		if (!timedState.getUom().equals(StrolchModelConstants.UOM_NONE)) {
			element.setAttribute(Tags.UOM, timedState.getUom());
		}
		if (timedState.isHidden()) {
			element.setAttribute(Tags.HIDDEN, Boolean.toString(timedState.isHidden()));
		}
		if (timedState.getIndex() != 0) {
			element.setAttribute(Tags.INDEX, Integer.toString(timedState.getIndex()));
		}

		SortedSet<ITimeValue<IValue<?>>> values = timedState.getTimeEvolution().getValues();
		for (ITimeValue<IValue<?>> iTimeValue : values) {

			Long time = iTimeValue.getTime();
			String valueS = iTimeValue.getValue().getValueAsString();

			Element valueElement = document.createElement(Tags.VALUE);
			valueElement.setAttribute(Tags.TIME, ISO8601FormatFactory.getInstance().formatDate(time));
			valueElement.setAttribute(Tags.VALUE, valueS);

			element.appendChild(valueElement);
		}

		return element;
	}

	protected Element toDom(ParameterBag bag) {
		Element bagElement = document.createElement(Tags.PARAMETER_BAG);
		fillElement(bagElement, (ParameterizedElement) bag);
		return bagElement;
	}

	protected Element toDom(Parameter<?> param) {
		Element element = document.createElement(Tags.PARAMETER);
		fillElement(element, (AbstractStrolchElement) param);

		element.setAttribute(Tags.VALUE, param.getValueAsString());

		if (!param.getInterpretation().equals(StrolchModelConstants.INTERPRETATION_NONE)) {
			element.setAttribute(Tags.INTERPRETATION, param.getInterpretation());
		}
		if (!param.getUom().equals(StrolchModelConstants.UOM_NONE)) {
			element.setAttribute(Tags.UOM, param.getUom());
		}
		if (param.isHidden()) {
			element.setAttribute(Tags.HIDDEN, Boolean.toString(param.isHidden()));
		}
		if (param.getIndex() != 0) {
			element.setAttribute(Tags.INDEX, Integer.toString(param.getIndex()));
		}

		return element;
	}

	protected void fillElement(Element element, AbstractStrolchElement strolchElement) {
		element.setAttribute(Tags.ID, strolchElement.getId());
		element.setAttribute(Tags.NAME, strolchElement.getName());
		element.setAttribute(Tags.TYPE, strolchElement.getType());
	}

	protected void fillElement(Element element, GroupedParameterizedElement groupedParameterizedElement) {
		fillElement(element, (AbstractStrolchElement) groupedParameterizedElement);

		if (groupedParameterizedElement.hasParameterBags()) {
			for (String bagKey : groupedParameterizedElement.getParameterBagKeySet()) {
				ParameterBag bag = groupedParameterizedElement.getParameterBag(bagKey);
				Element bagElement = toDom(bag);
				element.appendChild(bagElement);
			}
		}
	}

	protected void fillElement(Element element, ParameterizedElement parameterizedElement) {
		fillElement(element, (AbstractStrolchElement) parameterizedElement);

		if (parameterizedElement.hasParameters()) {
			for (String paramKey : parameterizedElement.getParameterKeySet()) {
				Parameter<?> parameter = parameterizedElement.getParameter(paramKey);
				Element paramElement = toDom(parameter);
				element.appendChild(paramElement);
			}
		}
	}

}