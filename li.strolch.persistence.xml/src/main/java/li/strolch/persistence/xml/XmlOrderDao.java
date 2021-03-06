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
package li.strolch.persistence.xml;

import java.util.List;

import li.strolch.model.Order;
import li.strolch.model.Tags;
import li.strolch.model.query.OrderQuery;
import li.strolch.persistence.api.OrderDao;
import li.strolch.persistence.api.StrolchTransaction;

public class XmlOrderDao extends AbstractDao<Order> implements OrderDao {

	protected XmlOrderDao(StrolchTransaction tx) {
		super(tx);
	}

	@Override
	protected String getClassType() {
		return Tags.ORDER;
	}

	@Override
	public <U> List<U> doQuery(OrderQuery<U> query) {
		// TODO implement XML file based querying...
		throw new UnsupportedOperationException("not yet implemented!");
	}
}
