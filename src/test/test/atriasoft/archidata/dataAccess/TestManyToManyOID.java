package test.atriasoft.archidata.dataAccess;

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
import test.atriasoft.archidata.dataAccess.model.TypeManyToManyOIDRemote;
import test.atriasoft.archidata.dataAccess.model.TypeManyToManyOIDRoot;
import test.atriasoft.archidata.dataAccess.model.TypeManyToManyOIDRootExpand;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@EnabledIfEnvironmentVariable(named = "INCLUDE_MY_SQL_SPECIFIC", matches = "true")
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

	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	class SimpleTestInsertionAndRetrieve {

		@BeforeAll
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

		@AfterAll
		public void dropTables() throws Exception {
			if (ConfigureDb.da instanceof final DBAccessSQL daSQL) {
				daSQL.drop(TypeManyToManyOIDRoot.class);
				daSQL.drop(TypeManyToManyOIDRemote.class);
			}
		}

		@Order(2)
		@Test
		public void testSimpleInsertAndRetieve() throws Exception {
			final TypeManyToManyOIDRoot test = new TypeManyToManyOIDRoot();
			test.otherData = "root insert";
			final TypeManyToManyOIDRoot insertedData = ConfigureDb.da.insert(test);
			Assertions.assertNotNull(insertedData);
			Assertions.assertNotNull(insertedData.oid);
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
	}

	// TODO: add and remove link from remote class
	@Order(3)
	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	class AddLinkInsertInRoot {
		TypeManyToManyOIDRemote insertedRemote1;
		TypeManyToManyOIDRemote insertedRemote2;
		TypeManyToManyOIDRoot insertedData;

		@BeforeAll
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

		@AfterAll
		public void dropTables() throws Exception {
			if (ConfigureDb.da instanceof final DBAccessSQL daSQL) {
				daSQL.drop(TypeManyToManyOIDRoot.class);
				daSQL.drop(TypeManyToManyOIDRemote.class);
			}
		}

		// ---------------------------------------------------------------
		// -- Add remote:
		// ---------------------------------------------------------------
		@Order(1)
		@Test
		public void addRemotes() throws Exception {

			TypeManyToManyOIDRemote remote = new TypeManyToManyOIDRemote();
			for (int iii = 0; iii < 100; iii++) {
				remote.data = "tmp" + iii;
				this.insertedRemote1 = ConfigureDb.da.insert(remote);
				ConfigureDb.da.delete(TypeManyToManyOIDRemote.class, this.insertedRemote1.oid);
			}
			remote = new TypeManyToManyOIDRemote();
			remote.data = "remote1";
			this.insertedRemote1 = ConfigureDb.da.insert(remote);
			Assertions.assertEquals(this.insertedRemote1.data, remote.data);

			remote = new TypeManyToManyOIDRemote();
			remote.data = "remote2";
			this.insertedRemote2 = ConfigureDb.da.insert(remote);
			Assertions.assertEquals(this.insertedRemote2.data, remote.data);
		}

		@Order(2)
		@Test
		public void insertDataWithoutRemote() throws Exception {

			final TypeManyToManyOIDRoot test = new TypeManyToManyOIDRoot();
			test.otherData = "root insert 55";
			this.insertedData = ConfigureDb.da.insert(test);
			Assertions.assertNotNull(this.insertedData);
			Assertions.assertNotNull(this.insertedData.oid);
			Assertions.assertNull(this.insertedData.remote);

			// Try to retrieve all the data:
			final TypeManyToManyOIDRoot retrieve = ConfigureDb.da.get(TypeManyToManyOIDRoot.class,
					this.insertedData.oid);

			Assertions.assertNotNull(retrieve);
			Assertions.assertNotNull(retrieve.oid);
			Assertions.assertEquals(this.insertedData.oid, retrieve.oid);
			Assertions.assertNotNull(retrieve.otherData);
			Assertions.assertEquals(this.insertedData.otherData, retrieve.otherData);
			Assertions.assertNull(retrieve.remote);
		}

		@Order(3)
		@Test
		public void addLinksRemotes() throws Exception {
			// Add remote elements
			AddOnManyToMany.addLink(ConfigureDb.da, TypeManyToManyOIDRoot.class, this.insertedData.oid, "remote",
					this.insertedRemote1.oid);
			AddOnManyToMany.addLink(ConfigureDb.da, TypeManyToManyOIDRoot.class, this.insertedData.oid, "remote",
					this.insertedRemote2.oid);

			final TypeManyToManyOIDRoot retrieve = ConfigureDb.da.get(TypeManyToManyOIDRoot.class,
					this.insertedData.oid);

			Assertions.assertNotNull(retrieve);
			Assertions.assertNotNull(retrieve.oid);
			Assertions.assertEquals(this.insertedData.oid, retrieve.oid);
			Assertions.assertNotNull(retrieve.otherData);
			Assertions.assertEquals(this.insertedData.otherData, retrieve.otherData);
			Assertions.assertNotNull(retrieve.remote);
			Assertions.assertEquals(2, retrieve.remote.size());
			Assertions.assertEquals(retrieve.remote.get(0), this.insertedRemote1.oid);
			Assertions.assertEquals(retrieve.remote.get(1), this.insertedRemote2.oid);

			// -- Verify remote is linked:
			final TypeManyToManyOIDRemote retrieveRemote = ConfigureDb.da.get(TypeManyToManyOIDRemote.class,
					this.insertedRemote1.oid);

			Assertions.assertNotNull(retrieveRemote);
			Assertions.assertNotNull(retrieveRemote.oid);
			Assertions.assertEquals(this.insertedRemote1.oid, retrieveRemote.oid);
			Assertions.assertNotNull(retrieveRemote.data);
			Assertions.assertEquals(this.insertedRemote1.data, retrieveRemote.data);
			Assertions.assertNotNull(retrieveRemote.remoteToParent);
			Assertions.assertEquals(1, retrieveRemote.remoteToParent.size());
			Assertions.assertEquals(this.insertedData.oid, retrieveRemote.remoteToParent.get(0));
		}

		@Order(3)
		@Test
		public void testExpand() throws Exception {
			final TypeManyToManyOIDRootExpand retrieveExpand = ConfigureDb.da.get(TypeManyToManyOIDRootExpand.class,
					this.insertedData.oid);

			Assertions.assertNotNull(retrieveExpand);
			Assertions.assertNotNull(retrieveExpand.oid);
			Assertions.assertEquals(this.insertedData.oid, retrieveExpand.oid);
			Assertions.assertNotNull(retrieveExpand.otherData);
			Assertions.assertEquals(this.insertedData.otherData, retrieveExpand.otherData);
			Assertions.assertNotNull(retrieveExpand.remote);
			Assertions.assertEquals(2, retrieveExpand.remote.size());
			Assertions.assertEquals(retrieveExpand.remote.get(0).oid, this.insertedRemote1.oid);
			Assertions.assertEquals(retrieveExpand.remote.get(0).data, this.insertedRemote1.data);
			Assertions.assertEquals(retrieveExpand.remote.get(1).oid, this.insertedRemote2.oid);
			Assertions.assertEquals(retrieveExpand.remote.get(1).data, this.insertedRemote2.data);
		}

		@Order(4)
		@Test
		public void removeLinksRemotes() throws Exception {
			// Remove an element
			long count = AddOnManyToMany.removeLink(ConfigureDb.da, TypeManyToManyOIDRoot.class, this.insertedData.oid,
					"remote", this.insertedRemote1.oid);
			Assertions.assertEquals(1, count);

			TypeManyToManyOIDRoot retrieve = ConfigureDb.da.get(TypeManyToManyOIDRoot.class, this.insertedData.oid);

			Assertions.assertNotNull(retrieve);
			Assertions.assertNotNull(retrieve.oid);
			Assertions.assertEquals(this.insertedData.oid, retrieve.oid);
			Assertions.assertNotNull(retrieve.otherData);
			Assertions.assertEquals(this.insertedData.otherData, retrieve.otherData);
			Assertions.assertNotNull(retrieve.remote);
			Assertions.assertEquals(retrieve.remote.size(), 1);
			Assertions.assertEquals(retrieve.remote.get(0), this.insertedRemote2.oid);

			// Remove the second element
			count = AddOnManyToMany.removeLink(ConfigureDb.da, TypeManyToManyOIDRoot.class, retrieve.oid, "remote",
					this.insertedRemote2.oid);
			Assertions.assertEquals(1, count);

			retrieve = ConfigureDb.da.get(TypeManyToManyOIDRoot.class, this.insertedData.oid);

			Assertions.assertNotNull(retrieve);
			Assertions.assertNotNull(retrieve.oid);
			Assertions.assertEquals(this.insertedData.oid, retrieve.oid);
			Assertions.assertNotNull(retrieve.otherData);
			Assertions.assertEquals(this.insertedData.otherData, retrieve.otherData);
			Assertions.assertNull(retrieve.remote);

			ConfigureDb.da.delete(TypeManyToManyOIDRoot.class, this.insertedData.oid);
		}
	}

	@Order(4)
	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	class directInsertAndRemoveInRoot {
		TypeManyToManyOIDRemote insertedRemote1;
		TypeManyToManyOIDRemote insertedRemote2;
		TypeManyToManyOIDRoot insertedRoot1;
		TypeManyToManyOIDRoot insertedRoot2;

		@BeforeAll
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

		@AfterAll
		public void dropTables() throws Exception {
			if (ConfigureDb.da instanceof final DBAccessSQL daSQL) {
				daSQL.drop(TypeManyToManyOIDRoot.class);
				daSQL.drop(TypeManyToManyOIDRemote.class);
			}
		}

		// ---------------------------------------------------------------
		// -- Add remote:
		// ---------------------------------------------------------------
		@Order(1)
		@Test
		public void addRemotes() throws Exception {

			TypeManyToManyOIDRemote remote = new TypeManyToManyOIDRemote();
			for (int iii = 0; iii < 100; iii++) {
				remote.data = "tmp" + iii;
				this.insertedRemote1 = ConfigureDb.da.insert(remote);
				ConfigureDb.da.delete(TypeManyToManyOIDRemote.class, this.insertedRemote1.oid);
			}
			remote = new TypeManyToManyOIDRemote();
			remote.data = "remote 1";
			this.insertedRemote1 = ConfigureDb.da.insert(remote);
			Assertions.assertEquals(this.insertedRemote1.data, remote.data);

			remote = new TypeManyToManyOIDRemote();
			remote.data = "remote 2";
			this.insertedRemote2 = ConfigureDb.da.insert(remote);
			Assertions.assertEquals(this.insertedRemote2.data, remote.data);

			TypeManyToManyOIDRoot root = new TypeManyToManyOIDRoot();
			root.otherData = "root 1";
			this.insertedRoot1 = ConfigureDb.da.insert(root);

			root = new TypeManyToManyOIDRoot();
			root.otherData = "root 2";
			this.insertedRoot2 = ConfigureDb.da.insert(root);
		}

		@Order(3)
		@Test
		public void addLinksRemotes() throws Exception {
			// Add remote elements
			AddOnManyToMany.addLink(ConfigureDb.da, TypeManyToManyOIDRemote.class, this.insertedRemote2.oid,
					"remoteToParent", this.insertedRoot1.oid);
			AddOnManyToMany.addLink(ConfigureDb.da, TypeManyToManyOIDRemote.class, this.insertedRemote2.oid,
					"remoteToParent", this.insertedRoot2.oid);

			final TypeManyToManyOIDRemote retrieve = ConfigureDb.da.get(TypeManyToManyOIDRemote.class,
					this.insertedRemote2.oid);

			Assertions.assertNotNull(retrieve);
			Assertions.assertNotNull(retrieve.oid);
			Assertions.assertEquals(this.insertedRemote2.oid, retrieve.oid);
			Assertions.assertNotNull(retrieve.data);
			Assertions.assertEquals(this.insertedRemote2.data, retrieve.data);
			Assertions.assertNotNull(retrieve.remoteToParent);
			Assertions.assertEquals(2, retrieve.remoteToParent.size());
			Assertions.assertEquals(this.insertedRoot1.oid, retrieve.remoteToParent.get(0));
			Assertions.assertEquals(this.insertedRoot2.oid, retrieve.remoteToParent.get(1));

			final TypeManyToManyOIDRoot retrieveExpand = ConfigureDb.da.get(TypeManyToManyOIDRoot.class,
					this.insertedRoot1.oid);

			Assertions.assertNotNull(retrieveExpand);
			Assertions.assertNotNull(retrieveExpand.oid);
			Assertions.assertEquals(this.insertedRoot1.oid, retrieveExpand.oid);
			Assertions.assertNotNull(retrieveExpand.otherData);
			Assertions.assertEquals(this.insertedRoot1.otherData, retrieveExpand.otherData);
			Assertions.assertNotNull(retrieveExpand.remote);
			Assertions.assertEquals(1, retrieveExpand.remote.size());
			Assertions.assertEquals(this.insertedRemote2.oid, retrieveExpand.remote.get(0));

			// -- Verify remote is linked:
			final TypeManyToManyOIDRoot retrieveRemote = ConfigureDb.da.get(TypeManyToManyOIDRoot.class,
					this.insertedRoot2.oid);

			Assertions.assertNotNull(retrieveRemote);
			Assertions.assertNotNull(retrieveRemote.oid);
			Assertions.assertEquals(this.insertedRoot2.oid, retrieveRemote.oid);
			Assertions.assertNotNull(retrieveRemote.otherData);
			Assertions.assertEquals(this.insertedRoot2.otherData, retrieveRemote.otherData);
			Assertions.assertNotNull(retrieveRemote.remote);
			Assertions.assertEquals(1, retrieveRemote.remote.size());
			Assertions.assertEquals(this.insertedRemote2.oid, retrieveRemote.remote.get(0));
		}

		@Order(4)
		@Test
		public void removeLinksRemotes() throws Exception {
			// Remove root elements
			AddOnManyToMany.removeLink(ConfigureDb.da, TypeManyToManyOIDRemote.class, this.insertedRemote2.oid,
					"remoteToParent", this.insertedRoot2.oid);

			final TypeManyToManyOIDRemote retrieve = ConfigureDb.da.get(TypeManyToManyOIDRemote.class,
					this.insertedRemote2.oid);

			Assertions.assertNotNull(retrieve);
			Assertions.assertNotNull(retrieve.oid);
			Assertions.assertEquals(this.insertedRemote2.oid, retrieve.oid);
			Assertions.assertNotNull(retrieve.data);
			Assertions.assertEquals(this.insertedRemote2.data, retrieve.data);
			Assertions.assertNotNull(retrieve.remoteToParent);
			Assertions.assertEquals(1, retrieve.remoteToParent.size());
			Assertions.assertEquals(this.insertedRoot1.oid, retrieve.remoteToParent.get(0));

			// -- Verify remote is linked:
			final TypeManyToManyOIDRoot retrieveExpand = ConfigureDb.da.get(TypeManyToManyOIDRoot.class,
					this.insertedRoot1.oid);

			Assertions.assertNotNull(retrieveExpand);
			Assertions.assertNotNull(retrieveExpand.oid);
			Assertions.assertEquals(this.insertedRoot1.oid, retrieveExpand.oid);
			Assertions.assertNotNull(retrieveExpand.otherData);
			Assertions.assertEquals(this.insertedRoot1.otherData, retrieveExpand.otherData);
			Assertions.assertNotNull(retrieveExpand.remote);
			Assertions.assertEquals(1, retrieveExpand.remote.size());
			Assertions.assertEquals(this.insertedRemote2.oid, retrieveExpand.remote.get(0));

			// -- Verify remote is un-linked:
			final TypeManyToManyOIDRoot retrieveRemote = ConfigureDb.da.get(TypeManyToManyOIDRoot.class,
					this.insertedRoot2.oid);

			Assertions.assertNotNull(retrieveRemote);
			Assertions.assertNotNull(retrieveRemote.oid);
			Assertions.assertEquals(this.insertedRoot2.oid, retrieveRemote.oid);
			Assertions.assertNotNull(retrieveRemote.otherData);
			Assertions.assertEquals(this.insertedRoot2.otherData, retrieveRemote.otherData);
			Assertions.assertNull(retrieveRemote.remote);

		}

		@Order(5)
		@Test
		public void removeSecondLinksRemotes() throws Exception {
			// Remove root elements
			AddOnManyToMany.removeLink(ConfigureDb.da, TypeManyToManyOIDRemote.class, this.insertedRemote2.oid,
					"remoteToParent", this.insertedRoot1.oid);

			final TypeManyToManyOIDRemote retrieve = ConfigureDb.da.get(TypeManyToManyOIDRemote.class,
					this.insertedRemote2.oid);

			Assertions.assertNotNull(retrieve);
			Assertions.assertNotNull(retrieve.oid);
			Assertions.assertEquals(this.insertedRemote2.oid, retrieve.oid);
			Assertions.assertNotNull(retrieve.data);
			Assertions.assertEquals(this.insertedRemote2.data, retrieve.data);
			Assertions.assertNull(retrieve.remoteToParent);

			// -- Verify remote is linked:
			final TypeManyToManyOIDRootExpand retrieveExpand = ConfigureDb.da.get(TypeManyToManyOIDRootExpand.class,
					this.insertedRoot1.oid);

			Assertions.assertNotNull(retrieveExpand);
			Assertions.assertNotNull(retrieveExpand.oid);
			Assertions.assertEquals(this.insertedRoot1.oid, retrieveExpand.oid);
			Assertions.assertNotNull(retrieveExpand.otherData);
			Assertions.assertEquals(this.insertedRoot1.otherData, retrieveExpand.otherData);
			Assertions.assertNull(retrieveExpand.remote);

			// -- Verify remote is un-linked:
			final TypeManyToManyOIDRootExpand retrieveRemote = ConfigureDb.da.get(TypeManyToManyOIDRootExpand.class,
					this.insertedRoot2.oid);

			Assertions.assertNotNull(retrieveRemote);
			Assertions.assertNotNull(retrieveRemote.oid);
			Assertions.assertEquals(this.insertedRoot2.oid, retrieveRemote.oid);
			Assertions.assertNotNull(retrieveRemote.otherData);
			Assertions.assertEquals(this.insertedRoot2.otherData, retrieveRemote.otherData);
			Assertions.assertNull(retrieveRemote.remote);

		}

		TypeManyToManyOIDRemote insertedParameters;

		// ---------------------------------------------------------------
		// -- Add parent with manyToMany in parameters:
		// ---------------------------------------------------------------
		@Order(6)
		@Test
		public void AddParentWithManyToManyInParameters() throws Exception {
			final TypeManyToManyOIDRemote test = new TypeManyToManyOIDRemote();
			test.data = "insert with remote";
			test.remoteToParent = new ArrayList<>();
			test.remoteToParent.add(this.insertedRoot1.oid);
			test.remoteToParent.add(this.insertedRoot2.oid);
			this.insertedParameters = ConfigureDb.da.insert(test);
			Assertions.assertNotNull(this.insertedParameters);
			Assertions.assertNotNull(this.insertedParameters.oid);
			Assertions.assertNotNull(this.insertedParameters.remoteToParent);
			Assertions.assertEquals(2, this.insertedParameters.remoteToParent.size());
			Assertions.assertEquals(this.insertedRoot1.oid, this.insertedParameters.remoteToParent.get(0));
			Assertions.assertEquals(this.insertedRoot2.oid, this.insertedParameters.remoteToParent.get(1));

			// -- Verify remote is linked:
			TypeManyToManyOIDRoot retrieveRoot = ConfigureDb.da.get(TypeManyToManyOIDRoot.class,
					this.insertedRoot1.oid);

			Assertions.assertNotNull(retrieveRoot);
			Assertions.assertNotNull(retrieveRoot.oid);
			Assertions.assertEquals(this.insertedRoot1.oid, retrieveRoot.oid);
			Assertions.assertNotNull(retrieveRoot.otherData);
			Assertions.assertEquals(this.insertedRoot1.otherData, retrieveRoot.otherData);
			Assertions.assertNotNull(retrieveRoot.remote);
			Assertions.assertEquals(1, retrieveRoot.remote.size());
			Assertions.assertEquals(this.insertedParameters.oid, retrieveRoot.remote.get(0));

			retrieveRoot = ConfigureDb.da.get(TypeManyToManyOIDRoot.class, this.insertedRoot2.oid);

			Assertions.assertNotNull(retrieveRoot);
			Assertions.assertNotNull(retrieveRoot.oid);
			Assertions.assertEquals(this.insertedRoot2.oid, retrieveRoot.oid);
			Assertions.assertNotNull(retrieveRoot.otherData);
			Assertions.assertEquals(this.insertedRoot2.otherData, retrieveRoot.otherData);
			Assertions.assertNotNull(retrieveRoot.remote);
			Assertions.assertEquals(1, retrieveRoot.remote.size());
			Assertions.assertEquals(this.insertedParameters.oid, retrieveRoot.remote.get(0));

		}

		// ---------------------------------------------------------------
		// -- Update Parent Data:
		// ---------------------------------------------------------------
		@Order(7)
		@Test
		public void updateRequest() throws Exception {
			final TypeManyToManyOIDRemote testUpdate = new TypeManyToManyOIDRemote();
			testUpdate.remoteToParent = new ArrayList<>();
			testUpdate.remoteToParent.add(this.insertedRoot2.oid);
			final long numberUpdate = ConfigureDb.da.update(testUpdate, this.insertedParameters.oid);
			Assertions.assertEquals(1, numberUpdate);

			final TypeManyToManyOIDRemote insertedDataUpdate = ConfigureDb.da.get(TypeManyToManyOIDRemote.class,
					this.insertedParameters.oid);
			Assertions.assertNotNull(insertedDataUpdate);
			Assertions.assertNotNull(insertedDataUpdate.oid);
			Assertions.assertNotNull(insertedDataUpdate.remoteToParent);
			Assertions.assertEquals(1, insertedDataUpdate.remoteToParent.size());
			Assertions.assertEquals(this.insertedRoot2.oid, insertedDataUpdate.remoteToParent.get(0));

			// -- Verify remote is linked (removed):
			TypeManyToManyOIDRoot retrieveRoot = ConfigureDb.da.get(TypeManyToManyOIDRoot.class,
					this.insertedRoot1.oid);

			Assertions.assertNotNull(retrieveRoot);
			Assertions.assertNotNull(retrieveRoot.oid);
			Assertions.assertEquals(this.insertedRoot1.oid, retrieveRoot.oid);
			Assertions.assertNotNull(retrieveRoot.otherData);
			Assertions.assertEquals(this.insertedRoot1.otherData, retrieveRoot.otherData);
			Assertions.assertNull(retrieveRoot.remote);

			// -- Verify remote is linked (keep):
			retrieveRoot = ConfigureDb.da.get(TypeManyToManyOIDRoot.class, this.insertedRoot2.oid);

			Assertions.assertNotNull(retrieveRoot);
			Assertions.assertNotNull(retrieveRoot.oid);
			Assertions.assertEquals(this.insertedRoot2.oid, retrieveRoot.oid);
			Assertions.assertNotNull(retrieveRoot.otherData);
			Assertions.assertEquals(this.insertedRoot2.otherData, retrieveRoot.otherData);
			Assertions.assertNotNull(retrieveRoot.remote);
			Assertions.assertEquals(1, retrieveRoot.remote.size());
			Assertions.assertEquals(this.insertedParameters.oid, retrieveRoot.remote.get(0));

		}
	}
}
