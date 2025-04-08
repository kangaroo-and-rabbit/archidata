package test.atriasoft.archidata.dataAccess;

import java.io.IOException;
import java.util.List;

import org.atriasoft.archidata.dataAccess.DBAccessSQL;
import org.atriasoft.archidata.dataAccess.DataFactory;
import org.atriasoft.archidata.dataAccess.addOnSQL.AddOnManyToMany;
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
import test.atriasoft.archidata.dataAccess.model.TypeManyToManyLongRemote;
import test.atriasoft.archidata.dataAccess.model.TypeManyToManyLongRoot;
import test.atriasoft.archidata.dataAccess.model.TypeManyToManyLongRootExpand;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestManyToManyLong {
	final static private Logger LOGGER = LoggerFactory.getLogger(TestManyToManyLong.class);

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
		final List<String> sqlCommand2 = DataFactory.createTable(TypeManyToManyLongRoot.class);
		final List<String> sqlCommand = DataFactory.createTable(TypeManyToManyLongRemote.class);
		sqlCommand.addAll(sqlCommand2);
		if (ConfigureDb.da instanceof final DBAccessSQL daSQL) {
			for (final String elem : sqlCommand) {
				LOGGER.debug("request: '{}'", elem);
				daSQL.executeSimpleQuery(elem);
			}
		}
	}

	@Order(2)
	@Test
	public void testSimpleInsertAndRetieve() throws Exception {
		final TypeManyToManyLongRoot test = new TypeManyToManyLongRoot();
		test.otherData = "kjhlkjlkj";
		final TypeManyToManyLongRoot insertedData = ConfigureDb.da.insert(test);
		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.id);
		Assertions.assertTrue(insertedData.id >= 0);
		Assertions.assertNull(insertedData.remote);

		// Try to retrieve all the data:
		final TypeManyToManyLongRoot retrieve = ConfigureDb.da.get(TypeManyToManyLongRoot.class, insertedData.id);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.id);
		Assertions.assertEquals(insertedData.id, retrieve.id);
		Assertions.assertNotNull(retrieve.otherData);
		Assertions.assertEquals(insertedData.otherData, retrieve.otherData);
		Assertions.assertNull(retrieve.remote);

		ConfigureDb.da.delete(TypeManyToManyLongRoot.class, insertedData.id);
	}

	@Order(3)
	@Test
	public void testSimpleInsertAndRetieveSubValues() throws Exception {

		TypeManyToManyLongRemote remote = new TypeManyToManyLongRemote();
		remote.data = "remote1";
		final TypeManyToManyLongRemote insertedRemote1 = ConfigureDb.da.insert(remote);
		Assertions.assertEquals(insertedRemote1.data, remote.data);

		remote = new TypeManyToManyLongRemote();
		remote.data = "remote2";
		final TypeManyToManyLongRemote insertedRemote2 = ConfigureDb.da.insert(remote);
		Assertions.assertEquals(insertedRemote2.data, remote.data);

		final TypeManyToManyLongRoot test = new TypeManyToManyLongRoot();
		test.otherData = "kjhlkjlkj";
		final TypeManyToManyLongRoot insertedData = ConfigureDb.da.insert(test);
		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.id);
		Assertions.assertTrue(insertedData.id >= 0);
		Assertions.assertNull(insertedData.remote);

		// Try to retrieve all the data:
		TypeManyToManyLongRoot retrieve = ConfigureDb.da.get(TypeManyToManyLongRoot.class, insertedData.id);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.id);
		Assertions.assertEquals(insertedData.id, retrieve.id);
		Assertions.assertNotNull(retrieve.otherData);
		Assertions.assertEquals(insertedData.otherData, retrieve.otherData);
		Assertions.assertNull(retrieve.remote);

		// Add remote elements
		AddOnManyToMany.addLink(ConfigureDb.da, TypeManyToManyLongRoot.class, retrieve.id, "remote",
				insertedRemote1.id);
		AddOnManyToMany.addLink(ConfigureDb.da, TypeManyToManyLongRoot.class, retrieve.id, "remote",
				insertedRemote2.id);

		retrieve = ConfigureDb.da.get(TypeManyToManyLongRoot.class, insertedData.id);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.id);
		Assertions.assertEquals(insertedData.id, retrieve.id);
		Assertions.assertNotNull(retrieve.otherData);
		Assertions.assertEquals(insertedData.otherData, retrieve.otherData);
		Assertions.assertNotNull(retrieve.remote);
		Assertions.assertEquals(2, retrieve.remote.size());
		Assertions.assertEquals(retrieve.remote.get(0), insertedRemote1.id);
		Assertions.assertEquals(retrieve.remote.get(1), insertedRemote2.id);

		final TypeManyToManyLongRootExpand retrieveExpand = ConfigureDb.da.get(TypeManyToManyLongRootExpand.class,
				insertedData.id);

		Assertions.assertNotNull(retrieveExpand);
		Assertions.assertNotNull(retrieveExpand.id);
		Assertions.assertEquals(insertedData.id, retrieveExpand.id);
		Assertions.assertNotNull(retrieveExpand.otherData);
		Assertions.assertEquals(insertedData.otherData, retrieveExpand.otherData);
		Assertions.assertNotNull(retrieveExpand.remote);
		Assertions.assertEquals(2, retrieveExpand.remote.size());
		Assertions.assertEquals(retrieveExpand.remote.get(0).id, insertedRemote1.id);
		Assertions.assertEquals(retrieveExpand.remote.get(1).id, insertedRemote2.id);

		// Remove an element
		long count = AddOnManyToMany.removeLink(ConfigureDb.da, TypeManyToManyLongRoot.class, retrieve.id, "remote",
				insertedRemote1.id);
		Assertions.assertEquals(1, count);

		retrieve = ConfigureDb.da.get(TypeManyToManyLongRoot.class, insertedData.id);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.id);
		Assertions.assertEquals(insertedData.id, retrieve.id);
		Assertions.assertNotNull(retrieve.otherData);
		Assertions.assertEquals(insertedData.otherData, retrieve.otherData);
		Assertions.assertNotNull(retrieve.remote);
		Assertions.assertEquals(retrieve.remote.size(), 1);
		Assertions.assertEquals(retrieve.remote.get(0), insertedRemote2.id);

		// Remove the second element
		count = AddOnManyToMany.removeLink(ConfigureDb.da, TypeManyToManyLongRoot.class, retrieve.id, "remote",
				insertedRemote2.id);
		Assertions.assertEquals(1, count);

		retrieve = ConfigureDb.da.get(TypeManyToManyLongRoot.class, insertedData.id);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.id);
		Assertions.assertEquals(insertedData.id, retrieve.id);
		Assertions.assertNotNull(retrieve.otherData);
		Assertions.assertEquals(insertedData.otherData, retrieve.otherData);
		Assertions.assertNull(retrieve.remote);

		ConfigureDb.da.delete(TypeManyToManyLongRoot.class, insertedData.id);
	}

	/* API TODO: - Replace list (permet de les ordonnées) - remove all links - delete en cascade .... (compliqué...) */

}
