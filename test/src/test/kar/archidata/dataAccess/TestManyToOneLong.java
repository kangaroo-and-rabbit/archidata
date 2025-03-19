package test.kar.archidata.dataAccess;

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
import org.kar.archidata.dataAccess.DBAccessSQL;
import org.kar.archidata.dataAccess.DataFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import test.kar.archidata.ConfigureDb;
import test.kar.archidata.StepwiseExtension;
import test.kar.archidata.dataAccess.model.TypeManyToOneLongRemote;
import test.kar.archidata.dataAccess.model.TypeManyToOneLongRoot;
import test.kar.archidata.dataAccess.model.TypeManyToOneLongRootExpand;
import test.kar.archidata.dataAccess.model.TypeManyToOneUUIDRemote;
import test.kar.archidata.dataAccess.model.TypeManyToOneUUIDRoot;
import test.kar.archidata.dataAccess.model.TypeManyToOneUUIDRootExpand;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestManyToOneLong {
	final static private Logger LOGGER = LoggerFactory.getLogger(TestManyToOneLong.class);

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
		final List<String> sqlCommand = DataFactory.createTable(TypeManyToOneLongRemote.class);
		sqlCommand.addAll(DataFactory.createTable(TypeManyToOneLongRoot.class));
		sqlCommand.addAll(DataFactory.createTable(TypeManyToOneUUIDRoot.class));
		sqlCommand.addAll(DataFactory.createTable(TypeManyToOneUUIDRemote.class));
		if (ConfigureDb.da instanceof final DBAccessSQL daSQL) {
			for (final String elem : sqlCommand) {
				LOGGER.debug("request: '{}'", elem);
				daSQL.executeSimpleQuery(elem);
			}
		}
	}

	@Order(2)
	@Test
	public void testRemoteLong() throws Exception {
		TypeManyToOneLongRemote remote = new TypeManyToOneLongRemote();
		remote.data = "remote1";
		final TypeManyToOneLongRemote insertedRemote1 = ConfigureDb.da.insert(remote);
		Assertions.assertEquals(insertedRemote1.data, remote.data);

		remote = new TypeManyToOneLongRemote();
		remote.data = "remote2";
		final TypeManyToOneLongRemote insertedRemote2 = ConfigureDb.da.insert(remote);
		Assertions.assertEquals(insertedRemote2.data, remote.data);

		final TypeManyToOneLongRoot test = new TypeManyToOneLongRoot();
		test.otherData = "kjhlkjlkj";
		test.remoteId = insertedRemote2.id;
		final TypeManyToOneLongRoot insertedData = ConfigureDb.da.insert(test);
		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.id);
		Assertions.assertTrue(insertedData.id >= 0);
		Assertions.assertEquals(test.otherData, insertedData.otherData);
		Assertions.assertEquals(insertedRemote2.id, insertedData.remoteId);

		TypeManyToOneLongRoot retrieve = ConfigureDb.da.get(TypeManyToOneLongRoot.class, insertedData.id);
		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.id);
		Assertions.assertEquals(insertedData.id, retrieve.id);
		Assertions.assertEquals(insertedData.otherData, retrieve.otherData);
		Assertions.assertEquals(insertedRemote2.id, retrieve.remoteId);

		TypeManyToOneLongRootExpand retrieve2 = ConfigureDb.da.get(TypeManyToOneLongRootExpand.class, insertedData.id);
		Assertions.assertNotNull(retrieve2);
		Assertions.assertNotNull(retrieve2.id);
		Assertions.assertEquals(insertedData.id, retrieve2.id);
		Assertions.assertEquals(insertedData.otherData, retrieve2.otherData);
		Assertions.assertNotNull(retrieve2.remote);
		Assertions.assertEquals(insertedRemote2.id, retrieve2.remote.id);
		Assertions.assertEquals(insertedRemote2.data, retrieve2.remote.data);

		// remove values:
		try {
			final long count = ConfigureDb.da.delete(TypeManyToOneLongRemote.class, insertedRemote2.id);
			Assertions.assertEquals(1L, count);
		} catch (final Exception ex) {
			ex.printStackTrace();
		}
		// check fail:

		retrieve = ConfigureDb.da.get(TypeManyToOneLongRoot.class, insertedData.id);
		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.id);
		Assertions.assertEquals(insertedData.id, retrieve.id);
		Assertions.assertEquals(insertedData.otherData, retrieve.otherData);
		Assertions.assertEquals(insertedRemote2.id, retrieve.remoteId);

		retrieve2 = ConfigureDb.da.get(TypeManyToOneLongRootExpand.class, insertedData.id);
		Assertions.assertNotNull(retrieve2);
		Assertions.assertNotNull(retrieve2.id);
		Assertions.assertEquals(insertedData.id, retrieve2.id);
		Assertions.assertEquals(insertedData.otherData, retrieve2.otherData);
		Assertions.assertNull(retrieve2.remote);
	}

	@Order(3)
	@Test
	public void testRemoteUUID() throws Exception {
		TypeManyToOneUUIDRemote remote = new TypeManyToOneUUIDRemote();
		remote.data = "remote1";
		final TypeManyToOneUUIDRemote insertedRemote1 = ConfigureDb.da.insert(remote);
		Assertions.assertEquals(insertedRemote1.data, remote.data);

		remote = new TypeManyToOneUUIDRemote();
		remote.data = "remote2";
		final TypeManyToOneUUIDRemote insertedRemote2 = ConfigureDb.da.insert(remote);
		Assertions.assertEquals(insertedRemote2.data, remote.data);

		final TypeManyToOneUUIDRoot test = new TypeManyToOneUUIDRoot();
		test.otherData = "kjhlkjlkj";
		test.remoteUuid = insertedRemote2.uuid;
		final TypeManyToOneUUIDRoot insertedData = ConfigureDb.da.insert(test);
		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.uuid);
		Assertions.assertEquals(test.otherData, insertedData.otherData);
		Assertions.assertEquals(insertedRemote2.uuid, insertedData.remoteUuid);

		TypeManyToOneUUIDRoot retrieve = ConfigureDb.da.get(TypeManyToOneUUIDRoot.class, insertedData.uuid);
		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.uuid);
		Assertions.assertEquals(insertedData.uuid, retrieve.uuid);
		Assertions.assertEquals(insertedData.otherData, retrieve.otherData);
		Assertions.assertEquals(insertedRemote2.uuid, retrieve.remoteUuid);

		TypeManyToOneUUIDRootExpand retrieve2 = ConfigureDb.da.get(TypeManyToOneUUIDRootExpand.class,
				insertedData.uuid);
		Assertions.assertNotNull(retrieve2);
		Assertions.assertNotNull(retrieve2.uuid);
		Assertions.assertEquals(insertedData.uuid, retrieve2.uuid);
		Assertions.assertEquals(insertedData.otherData, retrieve2.otherData);
		Assertions.assertNotNull(retrieve2.remote);
		Assertions.assertEquals(insertedRemote2.uuid, retrieve2.remote.uuid);
		Assertions.assertEquals(insertedRemote2.data, retrieve2.remote.data);

		// remove values:
		final long count = ConfigureDb.da.delete(TypeManyToOneUUIDRemote.class, insertedRemote2.uuid);
		Assertions.assertEquals(1, count);

		// check fail:

		retrieve = ConfigureDb.da.get(TypeManyToOneUUIDRoot.class, insertedData.uuid);
		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.uuid);
		Assertions.assertEquals(insertedData.uuid, retrieve.uuid);
		Assertions.assertEquals(insertedData.otherData, retrieve.otherData);
		Assertions.assertEquals(insertedRemote2.uuid, retrieve.remoteUuid);

		retrieve2 = ConfigureDb.da.get(TypeManyToOneUUIDRootExpand.class, insertedData.uuid);
		Assertions.assertNotNull(retrieve2);
		Assertions.assertNotNull(retrieve2.uuid);
		Assertions.assertEquals(insertedData.uuid, retrieve2.uuid);
		Assertions.assertEquals(insertedData.otherData, retrieve2.otherData);
		Assertions.assertNull(retrieve2.remote);
	}
}
