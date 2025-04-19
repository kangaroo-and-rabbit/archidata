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
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import test.atriasoft.archidata.ConfigureDb;
import test.atriasoft.archidata.StepwiseExtension;
import test.atriasoft.archidata.dataAccess.model.TypeOneToManyLongRemote;
import test.atriasoft.archidata.dataAccess.model.TypeOneToManyUUIDRemote;
import test.atriasoft.archidata.dataAccess.model.TypeOneToManyUUIDRoot;
import test.atriasoft.archidata.dataAccess.model.TypeOneToManyUUIDRootExpand;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestOneToManyUUID {
	final static private Logger LOGGER = LoggerFactory.getLogger(TestOneToManyLong.class);

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
		final List<String> sqlCommand = DataFactory.createTable(TypeOneToManyLongRemote.class);
		sqlCommand.addAll(DataFactory.createTable(TypeOneToManyUUIDRoot.class));
		sqlCommand.addAll(DataFactory.createTable(TypeOneToManyUUIDRemote.class));
		if (ConfigureDb.da instanceof final DBAccessSQL daSQL) {
			for (final String elem : sqlCommand) {
				LOGGER.debug("request: '{}'", elem);
				daSQL.executeSimpleQuery(elem);
			}
		}
	}

	@Order(2)
	@Test
	public void testParentUUID() throws Exception {
		// create parent:

		final TypeOneToManyUUIDRoot root = new TypeOneToManyUUIDRoot();
		root.otherData = "plouf";
		final TypeOneToManyUUIDRoot insertedRoot = ConfigureDb.da.insert(root);
		Assertions.assertEquals(insertedRoot.otherData, root.otherData);
		Assertions.assertNull(insertedRoot.remoteIds);

		final TypeOneToManyUUIDRoot root2 = new TypeOneToManyUUIDRoot();
		root2.otherData = "plouf 2";
		final TypeOneToManyUUIDRoot insertedRoot2 = ConfigureDb.da.insert(root2);
		Assertions.assertEquals(insertedRoot2.otherData, root2.otherData);
		Assertions.assertNull(insertedRoot2.remoteIds);

		// Create Some Remotes

		final TypeOneToManyUUIDRemote remote10 = new TypeOneToManyUUIDRemote();
		remote10.data = "remote10";
		remote10.rootUuid = insertedRoot.uuid;
		final TypeOneToManyUUIDRemote insertedRemote10 = ConfigureDb.da.insert(remote10);
		Assertions.assertEquals(insertedRemote10.data, remote10.data);
		Assertions.assertEquals(insertedRemote10.rootUuid, remote10.rootUuid);

		final TypeOneToManyUUIDRemote remote11 = new TypeOneToManyUUIDRemote();
		remote11.data = "remote11";
		remote11.rootUuid = insertedRoot.uuid;
		final TypeOneToManyUUIDRemote insertedRemote11 = ConfigureDb.da.insert(remote11);
		Assertions.assertEquals(insertedRemote11.data, remote11.data);
		Assertions.assertEquals(insertedRemote11.rootUuid, remote11.rootUuid);

		final TypeOneToManyUUIDRemote remote20 = new TypeOneToManyUUIDRemote();
		remote20.data = "remote20";
		remote20.rootUuid = insertedRoot2.uuid;
		final TypeOneToManyUUIDRemote insertedRemote20 = ConfigureDb.da.insert(remote20);
		Assertions.assertEquals(insertedRemote20.data, remote20.data);
		Assertions.assertEquals(insertedRemote20.rootUuid, remote20.rootUuid);

		// Check remote are inserted

		final TypeOneToManyUUIDRoot retreiveRoot1 = ConfigureDb.da.get(TypeOneToManyUUIDRoot.class, insertedRoot.uuid);
		Assertions.assertEquals(retreiveRoot1.otherData, insertedRoot.otherData);
		Assertions.assertNotNull(retreiveRoot1.remoteIds);
		Assertions.assertEquals(2, retreiveRoot1.remoteIds.size());
		Assertions.assertEquals(insertedRemote10.uuid, retreiveRoot1.remoteIds.get(0));
		Assertions.assertEquals(insertedRemote11.uuid, retreiveRoot1.remoteIds.get(1));

		final TypeOneToManyUUIDRoot retreiveRoot2 = ConfigureDb.da.get(TypeOneToManyUUIDRoot.class, insertedRoot2.uuid);
		Assertions.assertEquals(retreiveRoot2.otherData, insertedRoot2.otherData);
		Assertions.assertNotNull(retreiveRoot2.remoteIds);
		Assertions.assertEquals(1, retreiveRoot2.remoteIds.size());
		Assertions.assertEquals(insertedRemote20.uuid, retreiveRoot2.remoteIds.get(0));

		// Check remote are inserted and expandable

		final TypeOneToManyUUIDRootExpand retreiveRootExpand1 = ConfigureDb.da.get(TypeOneToManyUUIDRootExpand.class,
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

		final TypeOneToManyUUIDRootExpand retreiveRootExpand2 = ConfigureDb.da.get(TypeOneToManyUUIDRootExpand.class,
				insertedRoot2.uuid);
		Assertions.assertEquals(retreiveRootExpand2.otherData, insertedRoot2.otherData);
		Assertions.assertNotNull(retreiveRootExpand2.remotes);
		Assertions.assertEquals(1, retreiveRootExpand2.remotes.size());
		Assertions.assertEquals(insertedRemote20.uuid, retreiveRootExpand2.remotes.get(0).uuid);
		Assertions.assertEquals(insertedRemote20.rootUuid, retreiveRootExpand2.remotes.get(0).rootUuid);
		Assertions.assertEquals(insertedRemote20.data, retreiveRootExpand2.remotes.get(0).data);

	}
}
