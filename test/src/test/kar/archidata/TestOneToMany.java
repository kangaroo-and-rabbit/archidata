package test.kar.archidata;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kar.archidata.GlobalConfiguration;
import org.kar.archidata.dataAccess.DataAccess;
import org.kar.archidata.dataAccess.DataFactory;
import org.kar.archidata.db.DBEntry;
import org.kar.archidata.tools.ConfigBaseVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import test.kar.archidata.model.TypeOneToManyRemote;
import test.kar.archidata.model.TypeOneToManyRoot;
import test.kar.archidata.model.TypeOneToManyRootExpand;
import test.kar.archidata.model.TypeOneToManyUUIDRemote;
import test.kar.archidata.model.TypeOneToManyUUIDRoot;
import test.kar.archidata.model.TypeOneToManyUUIDRootExpand;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestOneToMany {
	final static private Logger LOGGER = LoggerFactory.getLogger(TestOneToMany.class);

	@BeforeAll
	public static void configureWebServer() throws Exception {
		if (!"true".equalsIgnoreCase(System.getenv("TEST_E2E_MODE"))) {
			ConfigBaseVariable.dbType = "sqlite";
			ConfigBaseVariable.dbHost = "memory";
			// for test we need to connect all time the DB
			ConfigBaseVariable.dbKeepConnected = "true";
		}
		// Connect the dataBase...
		final DBEntry entry = DBEntry.createInterface(GlobalConfiguration.dbConfig);
		entry.connect();
	}

	@AfterAll
	public static void removeDataBase() throws IOException {
		LOGGER.info("Remove the test db");
		DBEntry.closeAllForceMode();
		ConfigBaseVariable.clearAllValue();
	}

	@Order(1)
	@Test
	public void testCreateTable() throws Exception {
		final List<String> sqlCommand = DataFactory.createTable(TypeOneToManyRemote.class);
		sqlCommand.addAll(DataFactory.createTable(TypeOneToManyRoot.class));
		sqlCommand.addAll(DataFactory.createTable(TypeOneToManyUUIDRemote.class));
		sqlCommand.addAll(DataFactory.createTable(TypeOneToManyUUIDRoot.class));
		for (final String elem : sqlCommand) {
			LOGGER.debug("request: '{}'", elem);
			DataAccess.executeSimpleQuery(elem);
		}
	}

	@Order(2)
	@Test
	public void testParentLong() throws Exception {
		// create parent:

		final TypeOneToManyRoot root = new TypeOneToManyRoot();
		root.otherData = "plouf";
		final TypeOneToManyRoot insertedRoot = DataAccess.insert(root);
		Assertions.assertEquals(insertedRoot.otherData, root.otherData);
		Assertions.assertNull(insertedRoot.remoteIds);

		final TypeOneToManyRoot root2 = new TypeOneToManyRoot();
		root2.otherData = "plouf 2";
		final TypeOneToManyRoot insertedRoot2 = DataAccess.insert(root2);
		Assertions.assertEquals(insertedRoot2.otherData, root2.otherData);
		Assertions.assertNull(insertedRoot2.remoteIds);

		// Create Some Remotes

		final TypeOneToManyRemote remote10 = new TypeOneToManyRemote();
		remote10.data = "remote10";
		remote10.rootId = insertedRoot.id;
		final TypeOneToManyRemote insertedRemote10 = DataAccess.insert(remote10);
		Assertions.assertEquals(insertedRemote10.data, remote10.data);
		Assertions.assertEquals(insertedRemote10.rootId, remote10.rootId);

		final TypeOneToManyRemote remote11 = new TypeOneToManyRemote();
		remote11.data = "remote11";
		remote11.rootId = insertedRoot.id;
		final TypeOneToManyRemote insertedRemote11 = DataAccess.insert(remote11);
		Assertions.assertEquals(insertedRemote11.data, remote11.data);
		Assertions.assertEquals(insertedRemote11.rootId, remote11.rootId);

		final TypeOneToManyRemote remote20 = new TypeOneToManyRemote();
		remote20.data = "remote20";
		remote20.rootId = insertedRoot2.id;
		final TypeOneToManyRemote insertedRemote20 = DataAccess.insert(remote20);
		Assertions.assertEquals(insertedRemote20.data, remote20.data);
		Assertions.assertEquals(insertedRemote20.rootId, remote20.rootId);

		// Check remote are inserted

		final TypeOneToManyRoot retreiveRoot1 = DataAccess.get(TypeOneToManyRoot.class, insertedRoot.id);
		Assertions.assertEquals(retreiveRoot1.otherData, insertedRoot.otherData);
		Assertions.assertNotNull(retreiveRoot1.remoteIds);
		Assertions.assertEquals(2, retreiveRoot1.remoteIds.size());
		Assertions.assertEquals(insertedRemote10.id, retreiveRoot1.remoteIds.get(0));
		Assertions.assertEquals(insertedRemote11.id, retreiveRoot1.remoteIds.get(1));

		final TypeOneToManyRoot retreiveRoot2 = DataAccess.get(TypeOneToManyRoot.class, insertedRoot2.id);
		Assertions.assertEquals(retreiveRoot2.otherData, insertedRoot2.otherData);
		Assertions.assertNotNull(retreiveRoot2.remoteIds);
		Assertions.assertEquals(1, retreiveRoot2.remoteIds.size());
		Assertions.assertEquals(insertedRemote20.id, retreiveRoot2.remoteIds.get(0));

		// Check remote are inserted and expandable

		final TypeOneToManyRootExpand retreiveRootExpand1 = DataAccess.get(TypeOneToManyRootExpand.class,
				insertedRoot.id);
		Assertions.assertEquals(retreiveRootExpand1.otherData, insertedRoot.otherData);
		Assertions.assertNotNull(retreiveRootExpand1.remotes);
		Assertions.assertEquals(2, retreiveRootExpand1.remotes.size());
		Assertions.assertEquals(insertedRemote10.id, retreiveRootExpand1.remotes.get(0).id);
		Assertions.assertEquals(insertedRemote10.rootId, retreiveRootExpand1.remotes.get(0).rootId);
		Assertions.assertEquals(insertedRemote10.data, retreiveRootExpand1.remotes.get(0).data);
		Assertions.assertEquals(insertedRemote11.id, retreiveRootExpand1.remotes.get(1).id);
		Assertions.assertEquals(insertedRemote11.rootId, retreiveRootExpand1.remotes.get(1).rootId);
		Assertions.assertEquals(insertedRemote11.data, retreiveRootExpand1.remotes.get(1).data);

		final TypeOneToManyRootExpand retreiveRootExpand2 = DataAccess.get(TypeOneToManyRootExpand.class,
				insertedRoot2.id);
		Assertions.assertEquals(retreiveRootExpand2.otherData, insertedRoot2.otherData);
		Assertions.assertNotNull(retreiveRootExpand2.remotes);
		Assertions.assertEquals(1, retreiveRootExpand2.remotes.size());
		Assertions.assertEquals(insertedRemote20.id, retreiveRootExpand2.remotes.get(0).id);
		Assertions.assertEquals(insertedRemote20.rootId, retreiveRootExpand2.remotes.get(0).rootId);
		Assertions.assertEquals(insertedRemote20.data, retreiveRootExpand2.remotes.get(0).data);

	}

	@Order(2)
	@Test
	public void testParentUUID() throws Exception {
		// create parent:

		final TypeOneToManyUUIDRoot root = new TypeOneToManyUUIDRoot();
		root.otherData = "plouf";
		final TypeOneToManyUUIDRoot insertedRoot = DataAccess.insert(root);
		Assertions.assertEquals(insertedRoot.otherData, root.otherData);
		Assertions.assertNull(insertedRoot.remoteIds);

		final TypeOneToManyUUIDRoot root2 = new TypeOneToManyUUIDRoot();
		root2.otherData = "plouf 2";
		final TypeOneToManyUUIDRoot insertedRoot2 = DataAccess.insert(root2);
		Assertions.assertEquals(insertedRoot2.otherData, root2.otherData);
		Assertions.assertNull(insertedRoot2.remoteIds);

		// Create Some Remotes

		final TypeOneToManyUUIDRemote remote10 = new TypeOneToManyUUIDRemote();
		remote10.data = "remote10";
		remote10.rootUuid = insertedRoot.uuid;
		final TypeOneToManyUUIDRemote insertedRemote10 = DataAccess.insert(remote10);
		Assertions.assertEquals(insertedRemote10.data, remote10.data);
		Assertions.assertEquals(insertedRemote10.rootUuid, remote10.rootUuid);

		final TypeOneToManyUUIDRemote remote11 = new TypeOneToManyUUIDRemote();
		remote11.data = "remote11";
		remote11.rootUuid = insertedRoot.uuid;
		final TypeOneToManyUUIDRemote insertedRemote11 = DataAccess.insert(remote11);
		Assertions.assertEquals(insertedRemote11.data, remote11.data);
		Assertions.assertEquals(insertedRemote11.rootUuid, remote11.rootUuid);

		final TypeOneToManyUUIDRemote remote20 = new TypeOneToManyUUIDRemote();
		remote20.data = "remote20";
		remote20.rootUuid = insertedRoot2.uuid;
		final TypeOneToManyUUIDRemote insertedRemote20 = DataAccess.insert(remote20);
		Assertions.assertEquals(insertedRemote20.data, remote20.data);
		Assertions.assertEquals(insertedRemote20.rootUuid, remote20.rootUuid);

		// Check remote are inserted

		final TypeOneToManyUUIDRoot retreiveRoot1 = DataAccess.get(TypeOneToManyUUIDRoot.class, insertedRoot.uuid);
		Assertions.assertEquals(retreiveRoot1.otherData, insertedRoot.otherData);
		Assertions.assertNotNull(retreiveRoot1.remoteIds);
		Assertions.assertEquals(2, retreiveRoot1.remoteIds.size());
		Assertions.assertEquals(insertedRemote10.uuid, retreiveRoot1.remoteIds.get(0));
		Assertions.assertEquals(insertedRemote11.uuid, retreiveRoot1.remoteIds.get(1));

		final TypeOneToManyUUIDRoot retreiveRoot2 = DataAccess.get(TypeOneToManyUUIDRoot.class, insertedRoot2.uuid);
		Assertions.assertEquals(retreiveRoot2.otherData, insertedRoot2.otherData);
		Assertions.assertNotNull(retreiveRoot2.remoteIds);
		Assertions.assertEquals(1, retreiveRoot2.remoteIds.size());
		Assertions.assertEquals(insertedRemote20.uuid, retreiveRoot2.remoteIds.get(0));

		// Check remote are inserted and expandable

		final TypeOneToManyUUIDRootExpand retreiveRootExpand1 = DataAccess.get(TypeOneToManyUUIDRootExpand.class,
				insertedRoot.uuid);
		Assertions.assertEquals(retreiveRootExpand1.otherData, insertedRoot.otherData);
		Assertions.assertNotNull(retreiveRootExpand1.remotes);
		Assertions.assertEquals(2, retreiveRootExpand1.remotes.size());
		Assertions.assertEquals(insertedRemote10.uuid, retreiveRootExpand1.remotes.get(0).uuid);
		Assertions.assertEquals(insertedRemote10.rootUuid, retreiveRootExpand1.remotes.get(0).rootUuid);
		Assertions.assertEquals(insertedRemote10.data, retreiveRootExpand1.remotes.get(0).data);
		Assertions.assertEquals(insertedRemote11.uuid, retreiveRootExpand1.remotes.get(1).uuid);
		Assertions.assertEquals(insertedRemote11.rootUuid, retreiveRootExpand1.remotes.get(1).rootUuid);
		Assertions.assertEquals(insertedRemote11.data, retreiveRootExpand1.remotes.get(1).data);

		final TypeOneToManyUUIDRootExpand retreiveRootExpand2 = DataAccess.get(TypeOneToManyUUIDRootExpand.class,
				insertedRoot2.uuid);
		Assertions.assertEquals(retreiveRootExpand2.otherData, insertedRoot2.otherData);
		Assertions.assertNotNull(retreiveRootExpand2.remotes);
		Assertions.assertEquals(1, retreiveRootExpand2.remotes.size());
		Assertions.assertEquals(insertedRemote20.uuid, retreiveRootExpand2.remotes.get(0).uuid);
		Assertions.assertEquals(insertedRemote20.rootUuid, retreiveRootExpand2.remotes.get(0).rootUuid);
		Assertions.assertEquals(insertedRemote20.data, retreiveRootExpand2.remotes.get(0).data);

	}
}