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
package li.strolch.runtime.query.inmemory;

import static org.junit.Assert.assertEquals;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import li.strolch.model.ModelGenerator;
import li.strolch.model.Tags;
import li.strolch.model.audit.AccessType;
import li.strolch.model.audit.Audit;
import li.strolch.model.audit.AuditQuery;
import li.strolch.model.audit.AuditVisitor;
import li.strolch.model.audit.NoStrategyAuditVisitor;
import li.strolch.persistence.inmemory.InMemoryAuditDao;
import li.strolch.utils.StringMatchMode;
import li.strolch.utils.collections.DateRange;

import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author Robert von Burg <eitch@eitchnet.ch>
 */
@SuppressWarnings("nls")
public class AuditQueryTest {

	private static Date past;
	private static Date earlier;
	private static Date current;
	private static Date later;
	private static Date future;

	@BeforeClass
	public static void beforeClass() throws SQLException {

		Calendar cal = Calendar.getInstance();
		cal.clear();
		cal.set(2000, 1, 1);
		past = cal.getTime();
		cal.set(2000, 4, 1);
		earlier = cal.getTime();
		cal.set(2000, 6, 1);
		current = cal.getTime();
		cal.set(2000, 8, 1);
		later = cal.getTime();
		cal.set(2000, 11, 1);
		future = cal.getTime();
	}

	@Test
	public void shouldQueryTypeAndDateRange() throws SQLException {

		AuditVisitor<Audit> visitor = new NoStrategyAuditVisitor();

		AuditQuery<Audit> query = new AuditQuery<>(visitor, Tags.AUDIT, new DateRange().from(earlier, true).to(later,
				true));
		performQuery(query, Arrays.asList(0L, 1L, 2L, 3L, 4L));

		query = new AuditQuery<>(visitor, Tags.AUDIT, new DateRange().from(current, true).to(current, true));
		performQuery(query, Arrays.asList(1L, 3L, 4L));

		query = new AuditQuery<>(visitor, Tags.AUDIT, new DateRange().from(current, true));
		performQuery(query, Arrays.asList(1L, 2L, 3L, 4L));

		query = new AuditQuery<>(visitor, Tags.AUDIT, new DateRange().to(current, true));
		performQuery(query, Arrays.asList(0L, 1L, 3L, 4L));

		query = new AuditQuery<>(visitor, Tags.RESOURCE, new DateRange().from(past, true).to(future, true));
		performQuery(query, Arrays.<Long> asList());
	}

	@Test
	public void shouldQueryAudits() throws SQLException {

		AuditVisitor<Audit> visitor = new NoStrategyAuditVisitor();

		AuditQuery<Audit> query = new AuditQuery<>(visitor, Tags.AUDIT, new DateRange().from(past, true).to(future,
				true));
		query.action().accessTypes(AccessType.CREATE, AccessType.READ);
		performQuery(query, Arrays.asList(0L, 1L, 4L));

		query = new AuditQuery<>(visitor, Tags.AUDIT, new DateRange().from(past, true).to(future, true));
		query.action().accessTypes(AccessType.CREATE);
		performQuery(query, Arrays.asList(0L, 4L));

		query = new AuditQuery<>(visitor, Tags.AUDIT, new DateRange().from(past, true).to(future, true));
		query.action().accessTypes(AccessType.CREATE, AccessType.READ)
				.actions(StringMatchMode.EQUALS_CASE_SENSITIVE, "create", "read");
		performQuery(query, Arrays.asList(0L, 1L, 4L));

		query = new AuditQuery<>(visitor, Tags.AUDIT, new DateRange().from(past, true).to(future, true));
		query.action().accessTypes(AccessType.CREATE, AccessType.READ)
				.actions(StringMatchMode.EQUALS_CASE_SENSITIVE, "read");
		performQuery(query, Arrays.asList(1L));

		query = new AuditQuery<>(visitor, Tags.AUDIT, new DateRange().from(past, true).to(future, true));
		query.element().elementAccessed(StringMatchMode.CONTAINS_CASE_INSENSITIVE, "crea");
		performQuery(query, Arrays.asList(0L, 4L));

		query = new AuditQuery<>(visitor, Tags.AUDIT, new DateRange().from(past, true).to(future, true));
		query.element().elementAccessed(StringMatchMode.CONTAINS_CASE_SENSITIVE, "crea");
		performQuery(query, Arrays.<Long> asList());

		query = new AuditQuery<>(visitor, Tags.AUDIT, new DateRange().from(past, true).to(future, true));
		query.element().elementAccessed(StringMatchMode.EQUALS_CASE_INSENSITIVE, "create");
		performQuery(query, Arrays.asList(0L, 4L));

		query = new AuditQuery<>(visitor, Tags.AUDIT, new DateRange().from(past, true).to(future, true));
		query.identity().usernames(StringMatchMode.EQUALS_CASE_INSENSITIVE, "earlier");
		performQuery(query, Arrays.asList(0L));

		query = new AuditQuery<>(visitor, Tags.AUDIT, new DateRange().from(past, true).to(future, true));
		query.identity().usernames(StringMatchMode.EQUALS_CASE_INSENSITIVE, "earlier", "later");
		performQuery(query, Arrays.asList(0L, 2L));

		query = new AuditQuery<>(visitor, Tags.AUDIT, new DateRange().from(past, true).to(future, true));
		query.identity().usernames(StringMatchMode.EQUALS_CASE_INSENSITIVE, "earlier")
				.firstnames(StringMatchMode.CONTAINS_CASE_INSENSITIVE, "enn");
		performQuery(query, Arrays.asList(0L));

		query = new AuditQuery<>(visitor, Tags.AUDIT, new DateRange().from(past, true).to(future, true));
		query.identity().usernames(StringMatchMode.EQUALS_CASE_INSENSITIVE, "earlier")
				.firstnames(StringMatchMode.CONTAINS_CASE_INSENSITIVE, "enn")
				.lastnames(StringMatchMode.CONTAINS_CASE_INSENSITIVE, "kennedy");
		performQuery(query, Arrays.asList(0L));

		query = new AuditQuery<>(visitor, Tags.AUDIT, new DateRange().from(past, true).to(future, true));
		query.identity().firstnames(StringMatchMode.CONTAINS_CASE_INSENSITIVE, "enn")
				.lastnames(StringMatchMode.CONTAINS_CASE_INSENSITIVE, "kennedy");
		performQuery(query, Arrays.asList(0L, 1L, 2L, 3L, 4L));

		query = new AuditQuery<>(visitor, Tags.AUDIT, new DateRange().from(past, true).to(future, true));
		query.element().elementSubTypes(StringMatchMode.EQUALS_CASE_SENSITIVE, "Foo");
		performQuery(query, Arrays.asList(0L, 1L, 2L, 3L, 4L));

		query = new AuditQuery<>(visitor, Tags.AUDIT, new DateRange().from(past, true).to(future, true));
		query.element().elementSubTypes(StringMatchMode.EQUALS_CASE_SENSITIVE, "Bar");
		performQuery(query, Arrays.asList());

		query = new AuditQuery<>(visitor, Tags.AUDIT, new DateRange().from(past, true).to(future, true));
		query.limit(1).element().elementSubTypes(StringMatchMode.EQUALS_CASE_SENSITIVE, "Foo");
		performQuery(query, Arrays.asList(2L));
	}

	private void performQuery(AuditQuery<Audit> query, List<Long> expected) throws SQLException {

		InMemoryAuditDao dao = new InMemoryAuditDao();
		dao.saveAll(getAudits());

		List<Audit> result = dao.doQuery(query);
		Set<Long> ids = new HashSet<>();
		for (Audit audit : result) {
			ids.add(audit.getId());
		}
		assertEquals(new HashSet<>(expected), new HashSet<>(ids));
	}

	private static List<Audit> getAudits() {
		List<Audit> audits = new ArrayList<>();
		int i = 0;

		Audit randomAudit;
		randomAudit = ModelGenerator.randomAudit();
		randomAudit.setId(i++);
		randomAudit.setUsername("earlier");
		randomAudit.setDate(earlier);
		randomAudit.setAccessType(AccessType.CREATE);
		randomAudit.setAction("create");
		randomAudit.setElementAccessed(randomAudit.getAccessType().name());
		audits.add(randomAudit);

		randomAudit = ModelGenerator.randomAudit();
		randomAudit.setId(i++);
		randomAudit.setDate(current);
		randomAudit.setUsername("current");
		randomAudit.setAccessType(AccessType.READ);
		randomAudit.setAction("read");
		randomAudit.setElementAccessed(randomAudit.getAccessType().name());
		audits.add(randomAudit);

		randomAudit = ModelGenerator.randomAudit();
		randomAudit.setId(i++);
		randomAudit.setDate(later);
		randomAudit.setUsername("later");
		randomAudit.setAccessType(AccessType.UPDATE);
		randomAudit.setAction("update");
		randomAudit.setElementAccessed(randomAudit.getAccessType().name());
		audits.add(randomAudit);

		randomAudit = ModelGenerator.randomAudit();
		randomAudit.setId(i++);
		randomAudit.setDate(current);
		randomAudit.setUsername("current");
		randomAudit.setAccessType(AccessType.DELETE);
		randomAudit.setAction("delete");
		randomAudit.setElementAccessed(randomAudit.getAccessType().name());
		audits.add(randomAudit);

		randomAudit = ModelGenerator.randomAudit();
		randomAudit.setId(i++);
		randomAudit.setDate(current);
		randomAudit.setUsername("current");
		randomAudit.setAccessType(AccessType.CREATE);
		randomAudit.setAction("create");
		randomAudit.setElementAccessed(randomAudit.getAccessType().name());
		audits.add(randomAudit);

		return audits;
	}
}
