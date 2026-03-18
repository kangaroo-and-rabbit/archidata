package test.atriasoft.archidata.dataAccess.relationships;

import java.io.IOException;
import java.util.List;

import org.atriasoft.archidata.dataAccess.commonTools.ManyToManyTools;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import test.atriasoft.archidata.ConfigureDb;
import test.atriasoft.archidata.StepwiseExtension;
import test.atriasoft.archidata.dataAccess.model.TypeManyToManyDocLongRemote;
import test.atriasoft.archidata.dataAccess.model.TypeManyToManyDocLongRoot;
import test.atriasoft.archidata.dataAccess.model.TypeManyToManyDocLongRootExpand;
import test.atriasoft.archidata.dataAccess.model.TypeManyToOneDocLongChildExpand;
import test.atriasoft.archidata.dataAccess.model.TypeManyToOneDocLongChildTTT;
import test.atriasoft.archidata.dataAccess.model.TypeManyToOneDocLongParentIgnore;
import test.atriasoft.archidata.dataAccess.model.TypeOneToManyDocLongParentExpandIgnore;
import test.atriasoft.archidata.dataAccess.model.TypeOneToManyDocLongParentIgnore;
import test.atriasoft.archidata.dataAccess.model.TypeOneToManyDocLongRemote;

/**
 * Tests multi-row retrieval with entity-reference expansion.
 * Validates that gets() returns correct expanded entities for multiple rows.
 * This is the N+1 scenario — before batching, each row triggers individual queries.
 */
@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TestExpandMultiRow {

	// ManyToOne: 2 parents, 3 children (2 for parent1, 1 for parent2)
	private static TypeManyToOneDocLongParentIgnore m2oParent1;
	private static TypeManyToOneDocLongParentIgnore m2oParent2;
	private static TypeManyToOneDocLongChildTTT m2oChild1;
	private static TypeManyToOneDocLongChildTTT m2oChild2;
	private static TypeManyToOneDocLongChildTTT m2oChild3;

	// ManyToMany: 2 roots, 3 remotes
	private static TypeManyToManyDocLongRoot m2mRoot1;
	private static TypeManyToManyDocLongRoot m2mRoot2;
	private static TypeManyToManyDocLongRemote m2mRemote1;
	private static TypeManyToManyDocLongRemote m2mRemote2;
	private static TypeManyToManyDocLongRemote m2mRemote3;

	// OneToMany: 2 parents, 3 remotes
	private static TypeOneToManyDocLongParentIgnore o2mParent1;
	private static TypeOneToManyDocLongParentIgnore o2mParent2;
	private static TypeOneToManyDocLongRemote o2mRemote1;
	private static TypeOneToManyDocLongRemote o2mRemote2;
	private static TypeOneToManyDocLongRemote o2mRemote3;

	@BeforeAll
	static void setup() throws Exception {
		ConfigureDb.configure();
	}

	@AfterAll
	static void cleanup() throws IOException {
		ConfigureDb.clear();
	}

	// ========== ManyToOne: multi-row child expand ==========

	@Order(1)
	@Test
	void testManyToOneSetup() throws Exception {
		TypeManyToOneDocLongParentIgnore parent = new TypeManyToOneDocLongParentIgnore();
		parent.data = "mr_m2o_parent1";
		m2oParent1 = ConfigureDb.da.insert(parent);

		parent = new TypeManyToOneDocLongParentIgnore();
		parent.data = "mr_m2o_parent2";
		m2oParent2 = ConfigureDb.da.insert(parent);

		m2oChild1 = ConfigureDb.da.insert(new TypeManyToOneDocLongChildTTT("mr_child1", m2oParent1.getId()));
		m2oChild2 = ConfigureDb.da.insert(new TypeManyToOneDocLongChildTTT("mr_child2", m2oParent1.getId()));
		m2oChild3 = ConfigureDb.da.insert(new TypeManyToOneDocLongChildTTT("mr_child3", m2oParent2.getId()));
	}

	@Order(2)
	@Test
	void testManyToOneMultiRowExpand() throws Exception {
		final List<TypeManyToOneDocLongChildExpand> children = ConfigureDb.da
				.gets(TypeManyToOneDocLongChildExpand.class);
		Assertions.assertNotNull(children);
		Assertions.assertTrue(children.size() >= 3, "Expected at least 3 children, got " + children.size());

		// Find our children in the results
		TypeManyToOneDocLongChildExpand found1 = null;
		TypeManyToOneDocLongChildExpand found2 = null;
		TypeManyToOneDocLongChildExpand found3 = null;
		for (final TypeManyToOneDocLongChildExpand child : children) {
			if (m2oChild1.getId().equals(child.getId())) {
				found1 = child;
			} else if (m2oChild2.getId().equals(child.getId())) {
				found2 = child;
			} else if (m2oChild3.getId().equals(child.getId())) {
				found3 = child;
			}
		}

		// child1 and child2 should both expand to parent1
		Assertions.assertNotNull(found1);
		Assertions.assertNotNull(found1.parent);
		Assertions.assertEquals(m2oParent1.getId(), found1.parent.getId());
		Assertions.assertEquals(m2oParent1.data, found1.parent.data);

		Assertions.assertNotNull(found2);
		Assertions.assertNotNull(found2.parent);
		Assertions.assertEquals(m2oParent1.getId(), found2.parent.getId());

		// child3 should expand to parent2
		Assertions.assertNotNull(found3);
		Assertions.assertNotNull(found3.parent);
		Assertions.assertEquals(m2oParent2.getId(), found3.parent.getId());
		Assertions.assertEquals(m2oParent2.data, found3.parent.data);
	}

	// ========== ManyToMany: multi-row root expand ==========

	@Order(3)
	@Test
	void testManyToManySetup() throws Exception {
		TypeManyToManyDocLongRoot root = new TypeManyToManyDocLongRoot();
		root.otherData = "mr_m2m_root1";
		m2mRoot1 = ConfigureDb.da.insert(root);

		root = new TypeManyToManyDocLongRoot();
		root.otherData = "mr_m2m_root2";
		m2mRoot2 = ConfigureDb.da.insert(root);

		TypeManyToManyDocLongRemote remote = new TypeManyToManyDocLongRemote();
		remote.data = "mr_m2m_remote1";
		m2mRemote1 = ConfigureDb.da.insert(remote);

		remote = new TypeManyToManyDocLongRemote();
		remote.data = "mr_m2m_remote2";
		m2mRemote2 = ConfigureDb.da.insert(remote);

		remote = new TypeManyToManyDocLongRemote();
		remote.data = "mr_m2m_remote3";
		m2mRemote3 = ConfigureDb.da.insert(remote);

		// root1 has remote1 and remote2
		ManyToManyTools.addLink(ConfigureDb.da, TypeManyToManyDocLongRoot.class, m2mRoot1.getId(), "remote",
				m2mRemote1.getId());
		ManyToManyTools.addLink(ConfigureDb.da, TypeManyToManyDocLongRoot.class, m2mRoot1.getId(), "remote",
				m2mRemote2.getId());

		// root2 has remote2 and remote3 (remote2 shared)
		ManyToManyTools.addLink(ConfigureDb.da, TypeManyToManyDocLongRoot.class, m2mRoot2.getId(), "remote",
				m2mRemote2.getId());
		ManyToManyTools.addLink(ConfigureDb.da, TypeManyToManyDocLongRoot.class, m2mRoot2.getId(), "remote",
				m2mRemote3.getId());
	}

	@Order(4)
	@Test
	void testManyToManyMultiRowExpand() throws Exception {
		final List<TypeManyToManyDocLongRootExpand> roots = ConfigureDb.da
				.gets(TypeManyToManyDocLongRootExpand.class);
		Assertions.assertNotNull(roots);
		Assertions.assertTrue(roots.size() >= 2, "Expected at least 2 roots, got " + roots.size());

		TypeManyToManyDocLongRootExpand foundRoot1 = null;
		TypeManyToManyDocLongRootExpand foundRoot2 = null;
		for (final TypeManyToManyDocLongRootExpand r : roots) {
			if (m2mRoot1.getId().equals(r.getId())) {
				foundRoot1 = r;
			} else if (m2mRoot2.getId().equals(r.getId())) {
				foundRoot2 = r;
			}
		}

		// root1 should have remote1 and remote2
		Assertions.assertNotNull(foundRoot1);
		Assertions.assertNotNull(foundRoot1.remote);
		Assertions.assertEquals(2, foundRoot1.remote.size());
		Assertions.assertEquals(m2mRemote1.getId(), foundRoot1.remote.get(0).getId());
		Assertions.assertEquals(m2mRemote1.data, foundRoot1.remote.get(0).data);
		Assertions.assertEquals(m2mRemote2.getId(), foundRoot1.remote.get(1).getId());

		// root2 should have remote2 and remote3
		Assertions.assertNotNull(foundRoot2);
		Assertions.assertNotNull(foundRoot2.remote);
		Assertions.assertEquals(2, foundRoot2.remote.size());
		Assertions.assertEquals(m2mRemote2.getId(), foundRoot2.remote.get(0).getId());
		Assertions.assertEquals(m2mRemote3.getId(), foundRoot2.remote.get(1).getId());
		Assertions.assertEquals(m2mRemote3.data, foundRoot2.remote.get(1).data);
	}

	// ========== OneToMany: multi-row parent expand ==========

	@Order(5)
	@Test
	void testOneToManySetup() throws Exception {
		// Create remotes first (without parent link)
		o2mRemote1 = ConfigureDb.da.insert(new TypeOneToManyDocLongRemote("mr_o2m_remote1", null));
		o2mRemote2 = ConfigureDb.da.insert(new TypeOneToManyDocLongRemote("mr_o2m_remote2", null));
		o2mRemote3 = ConfigureDb.da.insert(new TypeOneToManyDocLongRemote("mr_o2m_remote3", null));

		// Create parents with remote IDs — addLinkWhenCreate=true sets parentId on remotes
		o2mParent1 = ConfigureDb.da.insert(new TypeOneToManyDocLongParentIgnore("mr_o2m_parent1",
				List.of(o2mRemote1.getId(), o2mRemote2.getId())));
		o2mParent2 = ConfigureDb.da.insert(new TypeOneToManyDocLongParentIgnore("mr_o2m_parent2",
				List.of(o2mRemote3.getId())));
	}

	@Order(6)
	@Test
	void testOneToManyMultiRowExpand() throws Exception {
		final List<TypeOneToManyDocLongParentExpandIgnore> parents = ConfigureDb.da
				.gets(TypeOneToManyDocLongParentExpandIgnore.class);
		Assertions.assertNotNull(parents);
		Assertions.assertTrue(parents.size() >= 2, "Expected at least 2 parents, got " + parents.size());

		TypeOneToManyDocLongParentExpandIgnore foundParent1 = null;
		TypeOneToManyDocLongParentExpandIgnore foundParent2 = null;
		for (final TypeOneToManyDocLongParentExpandIgnore p : parents) {
			if (o2mParent1.getId().equals(p.getId())) {
				foundParent1 = p;
			} else if (o2mParent2.getId().equals(p.getId())) {
				foundParent2 = p;
			}
		}

		// parent1 should have remote1 and remote2
		Assertions.assertNotNull(foundParent1);
		Assertions.assertNotNull(foundParent1.remoteEntities);
		Assertions.assertEquals(2, foundParent1.remoteEntities.size());
		Assertions.assertEquals(o2mRemote1.getId(), foundParent1.remoteEntities.get(0).getId());
		Assertions.assertEquals(o2mRemote1.data, foundParent1.remoteEntities.get(0).data);
		Assertions.assertEquals(o2mRemote2.getId(), foundParent1.remoteEntities.get(1).getId());
		Assertions.assertEquals(o2mRemote2.data, foundParent1.remoteEntities.get(1).data);

		// parent2 should have remote3
		Assertions.assertNotNull(foundParent2);
		Assertions.assertNotNull(foundParent2.remoteEntities);
		Assertions.assertEquals(1, foundParent2.remoteEntities.size());
		Assertions.assertEquals(o2mRemote3.getId(), foundParent2.remoteEntities.get(0).getId());
		Assertions.assertEquals(o2mRemote3.data, foundParent2.remoteEntities.get(0).data);
	}
}
