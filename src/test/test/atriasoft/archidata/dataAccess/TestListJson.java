package test.atriasoft.archidata.dataAccess;

import java.io.IOException;
import java.util.ArrayList;

import org.atriasoft.archidata.dataAccess.commonTools.ListInDbTools;
import org.bson.types.ObjectId;
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
import test.atriasoft.archidata.dataAccess.model.SerializeListAsJson;
import test.atriasoft.archidata.dataAccess.model.SerializeListAsJsonObjectId;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestListJson {
	final static private Logger LOGGER = LoggerFactory.getLogger(TestListJson.class);

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
	public void testTableInsertAndRetrieve() throws Exception {}

	@Order(2)
	@Test
	public void testIO() throws Exception {
		final SerializeListAsJson test = new SerializeListAsJson();
		test.data = new ArrayList<>();
		test.data.add(5);
		test.data.add(2);
		test.data.add(8);
		test.data.add(6);
		test.data.add(51);

		final SerializeListAsJson insertedData = ConfigureDb.da.insert(test);

		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.oid);
		Assertions.assertNotNull(insertedData.data);
		Assertions.assertEquals(5, insertedData.data.size());
		Assertions.assertEquals(test.data.get(0), insertedData.data.get(0));
		Assertions.assertEquals(test.data.get(1), insertedData.data.get(1));
		Assertions.assertEquals(test.data.get(2), insertedData.data.get(2));
		Assertions.assertEquals(test.data.get(3), insertedData.data.get(3));
		Assertions.assertEquals(test.data.get(4), insertedData.data.get(4));

		// Try to retrieve all the data:
		final SerializeListAsJson retrieve = ConfigureDb.da.get(SerializeListAsJson.class, insertedData.oid);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.oid);
		Assertions.assertNotNull(retrieve.data);
		Assertions.assertEquals(5, retrieve.data.size());
		Assertions.assertEquals(test.data.get(0), retrieve.data.get(0));
		Assertions.assertEquals(test.data.get(1), retrieve.data.get(1));
		Assertions.assertEquals(test.data.get(2), retrieve.data.get(2));
		Assertions.assertEquals(test.data.get(3), retrieve.data.get(3));
		Assertions.assertEquals(test.data.get(4), retrieve.data.get(4));
	}

	@Order(3)
	@Test
	public void testToolInsert() throws Exception {
		final SerializeListAsJson test = new SerializeListAsJson();
		test.data = new ArrayList<>();

		final SerializeListAsJson insertedData = ConfigureDb.da.insert(test);

		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.oid);
		Assertions.assertNotNull(insertedData.data);
		Assertions.assertEquals(0, insertedData.data.size());

		final Integer firstDataInserted1 = 111;
		final Integer firstDataInserted2 = 222;
		final Integer firstDataInserted3 = 333;

		ListInDbTools.addLink(SerializeListAsJson.class, insertedData.oid, "data", firstDataInserted1);
		ListInDbTools.addLink(SerializeListAsJson.class, insertedData.oid, "data", firstDataInserted2);
		ListInDbTools.addLink(SerializeListAsJson.class, insertedData.oid, "data", firstDataInserted3);

		// Try to retrieve all the data:
		SerializeListAsJson retrieve = ConfigureDb.da.get(SerializeListAsJson.class, insertedData.oid);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.oid);
		Assertions.assertNotNull(retrieve.data);
		Assertions.assertEquals(3, retrieve.data.size());
		Assertions.assertEquals(firstDataInserted1, retrieve.data.get(0));
		Assertions.assertEquals(firstDataInserted2, retrieve.data.get(1));
		Assertions.assertEquals(firstDataInserted3, retrieve.data.get(2));

		ListInDbTools.removeLink(SerializeListAsJson.class, insertedData.oid, "data", firstDataInserted2);
		// Try to retrieve all the data:
		retrieve = ConfigureDb.da.get(SerializeListAsJson.class, insertedData.oid);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.oid);
		Assertions.assertNotNull(retrieve.data);
		Assertions.assertEquals(2, retrieve.data.size());
		Assertions.assertEquals(firstDataInserted1, retrieve.data.get(0));
		Assertions.assertEquals(firstDataInserted3, retrieve.data.get(1));
	}

	@Order(102)
	@Test
	public void testOIDIO() throws Exception {
		final SerializeListAsJsonObjectId test = new SerializeListAsJsonObjectId();
		test.data = new ArrayList<>();
		test.data.add(new ObjectId());
		test.data.add(new ObjectId());
		test.data.add(new ObjectId());
		test.data.add(new ObjectId());
		test.data.add(new ObjectId());

		final SerializeListAsJsonObjectId insertedData = ConfigureDb.da.insert(test);

		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.oid);
		Assertions.assertNotNull(insertedData.data);
		Assertions.assertEquals(5, insertedData.data.size());
		Assertions.assertEquals(test.data.get(0), insertedData.data.get(0));
		Assertions.assertEquals(test.data.get(1), insertedData.data.get(1));
		Assertions.assertEquals(test.data.get(2), insertedData.data.get(2));
		Assertions.assertEquals(test.data.get(3), insertedData.data.get(3));
		Assertions.assertEquals(test.data.get(4), insertedData.data.get(4));

		// Try to retrieve all the data:
		final SerializeListAsJsonObjectId retrieve = ConfigureDb.da.get(SerializeListAsJsonObjectId.class,
				insertedData.oid);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.oid);
		Assertions.assertNotNull(retrieve.data);
		Assertions.assertEquals(5, retrieve.data.size());
		Assertions.assertEquals(test.data.get(0), retrieve.data.get(0));
		Assertions.assertEquals(test.data.get(1), retrieve.data.get(1));
		Assertions.assertEquals(test.data.get(2), retrieve.data.get(2));
		Assertions.assertEquals(test.data.get(3), retrieve.data.get(3));
		Assertions.assertEquals(test.data.get(4), retrieve.data.get(4));
	}

	@Order(103)
	@Test
	public void testOIDToolInsert() throws Exception {
		final SerializeListAsJsonObjectId test = new SerializeListAsJsonObjectId();
		test.data = new ArrayList<>();

		final SerializeListAsJsonObjectId insertedData = ConfigureDb.da.insert(test);

		Assertions.assertNotNull(insertedData);
		Assertions.assertNotNull(insertedData.oid);
		Assertions.assertNotNull(insertedData.data);
		Assertions.assertEquals(0, insertedData.data.size());

		final ObjectId firstDataInserted1 = new ObjectId();
		final ObjectId firstDataInserted2 = new ObjectId();
		final ObjectId firstDataInserted3 = new ObjectId();
		ListInDbTools.addLink(SerializeListAsJsonObjectId.class, insertedData.oid, "data", firstDataInserted1);
		ListInDbTools.addLink(SerializeListAsJsonObjectId.class, insertedData.oid, "data", firstDataInserted2);
		ListInDbTools.addLink(SerializeListAsJsonObjectId.class, insertedData.oid, "data", firstDataInserted3);

		// Try to retrieve all the data:
		SerializeListAsJsonObjectId retrieve = ConfigureDb.da.get(SerializeListAsJsonObjectId.class, insertedData.oid);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.oid);
		Assertions.assertNotNull(retrieve.data);
		Assertions.assertEquals(3, retrieve.data.size());
		Assertions.assertEquals(firstDataInserted1, retrieve.data.get(0));
		Assertions.assertEquals(firstDataInserted2, retrieve.data.get(1));
		Assertions.assertEquals(firstDataInserted3, retrieve.data.get(2));

		ListInDbTools.removeLink(SerializeListAsJsonObjectId.class, insertedData.oid, "data", firstDataInserted2);
		// Try to retrieve all the data:
		retrieve = ConfigureDb.da.get(SerializeListAsJsonObjectId.class, insertedData.oid);

		Assertions.assertNotNull(retrieve);
		Assertions.assertNotNull(retrieve.oid);
		Assertions.assertNotNull(retrieve.data);
		Assertions.assertEquals(2, retrieve.data.size());
		Assertions.assertEquals(firstDataInserted1, retrieve.data.get(0));
		Assertions.assertEquals(firstDataInserted3, retrieve.data.get(1));
	}

}
