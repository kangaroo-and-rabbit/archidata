package test.atriasoft.archidata.dataAccess.SQL;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.atriasoft.archidata.dataAccess.DBAccessSQL;
import org.atriasoft.archidata.dataAccess.DataFactory;
import org.atriasoft.archidata.dataAccess.addOnSQL.AddOnManyToMany;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
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
@EnabledIfEnvironmentVariable(named = "INCLUDE_MY_SQL_SPECIFIC", matches = "true")
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

	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	class SimpleTestInsertionAndRetrieve {

		@BeforeAll
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

		@AfterAll
		public void dropTables() throws Exception {
			if (ConfigureDb.da instanceof final DBAccessSQL daSQL) {
				daSQL.drop(TypeManyToManyLongRoot.class);
				daSQL.drop(TypeManyToManyLongRemote.class);
			}
		}

		@Order(2)
		@Test
		public void testSimpleInsertAndRetieve() throws Exception {
			final TypeManyToManyLongRoot test = new TypeManyToManyLongRoot();
			test.otherData = "root insert";
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
	}

	// TODO: add and remove link from remote class
	@Order(3)
	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	class AddLinkInsertInRoot {
		TypeManyToManyLongRemote insertedRemote1;
		TypeManyToManyLongRemote insertedRemote2;
		TypeManyToManyLongRoot insertedData;

		@BeforeAll
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

		@AfterAll
		public void dropTables() throws Exception {
			if (ConfigureDb.da instanceof final DBAccessSQL daSQL) {
				daSQL.drop(TypeManyToManyLongRoot.class);
				daSQL.drop(TypeManyToManyLongRemote.class);
			}
		}

		// ---------------------------------------------------------------
		// -- Add remote:
		// ---------------------------------------------------------------
		@Order(1)
		@Test
		public void addRemotes() throws Exception {

			TypeManyToManyLongRemote remote = new TypeManyToManyLongRemote();
			for (int iii = 0; iii < 100; iii++) {
				remote.data = "tmp" + iii;
				this.insertedRemote1 = ConfigureDb.da.insert(remote);
				ConfigureDb.da.delete(TypeManyToManyLongRemote.class, this.insertedRemote1.id);
			}
			remote = new TypeManyToManyLongRemote();
			remote.data = "remote1";
			this.insertedRemote1 = ConfigureDb.da.insert(remote);
			Assertions.assertEquals(this.insertedRemote1.data, remote.data);

			remote = new TypeManyToManyLongRemote();
			remote.data = "remote2";
			this.insertedRemote2 = ConfigureDb.da.insert(remote);
			Assertions.assertEquals(this.insertedRemote2.data, remote.data);
		}

		@Order(2)
		@Test
		public void insertDataWithoutRemote() throws Exception {

			final TypeManyToManyLongRoot test = new TypeManyToManyLongRoot();
			test.otherData = "root insert 55";
			this.insertedData = ConfigureDb.da.insert(test);
			Assertions.assertNotNull(this.insertedData);
			Assertions.assertNotNull(this.insertedData.id);
			Assertions.assertTrue(this.insertedData.id >= 0);
			Assertions.assertNull(this.insertedData.remote);

			// Try to retrieve all the data:
			final TypeManyToManyLongRoot retrieve = ConfigureDb.da.get(TypeManyToManyLongRoot.class,
					this.insertedData.id);

			Assertions.assertNotNull(retrieve);
			Assertions.assertNotNull(retrieve.id);
			Assertions.assertEquals(this.insertedData.id, retrieve.id);
			Assertions.assertNotNull(retrieve.otherData);
			Assertions.assertEquals(this.insertedData.otherData, retrieve.otherData);
			Assertions.assertNull(retrieve.remote);
		}

		@Order(3)
		@Test
		public void addLinksRemotes() throws Exception {
			// Add remote elements
			AddOnManyToMany.addLink(ConfigureDb.da, TypeManyToManyLongRoot.class, this.insertedData.id, "remote",
					this.insertedRemote1.id);
			AddOnManyToMany.addLink(ConfigureDb.da, TypeManyToManyLongRoot.class, this.insertedData.id, "remote",
					this.insertedRemote2.id);

			final TypeManyToManyLongRoot retrieve = ConfigureDb.da.get(TypeManyToManyLongRoot.class,
					this.insertedData.id);

			Assertions.assertNotNull(retrieve);
			Assertions.assertNotNull(retrieve.id);
			Assertions.assertEquals(this.insertedData.id, retrieve.id);
			Assertions.assertNotNull(retrieve.otherData);
			Assertions.assertEquals(this.insertedData.otherData, retrieve.otherData);
			Assertions.assertNotNull(retrieve.remote);
			Assertions.assertEquals(2, retrieve.remote.size());
			Assertions.assertEquals(retrieve.remote.get(0), this.insertedRemote1.id);
			Assertions.assertEquals(retrieve.remote.get(1), this.insertedRemote2.id);

			// -- Verify remote is linked:
			final TypeManyToManyLongRemote retrieveRemote = ConfigureDb.da.get(TypeManyToManyLongRemote.class,
					this.insertedRemote1.id);

			Assertions.assertNotNull(retrieveRemote);
			Assertions.assertNotNull(retrieveRemote.id);
			Assertions.assertEquals(this.insertedRemote1.id, retrieveRemote.id);
			Assertions.assertNotNull(retrieveRemote.data);
			Assertions.assertEquals(this.insertedRemote1.data, retrieveRemote.data);
			Assertions.assertNotNull(retrieveRemote.remoteToParent);
			Assertions.assertEquals(1, retrieveRemote.remoteToParent.size());
			Assertions.assertEquals(this.insertedData.id, retrieveRemote.remoteToParent.get(0));
		}

		@Order(3)
		@Test
		public void testExpand() throws Exception {
			final TypeManyToManyLongRootExpand retrieveExpand = ConfigureDb.da.get(TypeManyToManyLongRootExpand.class,
					this.insertedData.id);

			Assertions.assertNotNull(retrieveExpand);
			Assertions.assertNotNull(retrieveExpand.id);
			Assertions.assertEquals(this.insertedData.id, retrieveExpand.id);
			Assertions.assertNotNull(retrieveExpand.otherData);
			Assertions.assertEquals(this.insertedData.otherData, retrieveExpand.otherData);
			Assertions.assertNotNull(retrieveExpand.remote);
			Assertions.assertEquals(2, retrieveExpand.remote.size());
			Assertions.assertEquals(retrieveExpand.remote.get(0).id, this.insertedRemote1.id);
			Assertions.assertEquals(retrieveExpand.remote.get(0).data, this.insertedRemote1.data);
			Assertions.assertEquals(retrieveExpand.remote.get(1).id, this.insertedRemote2.id);
			Assertions.assertEquals(retrieveExpand.remote.get(1).data, this.insertedRemote2.data);
		}

		@Order(4)
		@Test
		public void removeLinksRemotes() throws Exception {
			// Remove an element
			long count = AddOnManyToMany.removeLink(ConfigureDb.da, TypeManyToManyLongRoot.class, this.insertedData.id,
					"remote", this.insertedRemote1.id);
			Assertions.assertEquals(1, count);

			TypeManyToManyLongRoot retrieve = ConfigureDb.da.get(TypeManyToManyLongRoot.class, this.insertedData.id);

			Assertions.assertNotNull(retrieve);
			Assertions.assertNotNull(retrieve.id);
			Assertions.assertEquals(this.insertedData.id, retrieve.id);
			Assertions.assertNotNull(retrieve.otherData);
			Assertions.assertEquals(this.insertedData.otherData, retrieve.otherData);
			Assertions.assertNotNull(retrieve.remote);
			Assertions.assertEquals(retrieve.remote.size(), 1);
			Assertions.assertEquals(retrieve.remote.get(0), this.insertedRemote2.id);

			// Remove the second element
			count = AddOnManyToMany.removeLink(ConfigureDb.da, TypeManyToManyLongRoot.class, retrieve.id, "remote",
					this.insertedRemote2.id);
			Assertions.assertEquals(1, count);

			retrieve = ConfigureDb.da.get(TypeManyToManyLongRoot.class, this.insertedData.id);

			Assertions.assertNotNull(retrieve);
			Assertions.assertNotNull(retrieve.id);
			Assertions.assertEquals(this.insertedData.id, retrieve.id);
			Assertions.assertNotNull(retrieve.otherData);
			Assertions.assertEquals(this.insertedData.otherData, retrieve.otherData);
			Assertions.assertNull(retrieve.remote);

			ConfigureDb.da.delete(TypeManyToManyLongRoot.class, this.insertedData.id);
		}
	}

	@Order(4)
	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	class directInsertAndRemoveInRoot {
		TypeManyToManyLongRemote insertedRemote1;
		TypeManyToManyLongRemote insertedRemote2;
		TypeManyToManyLongRoot insertedRoot1;
		TypeManyToManyLongRoot insertedRoot2;

		@BeforeAll
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

		@AfterAll
		public void dropTables() throws Exception {
			if (ConfigureDb.da instanceof final DBAccessSQL daSQL) {
				daSQL.drop(TypeManyToManyLongRoot.class);
				daSQL.drop(TypeManyToManyLongRemote.class);
			}
		}

		// ---------------------------------------------------------------
		// -- Add remote:
		// ---------------------------------------------------------------
		@Order(1)
		@Test
		public void addRemotes() throws Exception {

			TypeManyToManyLongRemote remote = new TypeManyToManyLongRemote();
			for (int iii = 0; iii < 100; iii++) {
				remote.data = "tmp" + iii;
				this.insertedRemote1 = ConfigureDb.da.insert(remote);
				ConfigureDb.da.delete(TypeManyToManyLongRemote.class, this.insertedRemote1.id);
			}
			remote = new TypeManyToManyLongRemote();
			remote.data = "remote 1";
			this.insertedRemote1 = ConfigureDb.da.insert(remote);
			Assertions.assertEquals(this.insertedRemote1.data, remote.data);

			remote = new TypeManyToManyLongRemote();
			remote.data = "remote 2";
			this.insertedRemote2 = ConfigureDb.da.insert(remote);
			Assertions.assertEquals(this.insertedRemote2.data, remote.data);

			TypeManyToManyLongRoot root = new TypeManyToManyLongRoot();
			root.otherData = "root 1";
			this.insertedRoot1 = ConfigureDb.da.insert(root);

			root = new TypeManyToManyLongRoot();
			root.otherData = "root 2";
			this.insertedRoot2 = ConfigureDb.da.insert(root);
		}

		@Order(3)
		@Test
		public void addLinksRemotes() throws Exception {
			// Add remote elements
			AddOnManyToMany.addLink(ConfigureDb.da, TypeManyToManyLongRemote.class, this.insertedRemote2.id,
					"remoteToParent", this.insertedRoot1.id);
			AddOnManyToMany.addLink(ConfigureDb.da, TypeManyToManyLongRemote.class, this.insertedRemote2.id,
					"remoteToParent", this.insertedRoot2.id);

			final TypeManyToManyLongRemote retrieve = ConfigureDb.da.get(TypeManyToManyLongRemote.class,
					this.insertedRemote2.id);

			Assertions.assertNotNull(retrieve);
			Assertions.assertNotNull(retrieve.id);
			Assertions.assertEquals(this.insertedRemote2.id, retrieve.id);
			Assertions.assertNotNull(retrieve.data);
			Assertions.assertEquals(this.insertedRemote2.data, retrieve.data);
			Assertions.assertNotNull(retrieve.remoteToParent);
			Assertions.assertEquals(2, retrieve.remoteToParent.size());
			Assertions.assertEquals(this.insertedRoot1.id, retrieve.remoteToParent.get(0));
			Assertions.assertEquals(this.insertedRoot2.id, retrieve.remoteToParent.get(1));

			final TypeManyToManyLongRoot retrieveExpand = ConfigureDb.da.get(TypeManyToManyLongRoot.class,
					this.insertedRoot1.id);

			Assertions.assertNotNull(retrieveExpand);
			Assertions.assertNotNull(retrieveExpand.id);
			Assertions.assertEquals(this.insertedRoot1.id, retrieveExpand.id);
			Assertions.assertNotNull(retrieveExpand.otherData);
			Assertions.assertEquals(this.insertedRoot1.otherData, retrieveExpand.otherData);
			Assertions.assertNotNull(retrieveExpand.remote);
			Assertions.assertEquals(1, retrieveExpand.remote.size());
			Assertions.assertEquals(this.insertedRemote2.id, retrieveExpand.remote.get(0));

			// -- Verify remote is linked:
			final TypeManyToManyLongRoot retrieveRemote = ConfigureDb.da.get(TypeManyToManyLongRoot.class,
					this.insertedRoot2.id);

			Assertions.assertNotNull(retrieveRemote);
			Assertions.assertNotNull(retrieveRemote.id);
			Assertions.assertEquals(this.insertedRoot2.id, retrieveRemote.id);
			Assertions.assertNotNull(retrieveRemote.otherData);
			Assertions.assertEquals(this.insertedRoot2.otherData, retrieveRemote.otherData);
			Assertions.assertNotNull(retrieveRemote.remote);
			Assertions.assertEquals(1, retrieveRemote.remote.size());
			Assertions.assertEquals(this.insertedRemote2.id, retrieveRemote.remote.get(0));
		}

		@Order(4)
		@Test
		public void removeLinksRemotes() throws Exception {
			// Remove root elements
			AddOnManyToMany.removeLink(ConfigureDb.da, TypeManyToManyLongRemote.class, this.insertedRemote2.id,
					"remoteToParent", this.insertedRoot2.id);

			final TypeManyToManyLongRemote retrieve = ConfigureDb.da.get(TypeManyToManyLongRemote.class,
					this.insertedRemote2.id);

			Assertions.assertNotNull(retrieve);
			Assertions.assertNotNull(retrieve.id);
			Assertions.assertEquals(this.insertedRemote2.id, retrieve.id);
			Assertions.assertNotNull(retrieve.data);
			Assertions.assertEquals(this.insertedRemote2.data, retrieve.data);
			Assertions.assertNotNull(retrieve.remoteToParent);
			Assertions.assertEquals(1, retrieve.remoteToParent.size());
			Assertions.assertEquals(this.insertedRoot1.id, retrieve.remoteToParent.get(0));

			// -- Verify remote is linked:
			final TypeManyToManyLongRoot retrieveExpand = ConfigureDb.da.get(TypeManyToManyLongRoot.class,
					this.insertedRoot1.id);

			Assertions.assertNotNull(retrieveExpand);
			Assertions.assertNotNull(retrieveExpand.id);
			Assertions.assertEquals(this.insertedRoot1.id, retrieveExpand.id);
			Assertions.assertNotNull(retrieveExpand.otherData);
			Assertions.assertEquals(this.insertedRoot1.otherData, retrieveExpand.otherData);
			Assertions.assertNotNull(retrieveExpand.remote);
			Assertions.assertEquals(1, retrieveExpand.remote.size());
			Assertions.assertEquals(this.insertedRemote2.id, retrieveExpand.remote.get(0));

			// -- Verify remote is un-linked:
			final TypeManyToManyLongRoot retrieveRemote = ConfigureDb.da.get(TypeManyToManyLongRoot.class,
					this.insertedRoot2.id);

			Assertions.assertNotNull(retrieveRemote);
			Assertions.assertNotNull(retrieveRemote.id);
			Assertions.assertEquals(this.insertedRoot2.id, retrieveRemote.id);
			Assertions.assertNotNull(retrieveRemote.otherData);
			Assertions.assertEquals(this.insertedRoot2.otherData, retrieveRemote.otherData);
			Assertions.assertNull(retrieveRemote.remote);

		}

		@Order(5)
		@Test
		public void removeSecondLinksRemotes() throws Exception {
			// Remove root elements
			AddOnManyToMany.removeLink(ConfigureDb.da, TypeManyToManyLongRemote.class, this.insertedRemote2.id,
					"remoteToParent", this.insertedRoot1.id);

			final TypeManyToManyLongRemote retrieve = ConfigureDb.da.get(TypeManyToManyLongRemote.class,
					this.insertedRemote2.id);

			Assertions.assertNotNull(retrieve);
			Assertions.assertNotNull(retrieve.id);
			Assertions.assertEquals(this.insertedRemote2.id, retrieve.id);
			Assertions.assertNotNull(retrieve.data);
			Assertions.assertEquals(this.insertedRemote2.data, retrieve.data);
			Assertions.assertNull(retrieve.remoteToParent);

			// -- Verify remote is linked:
			final TypeManyToManyLongRootExpand retrieveExpand = ConfigureDb.da.get(TypeManyToManyLongRootExpand.class,
					this.insertedRoot1.id);

			Assertions.assertNotNull(retrieveExpand);
			Assertions.assertNotNull(retrieveExpand.id);
			Assertions.assertEquals(this.insertedRoot1.id, retrieveExpand.id);
			Assertions.assertNotNull(retrieveExpand.otherData);
			Assertions.assertEquals(this.insertedRoot1.otherData, retrieveExpand.otherData);
			Assertions.assertNull(retrieveExpand.remote);

			// -- Verify remote is un-linked:
			final TypeManyToManyLongRootExpand retrieveRemote = ConfigureDb.da.get(TypeManyToManyLongRootExpand.class,
					this.insertedRoot2.id);

			Assertions.assertNotNull(retrieveRemote);
			Assertions.assertNotNull(retrieveRemote.id);
			Assertions.assertEquals(this.insertedRoot2.id, retrieveRemote.id);
			Assertions.assertNotNull(retrieveRemote.otherData);
			Assertions.assertEquals(this.insertedRoot2.otherData, retrieveRemote.otherData);
			Assertions.assertNull(retrieveRemote.remote);

		}

		TypeManyToManyLongRemote insertedParameters;

		// ---------------------------------------------------------------
		// -- Add parent with manyToMany in parameters:
		// ---------------------------------------------------------------
		@Order(6)
		@Test
		public void AddParentWithManyToManyInParameters() throws Exception {
			final TypeManyToManyLongRemote test = new TypeManyToManyLongRemote();
			test.data = "insert with remote";
			test.remoteToParent = new ArrayList<>();
			test.remoteToParent.add(this.insertedRoot1.id);
			test.remoteToParent.add(this.insertedRoot2.id);
			this.insertedParameters = ConfigureDb.da.insert(test);
			Assertions.assertNotNull(this.insertedParameters);
			Assertions.assertNotNull(this.insertedParameters.id);
			Assertions.assertTrue(this.insertedParameters.id >= 0);
			Assertions.assertNotNull(this.insertedParameters.remoteToParent);
			Assertions.assertEquals(2, this.insertedParameters.remoteToParent.size());
			Assertions.assertEquals(this.insertedRoot1.id, this.insertedParameters.remoteToParent.get(0));
			Assertions.assertEquals(this.insertedRoot2.id, this.insertedParameters.remoteToParent.get(1));

			// -- Verify remote is linked:
			TypeManyToManyLongRoot retrieveRoot = ConfigureDb.da.get(TypeManyToManyLongRoot.class,
					this.insertedRoot1.id);

			Assertions.assertNotNull(retrieveRoot);
			Assertions.assertNotNull(retrieveRoot.id);
			Assertions.assertEquals(this.insertedRoot1.id, retrieveRoot.id);
			Assertions.assertNotNull(retrieveRoot.otherData);
			Assertions.assertEquals(this.insertedRoot1.otherData, retrieveRoot.otherData);
			Assertions.assertNotNull(retrieveRoot.remote);
			Assertions.assertEquals(1, retrieveRoot.remote.size());
			Assertions.assertEquals(this.insertedParameters.id, retrieveRoot.remote.get(0));

			retrieveRoot = ConfigureDb.da.get(TypeManyToManyLongRoot.class, this.insertedRoot2.id);

			Assertions.assertNotNull(retrieveRoot);
			Assertions.assertNotNull(retrieveRoot.id);
			Assertions.assertEquals(this.insertedRoot2.id, retrieveRoot.id);
			Assertions.assertNotNull(retrieveRoot.otherData);
			Assertions.assertEquals(this.insertedRoot2.otherData, retrieveRoot.otherData);
			Assertions.assertNotNull(retrieveRoot.remote);
			Assertions.assertEquals(1, retrieveRoot.remote.size());
			Assertions.assertEquals(this.insertedParameters.id, retrieveRoot.remote.get(0));

		}

		// ---------------------------------------------------------------
		// -- Update Parent Data:
		// ---------------------------------------------------------------
		@Order(7)
		@Test
		public void updateRequest() throws Exception {
			final TypeManyToManyLongRemote testUpdate = new TypeManyToManyLongRemote();
			testUpdate.remoteToParent = new ArrayList<>();
			testUpdate.remoteToParent.add(this.insertedRoot2.id);
			final long numberUpdate = ConfigureDb.da.update(testUpdate, this.insertedParameters.id);
			Assertions.assertEquals(1, numberUpdate);

			final TypeManyToManyLongRemote insertedDataUpdate = ConfigureDb.da.get(TypeManyToManyLongRemote.class,
					this.insertedParameters.id);
			Assertions.assertNotNull(insertedDataUpdate);
			Assertions.assertNotNull(insertedDataUpdate.id);
			Assertions.assertTrue(insertedDataUpdate.id >= 0);
			Assertions.assertNotNull(insertedDataUpdate.remoteToParent);
			Assertions.assertEquals(1, insertedDataUpdate.remoteToParent.size());
			Assertions.assertEquals(this.insertedRoot2.id, insertedDataUpdate.remoteToParent.get(0));

			// -- Verify remote is linked (removed):
			TypeManyToManyLongRoot retrieveRoot = ConfigureDb.da.get(TypeManyToManyLongRoot.class,
					this.insertedRoot1.id);

			Assertions.assertNotNull(retrieveRoot);
			Assertions.assertNotNull(retrieveRoot.id);
			Assertions.assertEquals(this.insertedRoot1.id, retrieveRoot.id);
			Assertions.assertNotNull(retrieveRoot.otherData);
			Assertions.assertEquals(this.insertedRoot1.otherData, retrieveRoot.otherData);
			Assertions.assertNull(retrieveRoot.remote);

			// -- Verify remote is linked (keep):
			retrieveRoot = ConfigureDb.da.get(TypeManyToManyLongRoot.class, this.insertedRoot2.id);

			Assertions.assertNotNull(retrieveRoot);
			Assertions.assertNotNull(retrieveRoot.id);
			Assertions.assertEquals(this.insertedRoot2.id, retrieveRoot.id);
			Assertions.assertNotNull(retrieveRoot.otherData);
			Assertions.assertEquals(this.insertedRoot2.otherData, retrieveRoot.otherData);
			Assertions.assertNotNull(retrieveRoot.remote);
			Assertions.assertEquals(1, retrieveRoot.remote.size());
			Assertions.assertEquals(this.insertedParameters.id, retrieveRoot.remote.get(0));

		}
	}
}
