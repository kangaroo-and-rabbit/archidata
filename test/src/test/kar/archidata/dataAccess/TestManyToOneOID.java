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
import test.kar.archidata.dataAccess.model.TypeManyToOneOIDRemote;
import test.kar.archidata.dataAccess.model.TypeManyToOneOIDRoot;
import test.kar.archidata.dataAccess.model.TypeManyToOneOIDRootExpand;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestManyToOneOID {
	final static private Logger LOGGER = LoggerFactory.getLogger(TestManyToOneOID.class);

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
		final List<String> sqlCommand = DataFactory.createTable(TypeManyToOneOIDRemote.class);
		sqlCommand.addAll(DataFactory.createTable(TypeManyToOneOIDRoot.class));
		if (ConfigureDb.da instanceof final DBAccessSQL daSQL) {
			for (final String elem : sqlCommand) {
				LOGGER.debug("request: '{}'", elem);
				daSQL.executeSimpleQuery(elem);
			}
		}
	}

	@Order(3)
	@Test
	public void testRemoteOID() throws Exception {
		TypeManyToOneOIDRemote remote = new TypeManyToOneOIDRemote();
		remote.data = "remote1";
		final TypeManyToOneOIDRemote insertedRemote1 = ConfigureDb.da.insert(remote);
		Assertions.assertEquals(insertedRemote1.data, remote.data);

		remote = new TypeManyToOneOIDRemote();
		remote.data = "remote2";
		final TypeManyToOneOIDRemote insertedRemote2 = ConfigureDb.da.insert(remote);
		Assertions.assertEquals(insertedRemote2.data, remote.data);

		final TypeManyToOneOIDRoot test = new TypeManyToOneOIDRoot();
		test.otherData = "kjhlkjlkj";
		test.remoteOid = insertedRemote2.oid;
		final TypeManyToOneOIDRoot insertedData = ConfigureDb.da.insert(test);
		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.oid);
		Assertions.assertEquals(test.otherData, insertedData.otherData);
		Assertions.assertEquals(insertedRemote2.oid, insertedData.remoteOid);

		TypeManyToOneOIDRoot retrieve = ConfigureDb.da.get(TypeManyToOneOIDRoot.class, insertedData.oid);
		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.oid);
		Assertions.assertEquals(insertedData.oid, retrieve.oid);
		Assertions.assertEquals(insertedData.otherData, retrieve.otherData);
		Assertions.assertEquals(insertedRemote2.oid, retrieve.remoteOid);

		TypeManyToOneOIDRootExpand retrieve2 = ConfigureDb.da.get(TypeManyToOneOIDRootExpand.class, insertedData.oid);
		Assertions.assertNotNull(retrieve2);
		Assertions.assertNotNull(retrieve2.oid);
		Assertions.assertEquals(insertedData.oid, retrieve2.oid);
		Assertions.assertEquals(insertedData.otherData, retrieve2.otherData);
		Assertions.assertNotNull(retrieve2.remote);
		Assertions.assertEquals(insertedRemote2.oid, retrieve2.remote.oid);
		Assertions.assertEquals(insertedRemote2.data, retrieve2.remote.data);

		// remove values:
		final long count = ConfigureDb.da.delete(TypeManyToOneOIDRemote.class, insertedRemote2.oid);
		Assertions.assertEquals(1, count);

		// check fail:

		retrieve = ConfigureDb.da.get(TypeManyToOneOIDRoot.class, insertedData.oid);
		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.oid);
		Assertions.assertEquals(insertedData.oid, retrieve.oid);
		Assertions.assertEquals(insertedData.otherData, retrieve.otherData);
		Assertions.assertEquals(insertedRemote2.oid, retrieve.remoteOid);

		retrieve2 = ConfigureDb.da.get(TypeManyToOneOIDRootExpand.class, insertedData.oid);
		Assertions.assertNotNull(retrieve2);
		Assertions.assertNotNull(retrieve2.oid);
		Assertions.assertEquals(insertedData.oid, retrieve2.oid);
		Assertions.assertEquals(insertedData.otherData, retrieve2.otherData);
		Assertions.assertNull(retrieve2.remote);
	}
}
