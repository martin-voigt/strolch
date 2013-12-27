package li.strolch.runtime.agent.impl;

import li.strolch.model.Resource;
import li.strolch.model.query.ResourceQuery;
import li.strolch.persistence.api.PersistenceHandler;
import li.strolch.persistence.api.ResourceDao;
import li.strolch.persistence.api.StrolchTransaction;
import li.strolch.runtime.agent.api.ResourceMap;

public class TransactionalResourceMap extends TransactionalElementMap<Resource, ResourceQuery> implements ResourceMap {

	public TransactionalResourceMap(String realm, PersistenceHandler persistenceHandler) {
		super(realm, persistenceHandler);
	}

	@Override
	protected ResourceDao getDao(StrolchTransaction tx) {
		return tx.getResourceDao();
	}
}