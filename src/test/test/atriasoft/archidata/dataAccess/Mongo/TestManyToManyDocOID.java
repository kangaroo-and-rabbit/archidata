package test.atriasoft.archidata.dataAccess.Mongo;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import org.atriasoft.archidata.dataAccess.commonTools.ManyToManyTools;
import org.atriasoft.archidata.dataAccess.options.AccessDeletedItems;
import org.atriasoft.archidata.dataAccess.options.ReadAllColumn;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import test.atriasoft.archidata.ConfigureDb;
import test.atriasoft.archidata.StepwiseExtension;
import test.atriasoft.archidata.dataAccess.model.TypeManyToManyDocOIDRemote;
import test.atriasoft.archidata.dataAccess.model.TypeManyToManyDocOIDRoot;
import test.atriasoft.archidata.dataAccess.model.TypeManyToManyDocOIDRootExpand;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestManyToManyDocOID {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestManyToManyDocOID.class);

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
		public void testCreateTable() throws Exception {}

		@AfterAll
		public void dropTables() throws Exception {}

		@Order(2)
		@Test
		public void testSimpleInsertAndRetieve() throws Exception {
			final TypeManyToManyDocOIDRoot test = new TypeManyToManyDocOIDRoot();
			test.otherData = "root insert";
			final TypeManyToManyDocOIDRoot insertedData = ConfigureDb.da.insert(test);
			Assertions.assertNotNull(insertedData);
			Assertions.assertNotNull(insertedData.getOid());
			Assertions.assertNull(insertedData.remote);

			// Try to retrieve all the data:
			final TypeManyToManyDocOIDRoot retrieve = ConfigureDb.da.getById(TypeManyToManyDocOIDRoot.class,
					insertedData.getOid());

			Assertions.assertNotNull(retrieve);
			Assertions.assertNotNull(retrieve.getOid());
			Assertions.assertEquals(insertedData.getOid(), retrieve.getOid());
			Assertions.assertNotNull(retrieve.otherData);
			Assertions.assertEquals(insertedData.otherData, retrieve.otherData);
			Assertions.assertNull(retrieve.remote);

			ConfigureDb.da.deleteById(TypeManyToManyDocOIDRoot.class, insertedData.getOid());
		}
	}

	// TODO: add and remove link from remote class
	@Order(3)
	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	class AddLinkInsertInRoot {
		TypeManyToManyDocOIDRemote insertedRemote1;
		TypeManyToManyDocOIDRemote insertedRemote2;
		TypeManyToManyDocOIDRoot insertedData;

		@BeforeAll
		public void testCreateTable() throws Exception {}

		@AfterAll
		public void dropTables() throws Exception {}

		// ---------------------------------------------------------------
		// -- Add remote:
		// ---------------------------------------------------------------
		@Order(1)
		@Test
		public void addRemotes() throws Exception {

			TypeManyToManyDocOIDRemote remote = new TypeManyToManyDocOIDRemote();
			for (int iii = 0; iii < 100; iii++) {
				remote.data = "tmp" + iii;
				this.insertedRemote1 = ConfigureDb.da.insert(remote);
				ConfigureDb.da.deleteById(TypeManyToManyDocOIDRemote.class, this.insertedRemote1.getOid());
			}
			remote = new TypeManyToManyDocOIDRemote();
			remote.data = "remote1";
			this.insertedRemote1 = ConfigureDb.da.insert(remote);
			Assertions.assertEquals(this.insertedRemote1.data, remote.data);

			remote = new TypeManyToManyDocOIDRemote();
			remote.data = "remote2";
			this.insertedRemote2 = ConfigureDb.da.insert(remote);
			Assertions.assertEquals(this.insertedRemote2.data, remote.data);
		}

		@Order(2)
		@Test
		public void insertDataWithoutRemote() throws Exception {

			final TypeManyToManyDocOIDRoot test = new TypeManyToManyDocOIDRoot();
			test.otherData = "root insert 55";
			this.insertedData = ConfigureDb.da.insert(test);
			Assertions.assertNotNull(this.insertedData);
			Assertions.assertNotNull(this.insertedData.getOid());
			Assertions.assertNull(this.insertedData.remote);

			// Try to retrieve all the data:
			final TypeManyToManyDocOIDRoot retrieve = ConfigureDb.da.getById(TypeManyToManyDocOIDRoot.class,
					this.insertedData.getOid());

			Assertions.assertNotNull(retrieve);
			Assertions.assertNotNull(retrieve.getOid());
			Assertions.assertEquals(this.insertedData.getOid(), retrieve.getOid());
			Assertions.assertNotNull(retrieve.otherData);
			Assertions.assertEquals(this.insertedData.otherData, retrieve.otherData);
			Assertions.assertNull(retrieve.remote);
		}

		@Order(3)
		@Test
		public void addLinksRemotes() throws Exception {
			// Add remote elements
			ManyToManyTools.addLink(ConfigureDb.da, //
					TypeManyToManyDocOIDRoot.class, //
					this.insertedData.getOid(), //
					"remote", this.insertedRemote1.getOid());
			Thread.sleep(150);
			ManyToManyTools.addLink(ConfigureDb.da, //
					TypeManyToManyDocOIDRoot.class, //
					this.insertedData.getOid(), //
					"remote", this.insertedRemote2.getOid());

			final TypeManyToManyDocOIDRoot retrieve = ConfigureDb.da.getById(TypeManyToManyDocOIDRoot.class,
					this.insertedData.getOid(), new AccessDeletedItems(), new ReadAllColumn());

			Assertions.assertNotNull(retrieve);
			Assertions.assertNotNull(retrieve.getOid());
			Assertions.assertEquals(this.insertedData.getOid(), retrieve.getOid());
			Assertions.assertNotNull(retrieve.otherData);
			Assertions.assertEquals(this.insertedData.otherData, retrieve.otherData);
			Assertions.assertNotNull(retrieve.remote);
			Assertions.assertEquals(2, retrieve.remote.size());
			Assertions.assertEquals(retrieve.remote.get(0), this.insertedRemote1.getOid());
			Assertions.assertEquals(retrieve.remote.get(1), this.insertedRemote2.getOid());
			Assertions.assertNotNull(retrieve.getCreatedAt());
			Assertions.assertNotNull(retrieve.getUpdatedAt());
			final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
			final String formattedCreatedAt = sdf.format(retrieve.getCreatedAt());
			final String formattedUpdatedAt = sdf.format(retrieve.getUpdatedAt());
			LOGGER.info("check: {} =?= {}", formattedCreatedAt, formattedUpdatedAt);
			Assertions.assertTrue(formattedUpdatedAt.compareTo(formattedCreatedAt) > 0);

			// -- Verify remote is linked:
			final TypeManyToManyDocOIDRemote retrieveRemote = ConfigureDb.da.getById(TypeManyToManyDocOIDRemote.class,
					this.insertedRemote1.getOid());

			Assertions.assertNotNull(retrieveRemote);
			Assertions.assertNotNull(retrieveRemote.getOid());
			Assertions.assertEquals(this.insertedRemote1.getOid(), retrieveRemote.getOid());
			Assertions.assertNotNull(retrieveRemote.data);
			Assertions.assertEquals(this.insertedRemote1.data, retrieveRemote.data);
			Assertions.assertNotNull(retrieveRemote.remoteToParent);
			Assertions.assertEquals(1, retrieveRemote.remoteToParent.size());
			Assertions.assertEquals(this.insertedData.getOid(), retrieveRemote.remoteToParent.get(0));
		}

		@Order(3)
		@Test
		public void testExpand() throws Exception {
			final TypeManyToManyDocOIDRootExpand retrieveExpand = ConfigureDb.da
					.getById(TypeManyToManyDocOIDRootExpand.class, this.insertedData.getOid());

			Assertions.assertNotNull(retrieveExpand);
			Assertions.assertNotNull(retrieveExpand.getOid());
			Assertions.assertEquals(this.insertedData.getOid(), retrieveExpand.getOid());
			Assertions.assertNotNull(retrieveExpand.otherData);
			Assertions.assertEquals(this.insertedData.otherData, retrieveExpand.otherData);
			Assertions.assertNotNull(retrieveExpand.remote);
			Assertions.assertEquals(2, retrieveExpand.remote.size());
			Assertions.assertEquals(retrieveExpand.remote.get(0).getOid(), this.insertedRemote1.getOid());
			Assertions.assertEquals(retrieveExpand.remote.get(0).data, this.insertedRemote1.data);
			Assertions.assertEquals(retrieveExpand.remote.get(1).getOid(), this.insertedRemote2.getOid());
			Assertions.assertEquals(retrieveExpand.remote.get(1).data, this.insertedRemote2.data);
		}

		@Order(4)
		@Test
		public void removeLinksRemotes() throws Exception {
			// Remove an element
			ManyToManyTools.removeLink(ConfigureDb.da, TypeManyToManyDocOIDRoot.class, //
					this.insertedData.getOid(), //
					"remote", this.insertedRemote1.getOid());

			TypeManyToManyDocOIDRoot retrieve = ConfigureDb.da.getById(TypeManyToManyDocOIDRoot.class,
					this.insertedData.getOid());

			Assertions.assertNotNull(retrieve);
			Assertions.assertNotNull(retrieve.getOid());
			Assertions.assertEquals(this.insertedData.getOid(), retrieve.getOid());
			Assertions.assertNotNull(retrieve.otherData);
			Assertions.assertEquals(this.insertedData.otherData, retrieve.otherData);
			Assertions.assertNotNull(retrieve.remote);
			Assertions.assertEquals(retrieve.remote.size(), 1);
			Assertions.assertEquals(retrieve.remote.get(0), this.insertedRemote2.getOid());

			// Remove the second element
			ManyToManyTools.removeLink(ConfigureDb.da, TypeManyToManyDocOIDRoot.class, //
					retrieve.getOid(), //
					"remote", this.insertedRemote2.getOid());

			retrieve = ConfigureDb.da.getById(TypeManyToManyDocOIDRoot.class, this.insertedData.getOid());

			Assertions.assertNotNull(retrieve);
			Assertions.assertNotNull(retrieve.getOid());
			Assertions.assertEquals(this.insertedData.getOid(), retrieve.getOid());
			Assertions.assertNotNull(retrieve.otherData);
			Assertions.assertEquals(this.insertedData.otherData, retrieve.otherData);
			Assertions.assertNull(retrieve.remote);

			ConfigureDb.da.deleteById(TypeManyToManyDocOIDRoot.class, this.insertedData.getOid());
		}
	}

	@Order(4)
	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	class directInsertAndRemoveInRoot {
		TypeManyToManyDocOIDRemote insertedRemote1;
		TypeManyToManyDocOIDRemote insertedRemote2;
		TypeManyToManyDocOIDRoot insertedRoot1;
		TypeManyToManyDocOIDRoot insertedRoot2;

		@BeforeAll
		public void testCreateTable() throws Exception {}

		@AfterAll
		public void dropTables() throws Exception {}

		// ---------------------------------------------------------------
		// -- Add remote:
		// ---------------------------------------------------------------
		@Order(1)
		@Test
		public void addRemotes() throws Exception {

			TypeManyToManyDocOIDRemote remote = new TypeManyToManyDocOIDRemote();
			for (int iii = 0; iii < 100; iii++) {
				remote.data = "tmp" + iii;
				this.insertedRemote1 = ConfigureDb.da.insert(remote);
				ConfigureDb.da.deleteById(TypeManyToManyDocOIDRemote.class, this.insertedRemote1.getOid());
			}
			remote = new TypeManyToManyDocOIDRemote();
			remote.data = "remote 1";
			this.insertedRemote1 = ConfigureDb.da.insert(remote);
			Assertions.assertEquals(this.insertedRemote1.data, remote.data);

			remote = new TypeManyToManyDocOIDRemote();
			remote.data = "remote 2";
			this.insertedRemote2 = ConfigureDb.da.insert(remote);
			Assertions.assertEquals(this.insertedRemote2.data, remote.data);

			TypeManyToManyDocOIDRoot root = new TypeManyToManyDocOIDRoot();
			root.otherData = "root 1";
			this.insertedRoot1 = ConfigureDb.da.insert(root);

			root = new TypeManyToManyDocOIDRoot();
			root.otherData = "root 2";
			this.insertedRoot2 = ConfigureDb.da.insert(root);
		}

		@Order(3)
		@Test
		public void addLinksRemotes() throws Exception {
			// Add remote elements
			ManyToManyTools.addLink(ConfigureDb.da, TypeManyToManyDocOIDRemote.class, //
					this.insertedRemote2.getOid(), //
					"remoteToParent", this.insertedRoot1.getOid());
			ManyToManyTools.addLink(ConfigureDb.da, TypeManyToManyDocOIDRemote.class, //
					this.insertedRemote2.getOid(), //
					"remoteToParent", this.insertedRoot2.getOid());

			final TypeManyToManyDocOIDRemote retrieve = ConfigureDb.da.getById(TypeManyToManyDocOIDRemote.class,
					this.insertedRemote2.getOid());

			Assertions.assertNotNull(retrieve);
			Assertions.assertNotNull(retrieve.getOid());
			Assertions.assertEquals(this.insertedRemote2.getOid(), retrieve.getOid());
			Assertions.assertNotNull(retrieve.data);
			Assertions.assertEquals(this.insertedRemote2.data, retrieve.data);
			Assertions.assertNotNull(retrieve.remoteToParent);
			Assertions.assertEquals(2, retrieve.remoteToParent.size());
			Assertions.assertEquals(this.insertedRoot1.getOid(), retrieve.remoteToParent.get(0));
			Assertions.assertEquals(this.insertedRoot2.getOid(), retrieve.remoteToParent.get(1));

			final TypeManyToManyDocOIDRoot retrieveExpand = ConfigureDb.da.getById(TypeManyToManyDocOIDRoot.class,
					this.insertedRoot1.getOid());

			Assertions.assertNotNull(retrieveExpand);
			Assertions.assertNotNull(retrieveExpand.getOid());
			Assertions.assertEquals(this.insertedRoot1.getOid(), retrieveExpand.getOid());
			Assertions.assertNotNull(retrieveExpand.otherData);
			Assertions.assertEquals(this.insertedRoot1.otherData, retrieveExpand.otherData);
			Assertions.assertNotNull(retrieveExpand.remote);
			Assertions.assertEquals(1, retrieveExpand.remote.size());
			Assertions.assertEquals(this.insertedRemote2.getOid(), retrieveExpand.remote.get(0));

			// -- Verify remote is linked:
			final TypeManyToManyDocOIDRoot retrieveRemote = ConfigureDb.da.getById(TypeManyToManyDocOIDRoot.class,
					this.insertedRoot2.getOid());

			Assertions.assertNotNull(retrieveRemote);
			Assertions.assertNotNull(retrieveRemote.getOid());
			Assertions.assertEquals(this.insertedRoot2.getOid(), retrieveRemote.getOid());
			Assertions.assertNotNull(retrieveRemote.otherData);
			Assertions.assertEquals(this.insertedRoot2.otherData, retrieveRemote.otherData);
			Assertions.assertNotNull(retrieveRemote.remote);
			Assertions.assertEquals(1, retrieveRemote.remote.size());
			Assertions.assertEquals(this.insertedRemote2.getOid(), retrieveRemote.remote.get(0));
		}

		@Order(4)
		@Test
		public void removeLinksRemotes() throws Exception {
			// Remove root elements
			ManyToManyTools.removeLink(ConfigureDb.da, TypeManyToManyDocOIDRemote.class, //
					this.insertedRemote2.getOid(), //
					"remoteToParent", this.insertedRoot2.getOid());

			final TypeManyToManyDocOIDRemote retrieve = ConfigureDb.da.getById(TypeManyToManyDocOIDRemote.class,
					this.insertedRemote2.getOid());

			Assertions.assertNotNull(retrieve);
			Assertions.assertNotNull(retrieve.getOid());
			Assertions.assertEquals(this.insertedRemote2.getOid(), retrieve.getOid());
			Assertions.assertNotNull(retrieve.data);
			Assertions.assertEquals(this.insertedRemote2.data, retrieve.data);
			Assertions.assertNotNull(retrieve.remoteToParent);
			Assertions.assertEquals(1, retrieve.remoteToParent.size());
			Assertions.assertEquals(this.insertedRoot1.getOid(), retrieve.remoteToParent.get(0));

			// -- Verify remote is linked:
			final TypeManyToManyDocOIDRoot retrieveExpand = ConfigureDb.da.getById(TypeManyToManyDocOIDRoot.class,
					this.insertedRoot1.getOid());

			Assertions.assertNotNull(retrieveExpand);
			Assertions.assertNotNull(retrieveExpand.getOid());
			Assertions.assertEquals(this.insertedRoot1.getOid(), retrieveExpand.getOid());
			Assertions.assertNotNull(retrieveExpand.otherData);
			Assertions.assertEquals(this.insertedRoot1.otherData, retrieveExpand.otherData);
			Assertions.assertNotNull(retrieveExpand.remote);
			Assertions.assertEquals(1, retrieveExpand.remote.size());
			Assertions.assertEquals(this.insertedRemote2.getOid(), retrieveExpand.remote.get(0));

			// -- Verify remote is un-linked:
			final TypeManyToManyDocOIDRoot retrieveRemote = ConfigureDb.da.getById(TypeManyToManyDocOIDRoot.class,
					this.insertedRoot2.getOid());

			Assertions.assertNotNull(retrieveRemote);
			Assertions.assertNotNull(retrieveRemote.getOid());
			Assertions.assertEquals(this.insertedRoot2.getOid(), retrieveRemote.getOid());
			Assertions.assertNotNull(retrieveRemote.otherData);
			Assertions.assertEquals(this.insertedRoot2.otherData, retrieveRemote.otherData);
			Assertions.assertNull(retrieveRemote.remote);

		}

		@Order(5)
		@Test
		public void removeSecondLinksRemotes() throws Exception {
			// Remove root elements
			ManyToManyTools.removeLink(ConfigureDb.da, TypeManyToManyDocOIDRemote.class, //
					this.insertedRemote2.getOid(), //
					"remoteToParent", this.insertedRoot1.getOid());

			final TypeManyToManyDocOIDRemote retrieve = ConfigureDb.da.getById(TypeManyToManyDocOIDRemote.class,
					this.insertedRemote2.getOid());

			Assertions.assertNotNull(retrieve);
			Assertions.assertNotNull(retrieve.getOid());
			Assertions.assertEquals(this.insertedRemote2.getOid(), retrieve.getOid());
			Assertions.assertNotNull(retrieve.data);
			Assertions.assertEquals(this.insertedRemote2.data, retrieve.data);
			Assertions.assertNull(retrieve.remoteToParent);

			// -- Verify remote is linked:
			final TypeManyToManyDocOIDRootExpand retrieveExpand = ConfigureDb.da
					.getById(TypeManyToManyDocOIDRootExpand.class, this.insertedRoot1.getOid());

			Assertions.assertNotNull(retrieveExpand);
			Assertions.assertNotNull(retrieveExpand.getOid());
			Assertions.assertEquals(this.insertedRoot1.getOid(), retrieveExpand.getOid());
			Assertions.assertNotNull(retrieveExpand.otherData);
			Assertions.assertEquals(this.insertedRoot1.otherData, retrieveExpand.otherData);
			Assertions.assertNull(retrieveExpand.remote);

			// -- Verify remote is un-linked:
			final TypeManyToManyDocOIDRootExpand retrieveRemote = ConfigureDb.da
					.getById(TypeManyToManyDocOIDRootExpand.class, this.insertedRoot2.getOid());

			Assertions.assertNotNull(retrieveRemote);
			Assertions.assertNotNull(retrieveRemote.getOid());
			Assertions.assertEquals(this.insertedRoot2.getOid(), retrieveRemote.getOid());
			Assertions.assertNotNull(retrieveRemote.otherData);
			Assertions.assertEquals(this.insertedRoot2.otherData, retrieveRemote.otherData);
			Assertions.assertNull(retrieveRemote.remote);

		}

		TypeManyToManyDocOIDRemote insertedParameters;

		// ---------------------------------------------------------------
		// -- Add parent with manyToMany in parameters:
		// ---------------------------------------------------------------
		@Order(6)
		@Test
		public void AddParentWithManyToManyInParameters() throws Exception {
			final TypeManyToManyDocOIDRemote test = new TypeManyToManyDocOIDRemote();
			test.data = "insert with remote";
			test.remoteToParent = new ArrayList<>();
			test.remoteToParent.add(this.insertedRoot1.getOid());
			test.remoteToParent.add(this.insertedRoot2.getOid());
			this.insertedParameters = ConfigureDb.da.insert(test);
			Assertions.assertNotNull(this.insertedParameters);
			Assertions.assertNotNull(this.insertedParameters.getOid());
			Assertions.assertNotNull(this.insertedParameters.remoteToParent);
			Assertions.assertEquals(2, this.insertedParameters.remoteToParent.size());
			Assertions.assertEquals(this.insertedRoot1.getOid(), this.insertedParameters.remoteToParent.get(0));
			Assertions.assertEquals(this.insertedRoot2.getOid(), this.insertedParameters.remoteToParent.get(1));

			// -- Verify remote is linked:
			TypeManyToManyDocOIDRoot retrieveRoot = ConfigureDb.da.getById(TypeManyToManyDocOIDRoot.class,
					this.insertedRoot1.getOid());

			Assertions.assertNotNull(retrieveRoot);
			Assertions.assertNotNull(retrieveRoot.getOid());
			Assertions.assertEquals(this.insertedRoot1.getOid(), retrieveRoot.getOid());
			Assertions.assertNotNull(retrieveRoot.otherData);
			Assertions.assertEquals(this.insertedRoot1.otherData, retrieveRoot.otherData);
			Assertions.assertNotNull(retrieveRoot.remote);
			Assertions.assertEquals(1, retrieveRoot.remote.size());
			Assertions.assertEquals(this.insertedParameters.getOid(), retrieveRoot.remote.get(0));

			retrieveRoot = ConfigureDb.da.getById(TypeManyToManyDocOIDRoot.class, this.insertedRoot2.getOid());

			Assertions.assertNotNull(retrieveRoot);
			Assertions.assertNotNull(retrieveRoot.getOid());
			Assertions.assertEquals(this.insertedRoot2.getOid(), retrieveRoot.getOid());
			Assertions.assertNotNull(retrieveRoot.otherData);
			Assertions.assertEquals(this.insertedRoot2.otherData, retrieveRoot.otherData);
			Assertions.assertNotNull(retrieveRoot.remote);
			Assertions.assertEquals(1, retrieveRoot.remote.size());
			Assertions.assertEquals(this.insertedParameters.getOid(), retrieveRoot.remote.get(0));

		}

		// ---------------------------------------------------------------
		// -- Update Parent Data:
		// ---------------------------------------------------------------
		@Order(7)
		@Test
		public void updateRequest() throws Exception {
			final TypeManyToManyDocOIDRemote testUpdate = new TypeManyToManyDocOIDRemote();
			testUpdate.remoteToParent = new ArrayList<>();
			testUpdate.remoteToParent.add(this.insertedRoot2.getOid());
			final long numberUpdate = ConfigureDb.da.updateById(testUpdate, this.insertedParameters.getOid());
			Assertions.assertEquals(1, numberUpdate);

			final TypeManyToManyDocOIDRemote insertedDataUpdate = ConfigureDb.da
					.getById(TypeManyToManyDocOIDRemote.class, this.insertedParameters.getOid());
			Assertions.assertNotNull(insertedDataUpdate);
			Assertions.assertNotNull(insertedDataUpdate.getOid());
			Assertions.assertNotNull(insertedDataUpdate.remoteToParent);
			Assertions.assertEquals(1, insertedDataUpdate.remoteToParent.size());
			Assertions.assertEquals(this.insertedRoot2.getOid(), insertedDataUpdate.remoteToParent.get(0));

			// -- Verify remote is linked (removed):
			TypeManyToManyDocOIDRoot retrieveRoot = ConfigureDb.da.getById(TypeManyToManyDocOIDRoot.class,
					this.insertedRoot1.getOid());

			Assertions.assertNotNull(retrieveRoot);
			Assertions.assertNotNull(retrieveRoot.getOid());
			Assertions.assertEquals(this.insertedRoot1.getOid(), retrieveRoot.getOid());
			Assertions.assertNotNull(retrieveRoot.otherData);
			Assertions.assertEquals(this.insertedRoot1.otherData, retrieveRoot.otherData);
			Assertions.assertNull(retrieveRoot.remote);

			// -- Verify remote is linked (keep):
			retrieveRoot = ConfigureDb.da.getById(TypeManyToManyDocOIDRoot.class, this.insertedRoot2.getOid());

			Assertions.assertNotNull(retrieveRoot);
			Assertions.assertNotNull(retrieveRoot.getOid());
			Assertions.assertEquals(this.insertedRoot2.getOid(), retrieveRoot.getOid());
			Assertions.assertNotNull(retrieveRoot.otherData);
			Assertions.assertEquals(this.insertedRoot2.otherData, retrieveRoot.otherData);
			Assertions.assertNotNull(retrieveRoot.remote);
			Assertions.assertEquals(1, retrieveRoot.remote.size());
			Assertions.assertEquals(this.insertedParameters.getOid(), retrieveRoot.remote.get(0));

		}
	}
}
