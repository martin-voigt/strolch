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
package li.strolch.runtime.query.inmemory;

import java.util.ArrayList;
import java.util.List;

import li.strolch.model.Resource;
import li.strolch.runtime.component.ComponentContainer;
import li.strolch.runtime.query.ResourceQuery;
import li.strolch.runtime.query.ResourceQueryVisitor;

/**
 * @author Robert von Burg <eitch@eitchnet.ch>
 * 
 */
public class InMemoryResourceQueryVisitor implements ResourceQueryVisitor {

	private ArrayList<Resource> result;
	
	public List<Resource> performQuery(ComponentContainer container, ResourceQuery query) {
		this.result = new ArrayList<>();

		query.accept(this);

		return this.result;
	}

	@Override
	public void visit(ResourceQuery resourceQuery) {
		// TODO Auto-generated method stub

	}

	
}