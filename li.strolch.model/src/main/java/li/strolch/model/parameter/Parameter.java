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
package li.strolch.model.parameter;

import li.strolch.model.ParameterizedElement;
import li.strolch.model.StrolchElement;
import li.strolch.model.StrolchModelConstants;
import li.strolch.model.visitor.ParameterVisitor;

/**
 * @author Robert von Burg <eitch@eitchnet.ch>
 */
public interface Parameter<T> extends StrolchElement, Comparable<Parameter<?>> {

	/**
	 * Returns the value of the parameter as string
	 * 
	 * @return the value as string
	 */
	public String getValueAsString();

	/**
	 * Set the value of the parameter from a string
	 * 
	 * @param valueAsString
	 *            the string from which to set the value
	 */
	public void setValueFromString(String valueAsString);

	/**
	 * the value of the parameter
	 * 
	 * @return
	 */
	public T getValue();

	/**
	 * the value of the parameter
	 * 
	 * @param value
	 */
	public void setValue(T value);

	/**
	 * get the hidden attribute
	 * 
	 * @return
	 */
	public boolean isHidden();

	/**
	 * set the hidden attribute
	 * 
	 * @param hidden
	 */
	public void setHidden(boolean hidden);

	/**
	 * Get the UOM of this {@link Parameter}
	 * 
	 * @return
	 */
	public String getUom();

	/**
	 * Set the UOM of this {@link Parameter}
	 * 
	 * @param uom
	 */
	public void setUom(String uom);

	/**
	 * Returns the index of this {@link Parameter}. This can be used to sort the parameters in a UI
	 * 
	 * @return the index of this {@link Parameter}. This can be used to sort the parameters in a UI
	 */
	public int getIndex();

	/**
	 * Set the index of this {@link Parameter}. This can be used to sort the parameters in a UI
	 * 
	 * @param index
	 *            the index to set
	 */
	public void setIndex(int index);

	/**
	 * Returns the interpretation of this {@link Parameter}. The interpretation semantic describes what the value of
	 * this {@link Parameter} means. Currently there are three definitions, but any String value can be used:
	 * <ul>
	 * <li>{@link StrolchModelConstants#INTERPRETATION_NONE}</li>
	 * <li>{@link StrolchModelConstants#INTERPRETATION_ORDER_REF}</li>
	 * <li>{@link StrolchModelConstants#INTERPRETATION_RESOURCE_REF}</li>
	 * </ul>
	 * 
	 * @return string value
	 */
	public String getInterpretation();

	/**
	 * Set the interpretation of this {@link Parameter}. The interpretation semantic describes what the value of this
	 * {@link Parameter} means. Currently there are three definitions, but any String value can be used:
	 * <ul>
	 * <li>{@link StrolchModelConstants#INTERPRETATION_NONE}</li>
	 * <li>{@link StrolchModelConstants#INTERPRETATION_ORDER_REF}</li>
	 * <li>{@link StrolchModelConstants#INTERPRETATION_RESOURCE_REF}</li>
	 * </ul>
	 * 
	 * @param interpretation
	 */
	public void setInterpretation(String interpretation);

	/**
	 * The {@link ParameterizedElement} parent to which this {@link Parameter} belongs
	 * 
	 * @return
	 */
	@Override
	public ParameterizedElement getParent();

	/**
	 * Sets the parent for this {@link Parameter}
	 */
	public void setParent(ParameterizedElement parent);

	@Override
	public int hashCode();

	@Override
	public boolean equals(Object obj);
	
	@Override
	public int compareTo(Parameter<?> o);

	@Override
	public Parameter<T> getClone();

	public <U> U accept(ParameterVisitor visitor);	
}
