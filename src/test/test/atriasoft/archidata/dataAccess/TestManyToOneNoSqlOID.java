package test.atriasoft.archidata.dataAccess;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.List;

import org.atriasoft.archidata.dataAccess.DBAccessSQL;
import org.atriasoft.archidata.dataAccess.DataFactory;
import org.atriasoft.archidata.dataAccess.options.AccessDeletedItems;
import org.atriasoft.archidata.dataAccess.options.ReadAllColumn;
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
import test.atriasoft.archidata.dataAccess.model.TypeManyToOneNoSqlOIDRemote;
import test.atriasoft.archidata.dataAccess.model.TypeManyToOneNoSqlOIDRoot;
import test.atriasoft.archidata.dataAccess.model.TypeManyToOneNoSqlOIDRootExpand;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@EnabledIfEnvironmentVariable(named = "INCLUDE_MONGO_SPECIFIC", matches = "true")
public class TestManyToOneNoSqlOID {
	final static private Logger LOGGER = LoggerFactory.getLogger(TestManyToOneNoSqlOID.class);

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
		final List<String> sqlCommand = DataFactory.createTable(TypeManyToOneNoSqlOIDRemote.class);
		sqlCommand.addAll(DataFactory.createTable(TypeManyToOneNoSqlOIDRoot.class));
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
		TypeManyToOneNoSqlOIDRemote remote = new TypeManyToOneNoSqlOIDRemote();
		remote.data = "remote1";
		final TypeManyToOneNoSqlOIDRemote insertedRemote1 = ConfigureDb.da.insert(remote);
		Assertions.assertEquals(insertedRemote1.data, remote.data);

		remote = new TypeManyToOneNoSqlOIDRemote();
		remote.data = "remote2";
		final TypeManyToOneNoSqlOIDRemote insertedRemote2 = ConfigureDb.da.insert(remote);
		Assertions.assertEquals(insertedRemote2.data, remote.data);
		Thread.sleep(500);
		final TypeManyToOneNoSqlOIDRoot test = new TypeManyToOneNoSqlOIDRoot();
		test.otherData = "kjhlkjlkj";
		test.remoteOid = insertedRemote2.oid;
		final TypeManyToOneNoSqlOIDRoot insertedData = ConfigureDb.da.insert(test);
		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.oid);
		Assertions.assertEquals(test.otherData, insertedData.otherData);
		Assertions.assertEquals(insertedRemote2.oid, insertedData.remoteOid);

		TypeManyToOneNoSqlOIDRoot retrieve = ConfigureDb.da.get(TypeManyToOneNoSqlOIDRoot.class, insertedData.oid);
		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.oid);
		Assertions.assertEquals(insertedData.oid, retrieve.oid);
		Assertions.assertEquals(insertedData.otherData, retrieve.otherData);
		Assertions.assertEquals(insertedRemote2.oid, retrieve.remoteOid);

		TypeManyToOneNoSqlOIDRootExpand retrieve2 = ConfigureDb.da.get(TypeManyToOneNoSqlOIDRootExpand.class,
				insertedData.oid);
		Assertions.assertNotNull(retrieve2);
		Assertions.assertNotNull(retrieve2.oid);
		Assertions.assertEquals(insertedData.oid, retrieve2.oid);
		Assertions.assertEquals(insertedData.otherData, retrieve2.otherData);
		Assertions.assertNotNull(retrieve2.remote);
		Assertions.assertEquals(insertedRemote2.oid, retrieve2.remote.oid);
		Assertions.assertEquals(insertedRemote2.data, retrieve2.remote.data);

		final TypeManyToOneNoSqlOIDRemote remoteCheck = ConfigureDb.da.get(TypeManyToOneNoSqlOIDRemote.class,
				insertedRemote2.oid, new AccessDeletedItems(), new ReadAllColumn());
		Assertions.assertNotNull(remoteCheck);
		Assertions.assertNotNull(remoteCheck.oid);
		Assertions.assertEquals(insertedRemote2.oid, remoteCheck.oid);
		Assertions.assertNotNull(remoteCheck.remoteOids);
		Assertions.assertEquals(1, remoteCheck.remoteOids.size());
		Assertions.assertEquals(insertedData.oid, remoteCheck.remoteOids.get(0));
		Assertions.assertNotNull(remoteCheck.createdAt);
		Assertions.assertNotNull(remoteCheck.updatedAt);
		final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
		final String formattedCreatedAt = sdf.format(remoteCheck.createdAt);
		final String formattedUpdatedAt = sdf.format(remoteCheck.updatedAt);
		LOGGER.info("check: {} =?= {}", formattedCreatedAt, formattedUpdatedAt);
		Assertions.assertTrue(formattedUpdatedAt.compareTo(formattedCreatedAt) > 0);

		// remove values:
		final long count = ConfigureDb.da.delete(TypeManyToOneNoSqlOIDRemote.class, insertedRemote2.oid);
		Assertions.assertEquals(1, count);

		// check fail:

		retrieve = ConfigureDb.da.get(TypeManyToOneNoSqlOIDRoot.class, insertedData.oid);
		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.oid);
		Assertions.assertEquals(insertedData.oid, retrieve.oid);
		Assertions.assertEquals(insertedData.otherData, retrieve.otherData);
		Assertions.assertEquals(insertedRemote2.oid, retrieve.remoteOid);

		retrieve2 = ConfigureDb.da.get(TypeManyToOneNoSqlOIDRootExpand.class, insertedData.oid);
		Assertions.assertNotNull(retrieve2);
		Assertions.assertNotNull(retrieve2.oid);
		Assertions.assertEquals(insertedData.oid, retrieve2.oid);
		Assertions.assertEquals(insertedData.otherData, retrieve2.otherData);
		Assertions.assertNull(retrieve2.remote);
	}
}
