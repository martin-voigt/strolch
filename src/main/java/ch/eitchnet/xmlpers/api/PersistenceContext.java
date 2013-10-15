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
package ch.eitchnet.xmlpers.api;

import ch.eitchnet.xmlpers.objref.ObjectRef;

public class PersistenceContext<T> {

	private final ObjectRef objectRef;
	private T object;
	private ParserFactory<T> parserFactory;

	public PersistenceContext(ObjectRef objectRef) {
		this.objectRef = objectRef;
	}

	public ObjectRef getObjectRef() {
		return this.objectRef;
	}

	public T getObject() {
		return this.object;
	}

	public void setObject(T object) {
		this.object = object;
	}

	public ParserFactory<T> getParserFactor() {
		return this.parserFactory;
	}

	public void setParserFactory(ParserFactory<T> parserFactory) {
		this.parserFactory = parserFactory;
	}
}