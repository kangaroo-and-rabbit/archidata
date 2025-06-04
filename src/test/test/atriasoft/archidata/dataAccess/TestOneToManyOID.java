package test.atriasoft.archidata.dataAccess;

import java.io.IOException;
import java.util.List;

import org.atriasoft.archidata.dataAccess.DBAccessSQL;
import org.atriasoft.archidata.dataAccess.DataFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import test.atriasoft.archidata.ConfigureDb;
import test.atriasoft.archidata.StepwiseExtension;
import test.atriasoft.archidata.dataAccess.model.TypeOneToManyOIDRemote;
import test.atriasoft.archidata.dataAccess.model.TypeOneToManyOIDRoot;
import test.atriasoft.archidata.dataAccess.model.TypeOneToManyOIDRootExpand;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@EnabledIfEnvironmentVariable(named = "INCLUDE_MY_SQL_SPECIFIC", matches = "true")
public class TestOneToManyOID {
	final static private Logger LOGGER = LoggerFactory.getLogger(TestOneToManyOID.class);

	@BeforeAll
	public static void configureWebServer() throws Exception {
		ConfigureDb.configure();
	}

	@AfterAll
	public static void removeDataBase() throws IOException {
		ConfigureDb.clear();
	}

	@Order(1)
	@Test
	public void testCreateTable() throws Exception {
		final List<String> sqlCommand = DataFactory.createTable(TypeOneToManyOIDRemote.class);
		sqlCommand.addAll(DataFactory.createTable(TypeOneToManyOIDRoot.class));
		if (ConfigureDb.da instanceof final DBAccessSQL daSQL) {
			for (final String elem : sqlCommand) {
				LOGGER.debug("request: '{}'", elem);
				daSQL.executeSimpleQuery(elem);
			}
		}
	}

	@Order(2)
	@Test
	public void testParent() throws Exception {
		// create parent:

		final TypeOneToManyOIDRoot root = new TypeOneToManyOIDRoot();
		root.otherData = "plouf";
		final TypeOneToManyOIDRoot insertedRoot = ConfigureDb.da.insert(root);
		Assertions.assertEquals(insertedRoot.otherData, root.otherData);
		Assertions.assertNull(insertedRoot.remoteIds);

		final TypeOneToManyOIDRoot root2 = new TypeOneToManyOIDRoot();
		root2.otherData = "plouf 2";
		final TypeOneToManyOIDRoot insertedRoot2 = ConfigureDb.da.insert(root2);
		Assertions.assertEquals(insertedRoot2.otherData, root2.otherData);
		Assertions.assertNull(insertedRoot2.remoteIds);

		// Create Some Remotes
		final TypeOneToManyOIDRemote remote10 = new TypeOneToManyOIDRemote();
		remote10.data = "remote10";
		remote10.rootOid = insertedRoot.oid;
		final TypeOneToManyOIDRemote insertedRemote10 = ConfigureDb.da.insert(remote10);
		Assertions.assertEquals(insertedRemote10.data, remote10.data);
		Assertions.assertEquals(insertedRemote10.rootOid, remote10.rootOid);

		final TypeOneToManyOIDRemote remote11 = new TypeOneToManyOIDRemote();
		remote11.data = "remote11";
		remote11.rootOid = insertedRoot.oid;
		final TypeOneToManyOIDRemote insertedRemote11 = ConfigureDb.da.insert(remote11);
		Assertions.assertEquals(insertedRemote11.data, remote11.data);
		Assertions.assertEquals(insertedRemote11.rootOid, remote11.rootOid);

		final TypeOneToManyOIDRemote remote20 = new TypeOneToManyOIDRemote();
		remote20.data = "remote20";
		remote20.rootOid = insertedRoot2.oid;
		final TypeOneToManyOIDRemote insertedRemote20 = ConfigureDb.da.insert(remote20);
		Assertions.assertEquals(insertedRemote20.data, remote20.data);
		Assertions.assertEquals(insertedRemote20.rootOid, remote20.rootOid);

		// Check remote are inserted
		final TypeOneToManyOIDRoot retreiveRoot1 = ConfigureDb.da.get(TypeOneToManyOIDRoot.class, insertedRoot.oid);
		Assertions.assertEquals(retreiveRoot1.otherData, insertedRoot.otherData);
		Assertions.assertNotNull(retreiveRoot1.remoteIds);
		Assertions.assertEquals(2, retreiveRoot1.remoteIds.size());
		Assertions.assertEquals(insertedRemote10.oid, retreiveRoot1.remoteIds.get(0));
		Assertions.assertEquals(insertedRemote11.oid, retreiveRoot1.remoteIds.get(1));

		final TypeOneToManyOIDRoot retreiveRoot2 = ConfigureDb.da.get(TypeOneToManyOIDRoot.class, insertedRoot2.oid);
		Assertions.assertEquals(retreiveRoot2.otherData, insertedRoot2.otherData);
		Assertions.assertNotNull(retreiveRoot2.remoteIds);
		Assertions.assertEquals(1, retreiveRoot2.remoteIds.size());
		Assertions.assertEquals(insertedRemote20.oid, retreiveRoot2.remoteIds.get(0));

		// Check remote are inserted and expandable
		final TypeOneToManyOIDRootExpand retreiveRootExpand1 = ConfigureDb.da.get(TypeOneToManyOIDRootExpand.class,
				insertedRoot.oid);
		Assertions.assertEquals(retreiveRootExpand1.otherData, insertedRoot.otherData);
		Assertions.assertNotNull(retreiveRootExpand1.remotes);
		Assertions.assertEquals(2, retreiveRootExpand1.remotes.size());
		Assertions.assertEquals(insertedRemote10.oid, retreiveRootExpand1.remotes.get(0).oid);
		Assertions.assertEquals(insertedRemote10.rootOid, retreiveRootExpand1.remotes.get(0).rootOid);
		Assertions.assertEquals(insertedRemote10.data, retreiveRootExpand1.remotes.get(0).data);
		Assertions.assertEquals(insertedRemote11.oid, retreiveRootExpand1.remotes.get(1).oid);
		Assertions.assertEquals(insertedRemote11.rootOid, retreiveRootExpand1.remotes.get(1).rootOid);
		Assertions.assertEquals(insertedRemote11.data, retreiveRootExpand1.remotes.get(1).data);

		final TypeOneToManyOIDRootExpand retreiveRootExpand2 = ConfigureDb.da.get(TypeOneToManyOIDRootExpand.class,
				insertedRoot2.oid);
		Assertions.assertEquals(retreiveRootExpand2.otherData, insertedRoot2.otherData);
		Assertions.assertNotNull(retreiveRootExpand2.remotes);
		Assertions.assertEquals(1, retreiveRootExpand2.remotes.size());
		Assertions.assertEquals(insertedRemote20.oid, retreiveRootExpand2.remotes.get(0).oid);
		Assertions.assertEquals(insertedRemote20.rootOid, retreiveRootExpand2.remotes.get(0).rootOid);
		Assertions.assertEquals(insertedRemote20.data, retreiveRootExpand2.remotes.get(0).data);

	}

}
