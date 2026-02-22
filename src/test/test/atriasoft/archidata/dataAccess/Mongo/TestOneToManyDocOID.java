//package test.atriasoft.archidata.dataAccess.Mongo;
//
//import java.io.IOException;
//import java.util.List;
//
//import org.atriasoft.archidata.dataAccess.DBAccessSQL;
//import org.atriasoft.archidata.dataAccess.DataFactory;
//import org.junit.jupiter.api.AfterAll;
//import org.junit.jupiter.api.Assertions;
//import org.junit.jupiter.api.BeforeAll;
//import org.junit.jupiter.api.MethodOrderer;
//import org.junit.jupiter.api.Order;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.TestInstance;
//import org.junit.jupiter.api.TestMethodOrder;
//import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import test.atriasoft.archidata.ConfigureDb;
//import test.atriasoft.archidata.StepwiseExtension;
//import test.atriasoft.archidata.dataAccess.model.TypeOneToManyDocOIDRemote;
//import test.atriasoft.archidata.dataAccess.model.TypeOneToManyDocOIDRoot;
//import test.atriasoft.archidata.dataAccess.model.TypeOneToManyDocOIDRootExpand;
//
//@ExtendWith(StepwiseExtension.class)
//@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
//@TestInstance(TestInstance.Lifecycle.PER_CLASS)
//public class TestOneToManyDocOID {
//	private static final Logger LOGGER = LoggerFactory.getLogger(TestOneToManyDocOID.class);
//
//	@BeforeAll
//	public static void configureWebServer() throws Exception {
//		ConfigureDb.configure();
//	}
//
//	@AfterAll
//	public static void removeDataBase() throws IOException {
//		ConfigureDb.clear();
//	}
//
//	@Order(1)
//	@Test
//	public void testCreateTable() throws Exception {
//		final List<String> sqlCommand = DataFactory.createTable(TypeOneToManyDocOIDRemote.class);
//		sqlCommand.addAll(DataFactory.createTable(TypeOneToManyDocOIDRoot.class));
//		if (ConfigureDb.da instanceof final DBAccessSQL daSQL) {
//			for (final String elem : sqlCommand) {
//				LOGGER.debug("request: '{}'", elem);
//				daSQL.executeSimpleQuery(elem);
//			}
//		}
//	}
//
//	@Order(2)
//	@Test
//	public void testParent() throws Exception {
//		// create parent:
//
//		final TypeOneToManyDocOIDRoot root = new TypeOneToManyDocOIDRoot();
//		root.otherData = "plouf";
//		final TypeOneToManyDocOIDRoot insertedRoot = ConfigureDb.da.insert(root);
//		Assertions.assertEquals(insertedRoot.otherData, root.otherData);
//		Assertions.assertNull(insertedRoot.remoteIds);
//
//		final TypeOneToManyDocOIDRoot root2 = new TypeOneToManyDocOIDRoot();
//		root2.otherData = "plouf 2";
//		final TypeOneToManyDocOIDRoot insertedRoot2 = ConfigureDb.da.insert(root2);
//		Assertions.assertEquals(insertedRoot2.otherData, root2.otherData);
//		Assertions.assertNull(insertedRoot2.remoteIds);
//
//		// Create Some Remotes
//		final TypeOneToManyDocOIDRemote remote10 = new TypeOneToManyDocOIDRemote();
//		remote10.data = "remote10";
//		remote10.rootOid = insertedRoot.oid;
//		final TypeOneToManyDocOIDRemote insertedRemote10 = ConfigureDb.da.insert(remote10);
//		Assertions.assertEquals(insertedRemote10.data, remote10.data);
//		Assertions.assertEquals(insertedRemote10.rootOid, remote10.rootOid);
//
//		final TypeOneToManyDocOIDRemote remote11 = new TypeOneToManyDocOIDRemote();
//		remote11.data = "remote11";
//		remote11.rootOid = insertedRoot.oid;
//		final TypeOneToManyDocOIDRemote insertedRemote11 = ConfigureDb.da.insert(remote11);
//		Assertions.assertEquals(insertedRemote11.data, remote11.data);
//		Assertions.assertEquals(insertedRemote11.rootOid, remote11.rootOid);
//
//		final TypeOneToManyDocOIDRemote remote20 = new TypeOneToManyDocOIDRemote();
//		remote20.data = "remote20";
//		remote20.rootOid = insertedRoot2.oid;
//		final TypeOneToManyDocOIDRemote insertedRemote20 = ConfigureDb.da.insert(remote20);
//		Assertions.assertEquals(insertedRemote20.data, remote20.data);
//		Assertions.assertEquals(insertedRemote20.rootOid, remote20.rootOid);
//
//		// Check remote are inserted
//		final TypeOneToManyDocOIDRoot retreiveRoot1 = ConfigureDb.da.get(TypeOneToManyDocOIDRoot.class,
//				insertedRoot.oid);
//		Assertions.assertEquals(retreiveRoot1.otherData, insertedRoot.otherData);
//		Assertions.assertNotNull(retreiveRoot1.remoteIds);
//		Assertions.assertEquals(2, retreiveRoot1.remoteIds.size());
//		Assertions.assertEquals(insertedRemote10.oid, retreiveRoot1.remoteIds.get(0));
//		Assertions.assertEquals(insertedRemote11.oid, retreiveRoot1.remoteIds.get(1));
//
//		final TypeOneToManyDocOIDRoot retreiveRoot2 = ConfigureDb.da.get(TypeOneToManyDocOIDRoot.class,
//				insertedRoot2.oid);
//		Assertions.assertEquals(retreiveRoot2.otherData, insertedRoot2.otherData);
//		Assertions.assertNotNull(retreiveRoot2.remoteIds);
//		Assertions.assertEquals(1, retreiveRoot2.remoteIds.size());
//		Assertions.assertEquals(insertedRemote20.oid, retreiveRoot2.remoteIds.get(0));
//
//		// Check remote are inserted and expandable
//		final TypeOneToManyDocOIDRootExpand retreiveRootExpand1 = ConfigureDb.da
//				.get(TypeOneToManyDocOIDRootExpand.class, insertedRoot.oid);
//		Assertions.assertEquals(retreiveRootExpand1.otherData, insertedRoot.otherData);
//		Assertions.assertNotNull(retreiveRootExpand1.remotes);
//		Assertions.assertEquals(2, retreiveRootExpand1.remotes.size());
//		Assertions.assertEquals(insertedRemote10.oid, retreiveRootExpand1.remotes.get(0).oid);
//		Assertions.assertEquals(insertedRemote10.rootOid, retreiveRootExpand1.remotes.get(0).rootOid);
//		Assertions.assertEquals(insertedRemote10.data, retreiveRootExpand1.remotes.get(0).data);
//		Assertions.assertEquals(insertedRemote11.oid, retreiveRootExpand1.remotes.get(1).oid);
//		Assertions.assertEquals(insertedRemote11.rootOid, retreiveRootExpand1.remotes.get(1).rootOid);
//		Assertions.assertEquals(insertedRemote11.data, retreiveRootExpand1.remotes.get(1).data);
//
//		final TypeOneToManyDocOIDRootExpand retreiveRootExpand2 = ConfigureDb.da
//				.get(TypeOneToManyDocOIDRootExpand.class, insertedRoot2.oid);
//		Assertions.assertEquals(retreiveRootExpand2.otherData, insertedRoot2.otherData);
//		Assertions.assertNotNull(retreiveRootExpand2.remotes);
//		Assertions.assertEquals(1, retreiveRootExpand2.remotes.size());
//		Assertions.assertEquals(insertedRemote20.oid, retreiveRootExpand2.remotes.get(0).oid);
//		Assertions.assertEquals(insertedRemote20.rootOid, retreiveRootExpand2.remotes.get(0).rootOid);
//		Assertions.assertEquals(insertedRemote20.data, retreiveRootExpand2.remotes.get(0).data);
//
//	}
//
//	@Order(2)
//	@Test
//	public void testRemoveItem() throws Exception {
//		// create parent:
//
//		final TypeOneToManyDocOIDRoot root = new TypeOneToManyDocOIDRoot();
//		root.otherData = "plouf";
//		final TypeOneToManyDocOIDRoot insertedRoot = ConfigureDb.da.insert(root);
//		Assertions.assertEquals(insertedRoot.otherData, root.otherData);
//		Assertions.assertNull(insertedRoot.remoteIds);
//
//		final TypeOneToManyDocOIDRoot root2 = new TypeOneToManyDocOIDRoot();
//		root2.otherData = "plouf 2";
//		final TypeOneToManyDocOIDRoot insertedRoot2 = ConfigureDb.da.insert(root2);
//		Assertions.assertEquals(insertedRoot2.otherData, root2.otherData);
//		Assertions.assertNull(insertedRoot2.remoteIds);
//
//		// Create Some Remotes
//		final TypeOneToManyDocOIDRemote remote10 = new TypeOneToManyDocOIDRemote();
//		remote10.data = "remote10";
//		remote10.rootOid = insertedRoot.oid;
//		final TypeOneToManyDocOIDRemote insertedRemote10 = ConfigureDb.da.insert(remote10);
//		Assertions.assertEquals(insertedRemote10.data, remote10.data);
//		Assertions.assertEquals(insertedRemote10.rootOid, remote10.rootOid);
//
//		final TypeOneToManyDocOIDRemote remote11 = new TypeOneToManyDocOIDRemote();
//		remote11.data = "remote11";
//		remote11.rootOid = insertedRoot.oid;
//		final TypeOneToManyDocOIDRemote insertedRemote11 = ConfigureDb.da.insert(remote11);
//		Assertions.assertEquals(insertedRemote11.data, remote11.data);
//		Assertions.assertEquals(insertedRemote11.rootOid, remote11.rootOid);
//
//		final TypeOneToManyDocOIDRemote remote20 = new TypeOneToManyDocOIDRemote();
//		remote20.data = "remote20";
//		remote20.rootOid = insertedRoot2.oid;
//		final TypeOneToManyDocOIDRemote insertedRemote20 = ConfigureDb.da.insert(remote20);
//		Assertions.assertEquals(insertedRemote20.data, remote20.data);
//		Assertions.assertEquals(insertedRemote20.rootOid, remote20.rootOid);
//
//		// Check remote are inserted
//		final TypeOneToManyDocOIDRoot retreiveRoot1 = ConfigureDb.da.get(TypeOneToManyDocOIDRoot.class,
//				insertedRoot.oid);
//		Assertions.assertEquals(retreiveRoot1.otherData, insertedRoot.otherData);
//		Assertions.assertNotNull(retreiveRoot1.remoteIds);
//		Assertions.assertEquals(2, retreiveRoot1.remoteIds.size());
//		Assertions.assertEquals(insertedRemote10.oid, retreiveRoot1.remoteIds.get(0));
//		Assertions.assertEquals(insertedRemote11.oid, retreiveRoot1.remoteIds.get(1));
//
//		final TypeOneToManyDocOIDRoot retreiveRoot2 = ConfigureDb.da.get(TypeOneToManyDocOIDRoot.class,
//				insertedRoot2.oid);
//		Assertions.assertEquals(retreiveRoot2.otherData, insertedRoot2.otherData);
//		Assertions.assertNotNull(retreiveRoot2.remoteIds);
//		Assertions.assertEquals(1, retreiveRoot2.remoteIds.size());
//		Assertions.assertEquals(insertedRemote20.oid, retreiveRoot2.remoteIds.get(0));
//
//		// Check remote are inserted and expandable
//		final TypeOneToManyDocOIDRootExpand retreiveRootExpand1 = ConfigureDb.da
//				.get(TypeOneToManyDocOIDRootExpand.class, insertedRoot.oid);
//		Assertions.assertEquals(retreiveRootExpand1.otherData, insertedRoot.otherData);
//		Assertions.assertNotNull(retreiveRootExpand1.remotes);
//		Assertions.assertEquals(2, retreiveRootExpand1.remotes.size());
//		Assertions.assertEquals(insertedRemote10.oid, retreiveRootExpand1.remotes.get(0).oid);
//		Assertions.assertEquals(insertedRemote10.rootOid, retreiveRootExpand1.remotes.get(0).rootOid);
//		Assertions.assertEquals(insertedRemote10.data, retreiveRootExpand1.remotes.get(0).data);
//		Assertions.assertEquals(insertedRemote11.oid, retreiveRootExpand1.remotes.get(1).oid);
//		Assertions.assertEquals(insertedRemote11.rootOid, retreiveRootExpand1.remotes.get(1).rootOid);
//		Assertions.assertEquals(insertedRemote11.data, retreiveRootExpand1.remotes.get(1).data);
//
//		final TypeOneToManyDocOIDRootExpand retreiveRootExpand2 = ConfigureDb.da
//				.get(TypeOneToManyDocOIDRootExpand.class, insertedRoot2.oid);
//		Assertions.assertEquals(retreiveRootExpand2.otherData, insertedRoot2.otherData);
//		Assertions.assertNotNull(retreiveRootExpand2.remotes);
//		Assertions.assertEquals(1, retreiveRootExpand2.remotes.size());
//		Assertions.assertEquals(insertedRemote20.oid, retreiveRootExpand2.remotes.get(0).oid);
//		Assertions.assertEquals(insertedRemote20.rootOid, retreiveRootExpand2.remotes.get(0).rootOid);
//		Assertions.assertEquals(insertedRemote20.data, retreiveRootExpand2.remotes.get(0).data);
//
//	}
//
//}
