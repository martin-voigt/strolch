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
package ch.eitchnet.xmlpers.test;

import static ch.eitchnet.xmlpers.test.model.ModelBuilder.RES_ID;
import static ch.eitchnet.xmlpers.test.model.ModelBuilder.RES_TYPE;
import static ch.eitchnet.xmlpers.test.model.ModelBuilder.assertResource;
import static ch.eitchnet.xmlpers.test.model.ModelBuilder.assertResourceUpdated;
import static ch.eitchnet.xmlpers.test.model.ModelBuilder.createResource;
import static ch.eitchnet.xmlpers.test.model.ModelBuilder.updateResource;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.junit.BeforeClass;
import org.junit.Test;

import ch.eitchnet.xmlpers.api.IoMode;
import ch.eitchnet.xmlpers.api.ObjectDao;
import ch.eitchnet.xmlpers.api.PersistenceConstants;
import ch.eitchnet.xmlpers.api.PersistenceTransaction;
import ch.eitchnet.xmlpers.objref.IdOfSubTypeRef;
import ch.eitchnet.xmlpers.objref.SubTypeRef;
import ch.eitchnet.xmlpers.test.impl.TestConstants;
import ch.eitchnet.xmlpers.test.model.Resource;

/**
 * @author Robert von Burg <eitch@eitchnet.ch>
 * 
 */
public class ObjectDaoTest extends AbstractPersistenceTest {

	private static final String BASEPATH = "target/db/ObjectDaoTest/"; //$NON-NLS-1$

	@BeforeClass
	public static void beforeClass() {
		cleanPath(BASEPATH);
	}

	private void setup(IoMode ioMode) {
		Properties properties = new Properties();
		properties.setProperty(PersistenceConstants.PROP_BASEPATH, BASEPATH + ioMode.name());
		setup(properties);
	}

	@Test
	public void testCrudSax() {
		setup(IoMode.SAX);
		testCrud(IoMode.SAX);
	}

	@Test
	public void testCrudDom() {
		setup(IoMode.DOM);
		testCrud(IoMode.DOM);
	}

	private PersistenceTransaction freshTx(IoMode ioMode) {
		PersistenceTransaction tx = this.persistenceManager.openTx();
		tx.setIoMode(ioMode);
		return tx;
	}

	private void testCrud(IoMode ioMode) {

		ObjectDao objectDao;

		// create new resource
		Resource resource = createResource();
		try (PersistenceTransaction tx = freshTx(ioMode);) {
			objectDao = tx.getObjectDao();
			objectDao.add(resource);
		}

		// read resource
		try (PersistenceTransaction tx = freshTx(ioMode);) {
			IdOfSubTypeRef resRef = tx.getObjectRefCache().getIdOfSubTypeRef(TestConstants.TYPE_RES, RES_TYPE, RES_ID);
			objectDao = tx.getObjectDao();
			resource = objectDao.queryById(resRef);
			assertResource(resource);

			// modify resource
			updateResource(resource);
			objectDao.update(resource);
		}

		// read modified resource
		try (PersistenceTransaction tx = freshTx(ioMode);) {
			IdOfSubTypeRef resRef = tx.getObjectRefCache().getIdOfSubTypeRef(TestConstants.TYPE_RES, RES_TYPE, RES_ID);
			objectDao = tx.getObjectDao();
			resource = objectDao.queryById(resRef);
			assertResourceUpdated(resource);
		}

		// delete resource
		try (PersistenceTransaction tx = freshTx(ioMode);) {
			objectDao = tx.getObjectDao();
			objectDao.remove(resource);
		}

		// fail to read
		try (PersistenceTransaction tx = freshTx(ioMode);) {
			IdOfSubTypeRef resRef = tx.getObjectRefCache().getIdOfSubTypeRef(TestConstants.TYPE_RES, RES_TYPE, RES_ID);
			objectDao = tx.getObjectDao();
			resource = objectDao.queryById(resRef);
			assertNull(resource);

			// and create again
			resource = createResource();
			assertResource(resource);
			objectDao.add(resource);
		}
	}

	@Test
	public void testBulkSax() {
		setup(IoMode.SAX);
		IoMode ioMode = IoMode.SAX;
		testBulk(ioMode);
	}

	@Test
	public void testBulkDom() {
		setup(IoMode.DOM);
		IoMode ioMode = IoMode.DOM;
		testBulk(ioMode);
	}

	private void testBulk(IoMode ioMode) {

		// context
		String type = "testBulk" + ioMode.name(); //$NON-NLS-1$

		// create a list of resources
		List<Resource> resources = new ArrayList<>(10);
		for (int i = 0; i < 10; i++) {
			String id = RES_ID + "_" + i; //$NON-NLS-1$
			String name = "Bulk Test Object. " + i; //$NON-NLS-1$

			Resource resource = createResource(id, name, type);
			resources.add(resource);
		}

		ObjectDao objectDao;

		// save all
		try (PersistenceTransaction tx = freshTx(ioMode);) {
			objectDao = tx.getObjectDao();
			objectDao.addAll(resources);
			resources.clear();
		}

		// query all
		try (PersistenceTransaction tx = freshTx(ioMode);) {
			SubTypeRef subTypeRef = tx.getObjectRefCache().getSubTypeRef(TestConstants.TYPE_RES, type);
			objectDao = tx.getObjectDao();
			resources = objectDao.queryAll(subTypeRef);
			assertEquals("Expected to find 10 entries!", 10, resources.size()); //$NON-NLS-1$

			// delete them all
			objectDao.removeAll(resources);
		}

		// now query them again
		try (PersistenceTransaction tx = freshTx(ioMode);) {
			SubTypeRef subTypeRef = tx.getObjectRefCache().getSubTypeRef(TestConstants.TYPE_RES, type);
			objectDao = tx.getObjectDao();
			resources = objectDao.queryAll(subTypeRef);
			assertEquals("Expected to find 0 entries!", 0, resources.size()); //$NON-NLS-1$
		}
	}
}
