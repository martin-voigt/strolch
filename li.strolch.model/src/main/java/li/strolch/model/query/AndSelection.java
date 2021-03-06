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
package li.strolch.model.query;

import java.util.List;

/**
 * @author Robert von Burg <eitch@eitchnet.ch>
 */
public class AndSelection extends BooleanSelection {

	public AndSelection() {
		super();
	}

	@SafeVarargs
	public AndSelection(Selection... selections) {
		super(selections);
	}

	/**
	 * @param selections
	 */
	public AndSelection(List<Selection> selections) {
		super(selections);
	}

	@Override
	public AndSelection with(Selection selection) {
		super.with(selection);
		return this;
	}

	@Override
	public AndSelection with(List<Selection> selections) {
		super.with(selections);
		return this;
	}

	@Override
	public AndSelection with(Selection... selections) {
		super.with(selections);
		return this;
	}

	@Override
	public void accept(QueryVisitor visitor) {
		visitor.visitAnd(this);
	}
}
