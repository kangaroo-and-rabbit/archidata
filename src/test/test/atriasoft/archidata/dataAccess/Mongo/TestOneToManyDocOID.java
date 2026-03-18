package test.atriasoft.archidata.dataAccess.Mongo;

import java.io.IOException;

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
import test.atriasoft.archidata.dataAccess.model.TypeOneToManyDocOIDRemote;
import test.atriasoft.archidata.dataAccess.model.TypeOneToManyDocOIDRoot;
import test.atriasoft.archidata.dataAccess.model.TypeOneToManyDocOIDRootExpand;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TestOneToManyDocOID {

	private static TypeOneToManyDocOIDRoot insertedRoot1;
	private static TypeOneToManyDocOIDRoot insertedRoot2;
	private static TypeOneToManyDocOIDRemote insertedRemote10;
	private static TypeOneToManyDocOIDRemote insertedRemote11;
	private static TypeOneToManyDocOIDRemote insertedRemote20;

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
	void testCreateRoots() throws Exception {
		final TypeOneToManyDocOIDRoot root = new TypeOneToManyDocOIDRoot();
		root.otherData = "root1";
		insertedRoot1 = ConfigureDb.da.insert(root);
		Assertions.assertNotNull(insertedRoot1);
		Assertions.assertNull(insertedRoot1.remoteIds);

		final TypeOneToManyDocOIDRoot root2 = new TypeOneToManyDocOIDRoot();
		root2.otherData = "root2";
		insertedRoot2 = ConfigureDb.da.insert(root2);
		Assertions.assertNotNull(insertedRoot2);
		Assertions.assertNull(insertedRoot2.remoteIds);
	}

	@Order(2)
	@Test
	void testInsertRemotesAddsLinks() throws Exception {
		final TypeOneToManyDocOIDRemote remote10 = new TypeOneToManyDocOIDRemote();
		remote10.data = "remote10";
		remote10.rootOid = insertedRoot1.getOid();
		insertedRemote10 = ConfigureDb.da.insert(remote10);
		Assertions.assertEquals(remote10.data, insertedRemote10.data);
		Assertions.assertEquals(remote10.rootOid, insertedRemote10.rootOid);

		final TypeOneToManyDocOIDRemote remote11 = new TypeOneToManyDocOIDRemote();
		remote11.data = "remote11";
		remote11.rootOid = insertedRoot1.getOid();
		insertedRemote11 = ConfigureDb.da.insert(remote11);

		final TypeOneToManyDocOIDRemote remote20 = new TypeOneToManyDocOIDRemote();
		remote20.data = "remote20";
		remote20.rootOid = insertedRoot2.getOid();
		insertedRemote20 = ConfigureDb.da.insert(remote20);
	}

	@Order(3)
	@Test
	void testRetrieveRawIds() throws Exception {
		// Root1 should have 2 remotes
		final TypeOneToManyDocOIDRoot root1Check = ConfigureDb.da.getById(TypeOneToManyDocOIDRoot.class,
				insertedRoot1.getOid());
		Assertions.assertNotNull(root1Check);
		Assertions.assertEquals(insertedRoot1.otherData, root1Check.otherData);
		Assertions.assertNotNull(root1Check.remoteIds);
		Assertions.assertEquals(2, root1Check.remoteIds.size());
		Assertions.assertEquals(insertedRemote10.getOid(), root1Check.remoteIds.get(0));
		Assertions.assertEquals(insertedRemote11.getOid(), root1Check.remoteIds.get(1));

		// Root2 should have 1 remote
		final TypeOneToManyDocOIDRoot root2Check = ConfigureDb.da.getById(TypeOneToManyDocOIDRoot.class,
				insertedRoot2.getOid());
		Assertions.assertNotNull(root2Check);
		Assertions.assertNotNull(root2Check.remoteIds);
		Assertions.assertEquals(1, root2Check.remoteIds.size());
		Assertions.assertEquals(insertedRemote20.getOid(), root2Check.remoteIds.get(0));
	}

	@Order(4)
	@Test
	void testRetrieveExpand() throws Exception {
		// Root1 expand: should have 2 full remote entities
		final TypeOneToManyDocOIDRootExpand root1Expand = ConfigureDb.da
				.getById(TypeOneToManyDocOIDRootExpand.class, insertedRoot1.getOid());
		Assertions.assertNotNull(root1Expand);
		Assertions.assertEquals(insertedRoot1.otherData, root1Expand.otherData);
		Assertions.assertNotNull(root1Expand.remotes);
		Assertions.assertEquals(2, root1Expand.remotes.size());
		Assertions.assertEquals(insertedRemote10.getOid(), root1Expand.remotes.get(0).getOid());
		Assertions.assertEquals(insertedRemote10.data, root1Expand.remotes.get(0).data);
		Assertions.assertEquals(insertedRemote10.rootOid, root1Expand.remotes.get(0).rootOid);
		Assertions.assertEquals(insertedRemote11.getOid(), root1Expand.remotes.get(1).getOid());
		Assertions.assertEquals(insertedRemote11.data, root1Expand.remotes.get(1).data);

		// Root2 expand: should have 1 full remote entity
		final TypeOneToManyDocOIDRootExpand root2Expand = ConfigureDb.da
				.getById(TypeOneToManyDocOIDRootExpand.class, insertedRoot2.getOid());
		Assertions.assertNotNull(root2Expand);
		Assertions.assertNotNull(root2Expand.remotes);
		Assertions.assertEquals(1, root2Expand.remotes.size());
		Assertions.assertEquals(insertedRemote20.getOid(), root2Expand.remotes.get(0).getOid());
		Assertions.assertEquals(insertedRemote20.data, root2Expand.remotes.get(0).data);
		Assertions.assertEquals(insertedRemote20.rootOid, root2Expand.remotes.get(0).rootOid);
	}
}
