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
import org.kar.archidata.dataAccess.DataAccess;
import org.kar.archidata.dataAccess.DataAccessSQL;
import org.kar.archidata.dataAccess.DataFactory;
import org.kar.archidata.dataAccess.addOn.AddOnManyToMany;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import test.kar.archidata.ConfigureDb;
import test.kar.archidata.StepwiseExtension;
import test.kar.archidata.dataAccess.model.TypeManyToManyRemote;
import test.kar.archidata.dataAccess.model.TypeManyToManyRoot;
import test.kar.archidata.dataAccess.model.TypeManyToManyRootExpand;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestManyToMany {
	final static private Logger LOGGER = LoggerFactory.getLogger(TestManyToMany.class);

	private DataAccess da = null;

	@BeforeAll
	public static void configureWebServer() throws Exception {
		ConfigureDb.configure();
	}

	@AfterAll
	public static void removeDataBase() throws IOException {
		ConfigureDb.clear();
	}

	public TestManyToMany() {
		this.da = DataAccess.createInterface();
	}

	@Order(1)
	@Test
	public void testCreateTable() throws Exception {
		final List<String> sqlCommand2 = DataFactory.createTable(TypeManyToManyRoot.class);
		final List<String> sqlCommand = DataFactory.createTable(TypeManyToManyRemote.class);
		sqlCommand.addAll(sqlCommand2);
		if (this.da instanceof final DataAccessSQL daSQL) {
			for (final String elem : sqlCommand) {
				LOGGER.debug("request: '{}'", elem);
				daSQL.executeSimpleQuery(elem);
			}
		}
	}

	@Order(2)
	@Test
	public void testSimpleInsertAndRetieve() throws Exception {
		final TypeManyToManyRoot test = new TypeManyToManyRoot();
		test.otherData = "kjhlkjlkj";
		final TypeManyToManyRoot insertedData = this.da.insert(test);
		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.id);
		Assertions.assertTrue(insertedData.id >= 0);
		Assertions.assertNull(insertedData.remote);

		// Try to retrieve all the data:
		final TypeManyToManyRoot retrieve = this.da.get(TypeManyToManyRoot.class, insertedData.id);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.id);
		Assertions.assertEquals(insertedData.id, retrieve.id);
		Assertions.assertNotNull(retrieve.otherData);
		Assertions.assertEquals(insertedData.otherData, retrieve.otherData);
		Assertions.assertNull(retrieve.remote);

		this.da.delete(TypeManyToManyRoot.class, insertedData.id);
	}

	@Order(3)
	@Test
	public void testSimpleInsertAndRetieveSubValues() throws Exception {

		TypeManyToManyRemote remote = new TypeManyToManyRemote();
		remote.data = "remote1";
		final TypeManyToManyRemote insertedRemote1 = this.da.insert(remote);
		Assertions.assertEquals(insertedRemote1.data, remote.data);

		remote = new TypeManyToManyRemote();
		remote.data = "remote2";
		final TypeManyToManyRemote insertedRemote2 = this.da.insert(remote);
		Assertions.assertEquals(insertedRemote2.data, remote.data);

		final TypeManyToManyRoot test = new TypeManyToManyRoot();
		test.otherData = "kjhlkjlkj";
		final TypeManyToManyRoot insertedData = this.da.insert(test);
		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.id);
		Assertions.assertTrue(insertedData.id >= 0);
		Assertions.assertNull(insertedData.remote);

		// Try to retrieve all the data:
		TypeManyToManyRoot retrieve = this.da.get(TypeManyToManyRoot.class, insertedData.id);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.id);
		Assertions.assertEquals(insertedData.id, retrieve.id);
		Assertions.assertNotNull(retrieve.otherData);
		Assertions.assertEquals(insertedData.otherData, retrieve.otherData);
		Assertions.assertNull(retrieve.remote);

		// Add remote elements
		AddOnManyToMany.addLink(this.da, TypeManyToManyRoot.class, retrieve.id, "remote", insertedRemote1.id);
		AddOnManyToMany.addLink(this.da, TypeManyToManyRoot.class, retrieve.id, "remote", insertedRemote2.id);

		retrieve = this.da.get(TypeManyToManyRoot.class, insertedData.id);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.id);
		Assertions.assertEquals(insertedData.id, retrieve.id);
		Assertions.assertNotNull(retrieve.otherData);
		Assertions.assertEquals(insertedData.otherData, retrieve.otherData);
		Assertions.assertNotNull(retrieve.remote);
		Assertions.assertEquals(2, retrieve.remote.size());
		Assertions.assertEquals(retrieve.remote.get(0), insertedRemote1.id);
		Assertions.assertEquals(retrieve.remote.get(1), insertedRemote2.id);

		final TypeManyToManyRootExpand retrieveExpand = this.da.get(TypeManyToManyRootExpand.class, insertedData.id);

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
		long count = AddOnManyToMany.removeLink(this.da, TypeManyToManyRoot.class, retrieve.id, "remote",
				insertedRemote1.id);
		Assertions.assertEquals(1, count);

		retrieve = this.da.get(TypeManyToManyRoot.class, insertedData.id);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.id);
		Assertions.assertEquals(insertedData.id, retrieve.id);
		Assertions.assertNotNull(retrieve.otherData);
		Assertions.assertEquals(insertedData.otherData, retrieve.otherData);
		Assertions.assertNotNull(retrieve.remote);
		Assertions.assertEquals(retrieve.remote.size(), 1);
		Assertions.assertEquals(retrieve.remote.get(0), insertedRemote2.id);

		// Remove the second element
		count = AddOnManyToMany.removeLink(this.da, TypeManyToManyRoot.class, retrieve.id, "remote",
				insertedRemote2.id);
		Assertions.assertEquals(1, count);

		retrieve = this.da.get(TypeManyToManyRoot.class, insertedData.id);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.id);
		Assertions.assertEquals(insertedData.id, retrieve.id);
		Assertions.assertNotNull(retrieve.otherData);
		Assertions.assertEquals(insertedData.otherData, retrieve.otherData);
		Assertions.assertNull(retrieve.remote);

		this.da.delete(TypeManyToManyRoot.class, insertedData.id);
	}

	/* API TODO: - Replace list (permet de les ordonnées) - remove all links - delete en cascade .... (compliqué...) */

}