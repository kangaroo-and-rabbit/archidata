
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
import test.atriasoft.archidata.dataAccess.model.TypeManyToManyUUIDRemote;
import test.atriasoft.archidata.dataAccess.model.TypeManyToManyUUIDRoot;
import test.atriasoft.archidata.dataAccess.model.TypeManyToManyUUIDRootExpand;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@EnabledIfEnvironmentVariable(named = "INCLUDE_MY_SQL_SPECIFIC", matches = "true")
public class TestManyToManyUUID {
	final static private Logger LOGGER = LoggerFactory.getLogger(TestManyToManyUUID.class);

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
			final List<String> sqlCommand2 = DataFactory.createTable(TypeManyToManyUUIDRoot.class);
			final List<String> sqlCommand = DataFactory.createTable(TypeManyToManyUUIDRemote.class);
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
				daSQL.drop(TypeManyToManyUUIDRoot.class);
				daSQL.drop(TypeManyToManyUUIDRemote.class);
			}
		}

		@Order(2)
		@Test
		public void testSimpleInsertAndRetieve() throws Exception {
			final TypeManyToManyUUIDRoot test = new TypeManyToManyUUIDRoot();
			test.otherData = "root insert";
			final TypeManyToManyUUIDRoot insertedData = ConfigureDb.da.insert(test);
			Assertions.assertNotNull(insertedData);
			Assertions.assertNotNull(insertedData.uuid);
			Assertions.assertNull(insertedData.remote);

			// Try to retrieve all the data:
			final TypeManyToManyUUIDRoot retrieve = ConfigureDb.da.get(TypeManyToManyUUIDRoot.class, insertedData.uuid);

			Assertions.assertNotNull(retrieve);
			Assertions.assertNotNull(retrieve.uuid);
			Assertions.assertEquals(insertedData.uuid, retrieve.uuid);
			Assertions.assertNotNull(retrieve.otherData);
			Assertions.assertEquals(insertedData.otherData, retrieve.otherData);
			Assertions.assertNull(retrieve.remote);

			ConfigureDb.da.delete(TypeManyToManyUUIDRoot.class, insertedData.uuid);
		}
	}

	// TODO: add and remove link from remote class
	@Order(3)
	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	class AddLinkInsertInRoot {
		TypeManyToManyUUIDRemote insertedRemote1;
		TypeManyToManyUUIDRemote insertedRemote2;
		TypeManyToManyUUIDRoot insertedData;

		@BeforeAll
		public void testCreateTable() throws Exception {
			final List<String> sqlCommand2 = DataFactory.createTable(TypeManyToManyUUIDRoot.class);
			final List<String> sqlCommand = DataFactory.createTable(TypeManyToManyUUIDRemote.class);
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
				daSQL.drop(TypeManyToManyUUIDRoot.class);
				daSQL.drop(TypeManyToManyUUIDRemote.class);
			}
		}

		// ---------------------------------------------------------------
		// -- Add remote:
		// ---------------------------------------------------------------
		@Order(1)
		@Test
		public void addRemotes() throws Exception {

			TypeManyToManyUUIDRemote remote = new TypeManyToManyUUIDRemote();
			for (int iii = 0; iii < 100; iii++) {
				remote.data = "tmp" + iii;
				this.insertedRemote1 = ConfigureDb.da.insert(remote);
				ConfigureDb.da.delete(TypeManyToManyUUIDRemote.class, this.insertedRemote1.uuid);
			}
			remote = new TypeManyToManyUUIDRemote();
			remote.data = "remote1";
			this.insertedRemote1 = ConfigureDb.da.insert(remote);
			Assertions.assertEquals(this.insertedRemote1.data, remote.data);

			remote = new TypeManyToManyUUIDRemote();
			remote.data = "remote2";
			this.insertedRemote2 = ConfigureDb.da.insert(remote);
			Assertions.assertEquals(this.insertedRemote2.data, remote.data);
		}

		@Order(2)
		@Test
		public void insertDataWithoutRemote() throws Exception {

			final TypeManyToManyUUIDRoot test = new TypeManyToManyUUIDRoot();
			test.otherData = "root insert 55";
			this.insertedData = ConfigureDb.da.insert(test);
			Assertions.assertNotNull(this.insertedData);
			Assertions.assertNotNull(this.insertedData.uuid);
			Assertions.assertNull(this.insertedData.remote);

			// Try to retrieve all the data:
			final TypeManyToManyUUIDRoot retrieve = ConfigureDb.da.get(TypeManyToManyUUIDRoot.class,
					this.insertedData.uuid);

			Assertions.assertNotNull(retrieve);
			Assertions.assertNotNull(retrieve.uuid);
			Assertions.assertEquals(this.insertedData.uuid, retrieve.uuid);
			Assertions.assertNotNull(retrieve.otherData);
			Assertions.assertEquals(this.insertedData.otherData, retrieve.otherData);
			Assertions.assertNull(retrieve.remote);
		}

		@Order(3)
		@Test
		public void addLinksRemotes() throws Exception {
			// Add remote elements
			AddOnManyToMany.addLink(ConfigureDb.da, TypeManyToManyUUIDRoot.class, this.insertedData.uuid, "remote",
					this.insertedRemote1.uuid);
			AddOnManyToMany.addLink(ConfigureDb.da, TypeManyToManyUUIDRoot.class, this.insertedData.uuid, "remote",
					this.insertedRemote2.uuid);

			final TypeManyToManyUUIDRoot retrieve = ConfigureDb.da.get(TypeManyToManyUUIDRoot.class,
					this.insertedData.uuid);

			Assertions.assertNotNull(retrieve);
			Assertions.assertNotNull(retrieve.uuid);
			Assertions.assertEquals(this.insertedData.uuid, retrieve.uuid);
			Assertions.assertNotNull(retrieve.otherData);
			Assertions.assertEquals(this.insertedData.otherData, retrieve.otherData);
			Assertions.assertNotNull(retrieve.remote);
			Assertions.assertEquals(2, retrieve.remote.size());
			Assertions.assertEquals(retrieve.remote.get(0), this.insertedRemote1.uuid);
			Assertions.assertEquals(retrieve.remote.get(1), this.insertedRemote2.uuid);

			// -- Verify remote is linked:
			final TypeManyToManyUUIDRemote retrieveRemote = ConfigureDb.da.get(TypeManyToManyUUIDRemote.class,
					this.insertedRemote1.uuid);

			Assertions.assertNotNull(retrieveRemote);
			Assertions.assertNotNull(retrieveRemote.uuid);
			Assertions.assertEquals(this.insertedRemote1.uuid, retrieveRemote.uuid);
			Assertions.assertNotNull(retrieveRemote.data);
			Assertions.assertEquals(this.insertedRemote1.data, retrieveRemote.data);
			Assertions.assertNotNull(retrieveRemote.remoteToParent);
			Assertions.assertEquals(1, retrieveRemote.remoteToParent.size());
			Assertions.assertEquals(this.insertedData.uuid, retrieveRemote.remoteToParent.get(0));
		}

		@Order(3)
		@Test
		public void testExpand() throws Exception {
			final TypeManyToManyUUIDRootExpand retrieveExpand = ConfigureDb.da.get(TypeManyToManyUUIDRootExpand.class,
					this.insertedData.uuid);

			Assertions.assertNotNull(retrieveExpand);
			Assertions.assertNotNull(retrieveExpand.uuid);
			Assertions.assertEquals(this.insertedData.uuid, retrieveExpand.uuid);
			Assertions.assertNotNull(retrieveExpand.otherData);
			Assertions.assertEquals(this.insertedData.otherData, retrieveExpand.otherData);
			Assertions.assertNotNull(retrieveExpand.remote);
			Assertions.assertEquals(2, retrieveExpand.remote.size());
			Assertions.assertEquals(retrieveExpand.remote.get(0).uuid, this.insertedRemote1.uuid);
			Assertions.assertEquals(retrieveExpand.remote.get(0).data, this.insertedRemote1.data);
			Assertions.assertEquals(retrieveExpand.remote.get(1).uuid, this.insertedRemote2.uuid);
			Assertions.assertEquals(retrieveExpand.remote.get(1).data, this.insertedRemote2.data);
		}

		@Order(4)
		@Test
		public void removeLinksRemotes() throws Exception {
			// Remove an element
			long count = AddOnManyToMany.removeLink(ConfigureDb.da, TypeManyToManyUUIDRoot.class,
					this.insertedData.uuid, "remote", this.insertedRemote1.uuid);
			Assertions.assertEquals(1, count);

			TypeManyToManyUUIDRoot retrieve = ConfigureDb.da.get(TypeManyToManyUUIDRoot.class, this.insertedData.uuid);

			Assertions.assertNotNull(retrieve);
			Assertions.assertNotNull(retrieve.uuid);
			Assertions.assertEquals(this.insertedData.uuid, retrieve.uuid);
			Assertions.assertNotNull(retrieve.otherData);
			Assertions.assertEquals(this.insertedData.otherData, retrieve.otherData);
			Assertions.assertNotNull(retrieve.remote);
			Assertions.assertEquals(retrieve.remote.size(), 1);
			Assertions.assertEquals(retrieve.remote.get(0), this.insertedRemote2.uuid);

			// Remove the second element
			count = AddOnManyToMany.removeLink(ConfigureDb.da, TypeManyToManyUUIDRoot.class, retrieve.uuid, "remote",
					this.insertedRemote2.uuid);
			Assertions.assertEquals(1, count);

			retrieve = ConfigureDb.da.get(TypeManyToManyUUIDRoot.class, this.insertedData.uuid);

			Assertions.assertNotNull(retrieve);
			Assertions.assertNotNull(retrieve.uuid);
			Assertions.assertEquals(this.insertedData.uuid, retrieve.uuid);
			Assertions.assertNotNull(retrieve.otherData);
			Assertions.assertEquals(this.insertedData.otherData, retrieve.otherData);
			Assertions.assertNull(retrieve.remote);

			ConfigureDb.da.delete(TypeManyToManyUUIDRoot.class, this.insertedData.uuid);
		}
	}

	@Order(4)
	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	class directInsertAndRemoveInRoot {
		TypeManyToManyUUIDRemote insertedRemote1;
		TypeManyToManyUUIDRemote insertedRemote2;
		TypeManyToManyUUIDRoot insertedRoot1;
		TypeManyToManyUUIDRoot insertedRoot2;

		@BeforeAll
		public void testCreateTable() throws Exception {
			final List<String> sqlCommand2 = DataFactory.createTable(TypeManyToManyUUIDRoot.class);
			final List<String> sqlCommand = DataFactory.createTable(TypeManyToManyUUIDRemote.class);
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
				daSQL.drop(TypeManyToManyUUIDRoot.class);
				daSQL.drop(TypeManyToManyUUIDRemote.class);
			}
		}

		// ---------------------------------------------------------------
		// -- Add remote:
		// ---------------------------------------------------------------
		@Order(1)
		@Test
		public void addRemotes() throws Exception {

			TypeManyToManyUUIDRemote remote = new TypeManyToManyUUIDRemote();
			for (int iii = 0; iii < 100; iii++) {
				remote.data = "tmp" + iii;
				this.insertedRemote1 = ConfigureDb.da.insert(remote);
				ConfigureDb.da.delete(TypeManyToManyUUIDRemote.class, this.insertedRemote1.uuid);
			}
			remote = new TypeManyToManyUUIDRemote();
			remote.data = "remote 1";
			this.insertedRemote1 = ConfigureDb.da.insert(remote);
			Assertions.assertEquals(this.insertedRemote1.data, remote.data);

			remote = new TypeManyToManyUUIDRemote();
			remote.data = "remote 2";
			this.insertedRemote2 = ConfigureDb.da.insert(remote);
			Assertions.assertEquals(this.insertedRemote2.data, remote.data);

			TypeManyToManyUUIDRoot root = new TypeManyToManyUUIDRoot();
			root.otherData = "root 1";
			this.insertedRoot1 = ConfigureDb.da.insert(root);

			root = new TypeManyToManyUUIDRoot();
			root.otherData = "root 2";
			this.insertedRoot2 = ConfigureDb.da.insert(root);
		}

		@Order(3)
		@Test
		public void addLinksRemotes() throws Exception {
			// Add remote elements
			AddOnManyToMany.addLink(ConfigureDb.da, TypeManyToManyUUIDRemote.class, this.insertedRemote2.uuid,
					"remoteToParent", this.insertedRoot1.uuid);
			AddOnManyToMany.addLink(ConfigureDb.da, TypeManyToManyUUIDRemote.class, this.insertedRemote2.uuid,
					"remoteToParent", this.insertedRoot2.uuid);

			final TypeManyToManyUUIDRemote retrieve = ConfigureDb.da.get(TypeManyToManyUUIDRemote.class,
					this.insertedRemote2.uuid);

			Assertions.assertNotNull(retrieve);
			Assertions.assertNotNull(retrieve.uuid);
			Assertions.assertEquals(this.insertedRemote2.uuid, retrieve.uuid);
			Assertions.assertNotNull(retrieve.data);
			Assertions.assertEquals(this.insertedRemote2.data, retrieve.data);
			Assertions.assertNotNull(retrieve.remoteToParent);
			Assertions.assertEquals(2, retrieve.remoteToParent.size());
			Assertions.assertEquals(this.insertedRoot1.uuid, retrieve.remoteToParent.get(0));
			Assertions.assertEquals(this.insertedRoot2.uuid, retrieve.remoteToParent.get(1));

			final TypeManyToManyUUIDRoot retrieveExpand = ConfigureDb.da.get(TypeManyToManyUUIDRoot.class,
					this.insertedRoot1.uuid);

			Assertions.assertNotNull(retrieveExpand);
			Assertions.assertNotNull(retrieveExpand.uuid);
			Assertions.assertEquals(this.insertedRoot1.uuid, retrieveExpand.uuid);
			Assertions.assertNotNull(retrieveExpand.otherData);
			Assertions.assertEquals(this.insertedRoot1.otherData, retrieveExpand.otherData);
			Assertions.assertNotNull(retrieveExpand.remote);
			Assertions.assertEquals(1, retrieveExpand.remote.size());
			Assertions.assertEquals(this.insertedRemote2.uuid, retrieveExpand.remote.get(0));

			// -- Verify remote is linked:
			final TypeManyToManyUUIDRoot retrieveRemote = ConfigureDb.da.get(TypeManyToManyUUIDRoot.class,
					this.insertedRoot2.uuid);

			Assertions.assertNotNull(retrieveRemote);
			Assertions.assertNotNull(retrieveRemote.uuid);
			Assertions.assertEquals(this.insertedRoot2.uuid, retrieveRemote.uuid);
			Assertions.assertNotNull(retrieveRemote.otherData);
			Assertions.assertEquals(this.insertedRoot2.otherData, retrieveRemote.otherData);
			Assertions.assertNotNull(retrieveRemote.remote);
			Assertions.assertEquals(1, retrieveRemote.remote.size());
			Assertions.assertEquals(this.insertedRemote2.uuid, retrieveRemote.remote.get(0));
		}

		@Order(4)
		@Test
		public void removeLinksRemotes() throws Exception {
			// Remove root elements
			AddOnManyToMany.removeLink(ConfigureDb.da, TypeManyToManyUUIDRemote.class, this.insertedRemote2.uuid,
					"remoteToParent", this.insertedRoot2.uuid);

			final TypeManyToManyUUIDRemote retrieve = ConfigureDb.da.get(TypeManyToManyUUIDRemote.class,
					this.insertedRemote2.uuid);

			Assertions.assertNotNull(retrieve);
			Assertions.assertNotNull(retrieve.uuid);
			Assertions.assertEquals(this.insertedRemote2.uuid, retrieve.uuid);
			Assertions.assertNotNull(retrieve.data);
			Assertions.assertEquals(this.insertedRemote2.data, retrieve.data);
			Assertions.assertNotNull(retrieve.remoteToParent);
			Assertions.assertEquals(1, retrieve.remoteToParent.size());
			Assertions.assertEquals(this.insertedRoot1.uuid, retrieve.remoteToParent.get(0));

			// -- Verify remote is linked:
			final TypeManyToManyUUIDRoot retrieveExpand = ConfigureDb.da.get(TypeManyToManyUUIDRoot.class,
					this.insertedRoot1.uuid);

			Assertions.assertNotNull(retrieveExpand);
			Assertions.assertNotNull(retrieveExpand.uuid);
			Assertions.assertEquals(this.insertedRoot1.uuid, retrieveExpand.uuid);
			Assertions.assertNotNull(retrieveExpand.otherData);
			Assertions.assertEquals(this.insertedRoot1.otherData, retrieveExpand.otherData);
			Assertions.assertNotNull(retrieveExpand.remote);
			Assertions.assertEquals(1, retrieveExpand.remote.size());
			Assertions.assertEquals(this.insertedRemote2.uuid, retrieveExpand.remote.get(0));

			// -- Verify remote is un-linked:
			final TypeManyToManyUUIDRoot retrieveRemote = ConfigureDb.da.get(TypeManyToManyUUIDRoot.class,
					this.insertedRoot2.uuid);

			Assertions.assertNotNull(retrieveRemote);
			Assertions.assertNotNull(retrieveRemote.uuid);
			Assertions.assertEquals(this.insertedRoot2.uuid, retrieveRemote.uuid);
			Assertions.assertNotNull(retrieveRemote.otherData);
			Assertions.assertEquals(this.insertedRoot2.otherData, retrieveRemote.otherData);
			Assertions.assertNull(retrieveRemote.remote);

		}

		@Order(5)
		@Test
		public void removeSecondLinksRemotes() throws Exception {
			// Remove root elements
			AddOnManyToMany.removeLink(ConfigureDb.da, TypeManyToManyUUIDRemote.class, this.insertedRemote2.uuid,
					"remoteToParent", this.insertedRoot1.uuid);

			final TypeManyToManyUUIDRemote retrieve = ConfigureDb.da.get(TypeManyToManyUUIDRemote.class,
					this.insertedRemote2.uuid);

			Assertions.assertNotNull(retrieve);
			Assertions.assertNotNull(retrieve.uuid);
			Assertions.assertEquals(this.insertedRemote2.uuid, retrieve.uuid);
			Assertions.assertNotNull(retrieve.data);
			Assertions.assertEquals(this.insertedRemote2.data, retrieve.data);
			Assertions.assertNull(retrieve.remoteToParent);

			// -- Verify remote is linked:
			final TypeManyToManyUUIDRootExpand retrieveExpand = ConfigureDb.da.get(TypeManyToManyUUIDRootExpand.class,
					this.insertedRoot1.uuid);

			Assertions.assertNotNull(retrieveExpand);
			Assertions.assertNotNull(retrieveExpand.uuid);
			Assertions.assertEquals(this.insertedRoot1.uuid, retrieveExpand.uuid);
			Assertions.assertNotNull(retrieveExpand.otherData);
			Assertions.assertEquals(this.insertedRoot1.otherData, retrieveExpand.otherData);
			Assertions.assertNull(retrieveExpand.remote);

			// -- Verify remote is un-linked:
			final TypeManyToManyUUIDRootExpand retrieveRemote = ConfigureDb.da.get(TypeManyToManyUUIDRootExpand.class,
					this.insertedRoot2.uuid);

			Assertions.assertNotNull(retrieveRemote);
			Assertions.assertNotNull(retrieveRemote.uuid);
			Assertions.assertEquals(this.insertedRoot2.uuid, retrieveRemote.uuid);
			Assertions.assertNotNull(retrieveRemote.otherData);
			Assertions.assertEquals(this.insertedRoot2.otherData, retrieveRemote.otherData);
			Assertions.assertNull(retrieveRemote.remote);

		}

		TypeManyToManyUUIDRemote insertedParameters;

		// ---------------------------------------------------------------
		// -- Add parent with manyToMany in parameters:
		// ---------------------------------------------------------------
		@Order(6)
		@Test
		public void AddParentWithManyToManyInParameters() throws Exception {
			final TypeManyToManyUUIDRemote test = new TypeManyToManyUUIDRemote();
			test.data = "insert with remote";
			test.remoteToParent = new ArrayList<>();
			test.remoteToParent.add(this.insertedRoot1.uuid);
			test.remoteToParent.add(this.insertedRoot2.uuid);
			this.insertedParameters = ConfigureDb.da.insert(test);
			Assertions.assertNotNull(this.insertedParameters);
			Assertions.assertNotNull(this.insertedParameters.uuid);
			Assertions.assertNotNull(this.insertedParameters.remoteToParent);
			Assertions.assertEquals(2, this.insertedParameters.remoteToParent.size());
			Assertions.assertEquals(this.insertedRoot1.uuid, this.insertedParameters.remoteToParent.get(0));
			Assertions.assertEquals(this.insertedRoot2.uuid, this.insertedParameters.remoteToParent.get(1));

			// -- Verify remote is linked:
			TypeManyToManyUUIDRoot retrieveRoot = ConfigureDb.da.get(TypeManyToManyUUIDRoot.class,
					this.insertedRoot1.uuid);

			Assertions.assertNotNull(retrieveRoot);
			Assertions.assertNotNull(retrieveRoot.uuid);
			Assertions.assertEquals(this.insertedRoot1.uuid, retrieveRoot.uuid);
			Assertions.assertNotNull(retrieveRoot.otherData);
			Assertions.assertEquals(this.insertedRoot1.otherData, retrieveRoot.otherData);
			Assertions.assertNotNull(retrieveRoot.remote);
			Assertions.assertEquals(1, retrieveRoot.remote.size());
			Assertions.assertEquals(this.insertedParameters.uuid, retrieveRoot.remote.get(0));

			retrieveRoot = ConfigureDb.da.get(TypeManyToManyUUIDRoot.class, this.insertedRoot2.uuid);

			Assertions.assertNotNull(retrieveRoot);
			Assertions.assertNotNull(retrieveRoot.uuid);
			Assertions.assertEquals(this.insertedRoot2.uuid, retrieveRoot.uuid);
			Assertions.assertNotNull(retrieveRoot.otherData);
			Assertions.assertEquals(this.insertedRoot2.otherData, retrieveRoot.otherData);
			Assertions.assertNotNull(retrieveRoot.remote);
			Assertions.assertEquals(1, retrieveRoot.remote.size());
			Assertions.assertEquals(this.insertedParameters.uuid, retrieveRoot.remote.get(0));

		}

		// ---------------------------------------------------------------
		// -- Update Parent Data:
		// ---------------------------------------------------------------
		@Order(7)
		@Test
		public void updateRequest() throws Exception {
			final TypeManyToManyUUIDRemote testUpdate = new TypeManyToManyUUIDRemote();
			testUpdate.remoteToParent = new ArrayList<>();
			testUpdate.remoteToParent.add(this.insertedRoot2.uuid);
			final long numberUpdate = ConfigureDb.da.update(testUpdate, this.insertedParameters.uuid);
			Assertions.assertEquals(1, numberUpdate);

			final TypeManyToManyUUIDRemote insertedDataUpdate = ConfigureDb.da.get(TypeManyToManyUUIDRemote.class,
					this.insertedParameters.uuid);
			Assertions.assertNotNull(insertedDataUpdate);
			Assertions.assertNotNull(insertedDataUpdate.uuid);
			Assertions.assertNotNull(insertedDataUpdate.remoteToParent);
			Assertions.assertEquals(1, insertedDataUpdate.remoteToParent.size());
			Assertions.assertEquals(this.insertedRoot2.uuid, insertedDataUpdate.remoteToParent.get(0));

			// -- Verify remote is linked (removed):
			TypeManyToManyUUIDRoot retrieveRoot = ConfigureDb.da.get(TypeManyToManyUUIDRoot.class,
					this.insertedRoot1.uuid);

			Assertions.assertNotNull(retrieveRoot);
			Assertions.assertNotNull(retrieveRoot.uuid);
			Assertions.assertEquals(this.insertedRoot1.uuid, retrieveRoot.uuid);
			Assertions.assertNotNull(retrieveRoot.otherData);
			Assertions.assertEquals(this.insertedRoot1.otherData, retrieveRoot.otherData);
			Assertions.assertNull(retrieveRoot.remote);

			// -- Verify remote is linked (keep):
			retrieveRoot = ConfigureDb.da.get(TypeManyToManyUUIDRoot.class, this.insertedRoot2.uuid);

			Assertions.assertNotNull(retrieveRoot);
			Assertions.assertNotNull(retrieveRoot.uuid);
			Assertions.assertEquals(this.insertedRoot2.uuid, retrieveRoot.uuid);
			Assertions.assertNotNull(retrieveRoot.otherData);
			Assertions.assertEquals(this.insertedRoot2.otherData, retrieveRoot.otherData);
			Assertions.assertNotNull(retrieveRoot.remote);
			Assertions.assertEquals(1, retrieveRoot.remote.size());
			Assertions.assertEquals(this.insertedParameters.uuid, retrieveRoot.remote.get(0));

		}
	}
}
