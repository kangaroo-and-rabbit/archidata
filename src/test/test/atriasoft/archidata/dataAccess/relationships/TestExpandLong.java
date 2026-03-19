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
 * Tests entity-reference (expand) retrieval for all 3 relation types with Long IDs.
 */
@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TestExpandLong {

	// ManyToOne data
	private static TypeManyToOneDocLongParentIgnore m2oParent;
	private static TypeManyToOneDocLongChildTTT m2oChild;

	// ManyToMany data
	private static TypeManyToManyDocLongRoot m2mRoot;
	private static TypeManyToManyDocLongRemote m2mRemote1;
	private static TypeManyToManyDocLongRemote m2mRemote2;

	// OneToMany data
	private static TypeOneToManyDocLongParentIgnore o2mParent;
	private static TypeOneToManyDocLongRemote o2mRemote1;
	private static TypeOneToManyDocLongRemote o2mRemote2;

	@BeforeAll
	static void setup() throws Exception {
		ConfigureDb.configure();
	}

	@AfterAll
	static void cleanup() throws IOException {
		ConfigureDb.clear();
	}

	// ========== ManyToOne expand (child -> parent entity) ==========

	@Order(1)
	@Test
	void testManyToOneSetup() throws Exception {
		final TypeManyToOneDocLongParentIgnore parent = new TypeManyToOneDocLongParentIgnore();
		parent.data = "m2o_parent";
		m2oParent = ConfigureDb.da.insert(parent);
		Assertions.assertNotNull(m2oParent);

		final TypeManyToOneDocLongChildTTT child = new TypeManyToOneDocLongChildTTT("m2o_child", m2oParent.getId());
		m2oChild = ConfigureDb.da.insert(child);
		Assertions.assertNotNull(m2oChild);
		Assertions.assertEquals(m2oParent.getId(), m2oChild.parentId);
	}

	@Order(2)
	@Test
	void testManyToOneExpand() throws Exception {
		final TypeManyToOneDocLongChildExpand childExpand = ConfigureDb.da
				.getById(TypeManyToOneDocLongChildExpand.class, m2oChild.getId());
		Assertions.assertNotNull(childExpand);
		Assertions.assertEquals(m2oChild.otherData, childExpand.otherData);
		Assertions.assertNotNull(childExpand.parent);
		Assertions.assertEquals(m2oParent.getId(), childExpand.parent.getId());
		Assertions.assertEquals(m2oParent.data, childExpand.parent.data);
	}

	// ========== ManyToMany expand (root -> list of remote entities) ==========

	@Order(3)
	@Test
	void testManyToManySetup() throws Exception {
		final TypeManyToManyDocLongRoot root = new TypeManyToManyDocLongRoot();
		root.otherData = "m2m_root";
		m2mRoot = ConfigureDb.da.insert(root);
		Assertions.assertNotNull(m2mRoot);

		TypeManyToManyDocLongRemote remote = new TypeManyToManyDocLongRemote();
		remote.data = "m2m_remote1";
		m2mRemote1 = ConfigureDb.da.insert(remote);

		remote = new TypeManyToManyDocLongRemote();
		remote.data = "m2m_remote2";
		m2mRemote2 = ConfigureDb.da.insert(remote);

		ManyToManyTools.addLink(ConfigureDb.da, TypeManyToManyDocLongRoot.class, m2mRoot.getId(), "remote",
				m2mRemote1.getId());
		ManyToManyTools.addLink(ConfigureDb.da, TypeManyToManyDocLongRoot.class, m2mRoot.getId(), "remote",
				m2mRemote2.getId());
	}

	@Order(4)
	@Test
	void testManyToManyExpand() throws Exception {
		final TypeManyToManyDocLongRootExpand rootExpand = ConfigureDb.da.getById(TypeManyToManyDocLongRootExpand.class,
				m2mRoot.getId());
		Assertions.assertNotNull(rootExpand);
		Assertions.assertEquals(m2mRoot.otherData, rootExpand.otherData);
		Assertions.assertNotNull(rootExpand.remote);
		Assertions.assertEquals(2, rootExpand.remote.size());
		Assertions.assertEquals(m2mRemote1.getId(), rootExpand.remote.get(0).getId());
		Assertions.assertEquals(m2mRemote1.data, rootExpand.remote.get(0).data);
		Assertions.assertEquals(m2mRemote2.getId(), rootExpand.remote.get(1).getId());
		Assertions.assertEquals(m2mRemote2.data, rootExpand.remote.get(1).data);
	}

	// ========== OneToMany expand (parent -> list of remote entities) ==========

	@Order(5)
	@Test
	void testOneToManySetup() throws Exception {
		// Create remotes first (without parent link)
		o2mRemote1 = ConfigureDb.da.insert(new TypeOneToManyDocLongRemote("o2m_remote1", null));
		o2mRemote2 = ConfigureDb.da.insert(new TypeOneToManyDocLongRemote("o2m_remote2", null));

		// Create parent with remote IDs — addLinkWhenCreate=true sets parentId on remotes
		final TypeOneToManyDocLongParentIgnore parent = new TypeOneToManyDocLongParentIgnore("o2m_parent",
				List.of(o2mRemote1.getId(), o2mRemote2.getId()));
		o2mParent = ConfigureDb.da.insert(parent);
		Assertions.assertNotNull(o2mParent);
	}

	@Order(6)
	@Test
	void testOneToManyRawIds() throws Exception {
		final TypeOneToManyDocLongParentIgnore parentCheck = ConfigureDb.da
				.getById(TypeOneToManyDocLongParentIgnore.class, o2mParent.getId());
		Assertions.assertNotNull(parentCheck);
		Assertions.assertNotNull(parentCheck.remoteIds);
		Assertions.assertEquals(2, parentCheck.remoteIds.size());
		Assertions.assertEquals(o2mRemote1.getId(), parentCheck.remoteIds.get(0));
		Assertions.assertEquals(o2mRemote2.getId(), parentCheck.remoteIds.get(1));
	}

	@Order(7)
	@Test
	void testOneToManyExpand() throws Exception {
		final TypeOneToManyDocLongParentExpandIgnore parentExpand = ConfigureDb.da
				.getById(TypeOneToManyDocLongParentExpandIgnore.class, o2mParent.getId());
		Assertions.assertNotNull(parentExpand);
		Assertions.assertEquals(o2mParent.data, parentExpand.data);
		Assertions.assertNotNull(parentExpand.remoteEntities);
		Assertions.assertEquals(2, parentExpand.remoteEntities.size());
		Assertions.assertEquals(o2mRemote1.getId(), parentExpand.remoteEntities.get(0).getId());
		Assertions.assertEquals(o2mRemote1.data, parentExpand.remoteEntities.get(0).data);
		Assertions.assertEquals(o2mRemote2.getId(), parentExpand.remoteEntities.get(1).getId());
		Assertions.assertEquals(o2mRemote2.data, parentExpand.remoteEntities.get(1).data);
	}
}
