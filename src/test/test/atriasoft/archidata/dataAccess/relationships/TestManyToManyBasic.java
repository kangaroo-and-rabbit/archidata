package test.atriasoft.archidata.dataAccess.relationships;

import java.io.IOException;
import java.util.ArrayList;

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

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TestManyToManyBasic {

	private static TypeManyToManyDocLongRoot insertedRoot;
	private static TypeManyToManyDocLongRemote insertedRemote1;
	private static TypeManyToManyDocLongRemote insertedRemote2;

	@BeforeAll
	static void setup() throws Exception {
		ConfigureDb.configure();
	}

	@AfterAll
	static void cleanup() throws IOException {
		ConfigureDb.clear();
	}

	@Order(1)
	@Test
	void testCreateEntities() throws Exception {
		final TypeManyToManyDocLongRoot root = new TypeManyToManyDocLongRoot();
		root.otherData = "root_m2m";
		insertedRoot = ConfigureDb.da.insert(root);
		Assertions.assertNotNull(insertedRoot);
		Assertions.assertNull(insertedRoot.remote);

		TypeManyToManyDocLongRemote remote = new TypeManyToManyDocLongRemote();
		remote.data = "remote1_m2m";
		insertedRemote1 = ConfigureDb.da.insert(remote);

		remote = new TypeManyToManyDocLongRemote();
		remote.data = "remote2_m2m";
		insertedRemote2 = ConfigureDb.da.insert(remote);
	}

	@Order(2)
	@Test
	void testAddLinks() throws Exception {
		ManyToManyTools.addLink(ConfigureDb.da, TypeManyToManyDocLongRoot.class,
				insertedRoot.id, "remote", insertedRemote1.id);
		ManyToManyTools.addLink(ConfigureDb.da, TypeManyToManyDocLongRoot.class,
				insertedRoot.id, "remote", insertedRemote2.id);

		// Root should have both remotes
		final TypeManyToManyDocLongRoot rootCheck = ConfigureDb.da.getById(TypeManyToManyDocLongRoot.class,
				insertedRoot.id);
		Assertions.assertNotNull(rootCheck);
		Assertions.assertNotNull(rootCheck.remote);
		Assertions.assertEquals(2, rootCheck.remote.size());

		// Remote1 should have root in its remoteToParent
		final TypeManyToManyDocLongRemote remoteCheck = ConfigureDb.da.getById(TypeManyToManyDocLongRemote.class,
				insertedRemote1.id);
		Assertions.assertNotNull(remoteCheck);
		Assertions.assertNotNull(remoteCheck.remoteToParent);
		Assertions.assertEquals(1, remoteCheck.remoteToParent.size());
		Assertions.assertEquals(insertedRoot.id, remoteCheck.remoteToParent.get(0));
	}

	@Order(3)
	@Test
	void testRemoveLink() throws Exception {
		ManyToManyTools.removeLink(ConfigureDb.da, TypeManyToManyDocLongRoot.class,
				insertedRoot.id, "remote", insertedRemote1.id);

		// Root should only have remote2
		final TypeManyToManyDocLongRoot rootCheck = ConfigureDb.da.getById(TypeManyToManyDocLongRoot.class,
				insertedRoot.id);
		Assertions.assertNotNull(rootCheck);
		Assertions.assertNotNull(rootCheck.remote);
		Assertions.assertEquals(1, rootCheck.remote.size());
		Assertions.assertEquals(insertedRemote2.id, rootCheck.remote.get(0));

		// Remote1 should no longer have root
		final TypeManyToManyDocLongRemote remoteCheck = ConfigureDb.da.getById(TypeManyToManyDocLongRemote.class,
				insertedRemote1.id);
		Assertions.assertNotNull(remoteCheck);
		Assertions.assertNull(remoteCheck.remoteToParent);
	}

	@Order(4)
	@Test
	void testInsertWithLinksInConstructor() throws Exception {
		final TypeManyToManyDocLongRemote remote = new TypeManyToManyDocLongRemote();
		remote.data = "remote_with_links";
		remote.remoteToParent = new ArrayList<>();
		remote.remoteToParent.add(insertedRoot.id);
		final TypeManyToManyDocLongRemote insertedWithLinks = ConfigureDb.da.insert(remote);
		Assertions.assertNotNull(insertedWithLinks);
		Assertions.assertNotNull(insertedWithLinks.remoteToParent);
		Assertions.assertEquals(1, insertedWithLinks.remoteToParent.size());

		// Root should now have this remote too
		final TypeManyToManyDocLongRoot rootCheck = ConfigureDb.da.getById(TypeManyToManyDocLongRoot.class,
				insertedRoot.id);
		Assertions.assertNotNull(rootCheck);
		Assertions.assertNotNull(rootCheck.remote);
		Assertions.assertTrue(rootCheck.remote.contains(insertedWithLinks.id));
	}
}
