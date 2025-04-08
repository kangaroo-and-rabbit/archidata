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
import test.atriasoft.archidata.dataAccess.model.TypeManyToOneLongRemote;
import test.atriasoft.archidata.dataAccess.model.TypeManyToOneUUIDRemote;
import test.atriasoft.archidata.dataAccess.model.TypeManyToOneUUIDRoot;
import test.atriasoft.archidata.dataAccess.model.TypeManyToOneUUIDRootExpand;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestManyToOneUUID {
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
		sqlCommand.addAll(DataFactory.createTable(TypeManyToOneUUIDRoot.class));
		sqlCommand.addAll(DataFactory.createTable(TypeManyToOneUUIDRemote.class));
		if (ConfigureDb.da instanceof final DBAccessSQL daSQL) {
			for (final String elem : sqlCommand) {
				LOGGER.debug("request: '{}'", elem);
				daSQL.executeSimpleQuery(elem);
			}
		}
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
