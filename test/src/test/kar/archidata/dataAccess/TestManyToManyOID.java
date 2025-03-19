package test.kar.archidata.dataAccess;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
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
import org.kar.archidata.dataAccess.addOnSQL.AddOnManyToMany;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import test.kar.archidata.ConfigureDb;
import test.kar.archidata.StepwiseExtension;
import test.kar.archidata.dataAccess.model.TypeManyToManyOIDRemote;
import test.kar.archidata.dataAccess.model.TypeManyToManyOIDRoot;
import test.kar.archidata.dataAccess.model.TypeManyToManyOIDRootExpand;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestManyToManyOID {
	final static private Logger LOGGER = LoggerFactory.getLogger(TestManyToManyOID.class);

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
		final List<String> sqlCommand2 = DataFactory.createTable(TypeManyToManyOIDRoot.class);
		final List<String> sqlCommand = DataFactory.createTable(TypeManyToManyOIDRemote.class);
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
		final Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.MINUTE, -1);
		final Date beginTestDate = calendar.getTime();
		final TypeManyToManyOIDRoot test = new TypeManyToManyOIDRoot();
		test.otherData = "kjhlkjlkj";
		final TypeManyToManyOIDRoot insertedData = ConfigureDb.da.insert(test);
		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.oid);
		Assertions.assertTrue(insertedData.oid.getDate().after(beginTestDate));
		Assertions.assertNull(insertedData.remote);

		// Try to retrieve all the data:
		final TypeManyToManyOIDRoot retrieve = ConfigureDb.da.get(TypeManyToManyOIDRoot.class, insertedData.oid);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.oid);
		Assertions.assertEquals(insertedData.oid, retrieve.oid);
		Assertions.assertNotNull(retrieve.otherData);
		Assertions.assertEquals(insertedData.otherData, retrieve.otherData);
		Assertions.assertNull(retrieve.remote);

		ConfigureDb.da.delete(TypeManyToManyOIDRoot.class, insertedData.oid);
	}

	@Order(3)
	@Test
	public void testSimpleInsertAndRetieveSubValues() throws Exception {
		final Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.MINUTE, -1);
		final Date beginTestDate = calendar.getTime();
		TypeManyToManyOIDRemote remote = new TypeManyToManyOIDRemote();
		remote.data = "remote1";
		final TypeManyToManyOIDRemote insertedRemote1 = ConfigureDb.da.insert(remote);
		Assertions.assertEquals(insertedRemote1.data, remote.data);

		remote = new TypeManyToManyOIDRemote();
		remote.data = "remote2";
		final TypeManyToManyOIDRemote insertedRemote2 = ConfigureDb.da.insert(remote);
		Assertions.assertEquals(insertedRemote2.data, remote.data);

		final TypeManyToManyOIDRoot test = new TypeManyToManyOIDRoot();
		test.otherData = "kjhlkjlkj";
		final TypeManyToManyOIDRoot insertedData = ConfigureDb.da.insert(test);
		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.oid);
		Assertions.assertTrue(insertedData.oid.getDate().after(beginTestDate));
		Assertions.assertNull(insertedData.remote);

		// Try to retrieve all the data:
		TypeManyToManyOIDRoot retrieve = ConfigureDb.da.get(TypeManyToManyOIDRoot.class, insertedData.oid);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.oid);
		Assertions.assertEquals(insertedData.oid, retrieve.oid);
		Assertions.assertNotNull(retrieve.otherData);
		Assertions.assertEquals(insertedData.otherData, retrieve.otherData);
		Assertions.assertNull(retrieve.remote);

		// Add remote elements
		AddOnManyToMany.addLink(ConfigureDb.da, TypeManyToManyOIDRoot.class, retrieve.oid, "remote",
				insertedRemote1.oid);
		AddOnManyToMany.addLink(ConfigureDb.da, TypeManyToManyOIDRoot.class, retrieve.oid, "remote",
				insertedRemote2.oid);

		retrieve = ConfigureDb.da.get(TypeManyToManyOIDRoot.class, insertedData.oid);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.oid);
		Assertions.assertEquals(insertedData.oid, retrieve.oid);
		Assertions.assertNotNull(retrieve.otherData);
		Assertions.assertEquals(insertedData.otherData, retrieve.otherData);
		Assertions.assertNotNull(retrieve.remote);
		Assertions.assertEquals(2, retrieve.remote.size());
		Assertions.assertEquals(retrieve.remote.get(0), insertedRemote1.oid);
		Assertions.assertEquals(retrieve.remote.get(1), insertedRemote2.oid);

		final TypeManyToManyOIDRootExpand retrieveExpand = ConfigureDb.da.get(TypeManyToManyOIDRootExpand.class,
				insertedData.oid);

		Assertions.assertNotNull(retrieveExpand);
		Assertions.assertNotNull(retrieveExpand.oid);
		Assertions.assertEquals(insertedData.oid, retrieveExpand.oid);
		Assertions.assertNotNull(retrieveExpand.otherData);
		Assertions.assertEquals(insertedData.otherData, retrieveExpand.otherData);
		Assertions.assertNotNull(retrieveExpand.remote);
		Assertions.assertEquals(2, retrieveExpand.remote.size());
		Assertions.assertEquals(retrieveExpand.remote.get(0).oid, insertedRemote1.oid);
		Assertions.assertEquals(retrieveExpand.remote.get(1).oid, insertedRemote2.oid);

		// Remove an element
		long count = AddOnManyToMany.removeLink(ConfigureDb.da, TypeManyToManyOIDRoot.class, retrieve.oid, "remote",
				insertedRemote1.oid);
		Assertions.assertEquals(1, count);

		retrieve = ConfigureDb.da.get(TypeManyToManyOIDRoot.class, insertedData.oid);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.oid);
		Assertions.assertEquals(insertedData.oid, retrieve.oid);
		Assertions.assertNotNull(retrieve.otherData);
		Assertions.assertEquals(insertedData.otherData, retrieve.otherData);
		Assertions.assertNotNull(retrieve.remote);
		Assertions.assertEquals(retrieve.remote.size(), 1);
		Assertions.assertEquals(retrieve.remote.get(0), insertedRemote2.oid);

		// Remove the second element
		count = AddOnManyToMany.removeLink(ConfigureDb.da, TypeManyToManyOIDRoot.class, retrieve.oid, "remote",
				insertedRemote2.oid);
		Assertions.assertEquals(1, count);

		retrieve = ConfigureDb.da.get(TypeManyToManyOIDRoot.class, insertedData.oid);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.oid);
		Assertions.assertEquals(insertedData.oid, retrieve.oid);
		Assertions.assertNotNull(retrieve.otherData);
		Assertions.assertEquals(insertedData.otherData, retrieve.otherData);
		Assertions.assertNull(retrieve.remote);

		ConfigureDb.da.delete(TypeManyToManyOIDRoot.class, insertedData.oid);
	}

	/* API TODO: - Replace list (permet de les ordonnées) - remove all links - delete en cascade .... (compliqué...) */

}
