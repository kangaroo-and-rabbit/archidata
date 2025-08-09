package test.atriasoft.archidata.dataAccess.SQL;

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
import test.atriasoft.archidata.dataAccess.model.TypeManyToOneLongRemote;
import test.atriasoft.archidata.dataAccess.model.TypeManyToOneLongRoot;
import test.atriasoft.archidata.dataAccess.model.TypeManyToOneLongRootExpand;
import test.atriasoft.archidata.dataAccess.model.TypeManyToOneUUIDRemote;
import test.atriasoft.archidata.dataAccess.model.TypeManyToOneUUIDRoot;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@EnabledIfEnvironmentVariable(named = "INCLUDE_MY_SQL_SPECIFIC", matches = "true")
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

}
