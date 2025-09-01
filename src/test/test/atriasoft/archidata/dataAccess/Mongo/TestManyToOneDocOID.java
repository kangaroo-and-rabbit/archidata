package test.atriasoft.archidata.dataAccess.Mongo;

import java.io.IOException;
import java.util.List;

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
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import test.atriasoft.archidata.ConfigureDb;
import test.atriasoft.archidata.StepwiseExtension;
import test.atriasoft.archidata.dataAccess.model.TypeManyToOneDocOIDChildExpand;
import test.atriasoft.archidata.dataAccess.model.TypeManyToOneDocOIDChildFFF;
import test.atriasoft.archidata.dataAccess.model.TypeManyToOneDocOIDChildTTT;
import test.atriasoft.archidata.dataAccess.model.TypeManyToOneDocOIDParentDelete;
import test.atriasoft.archidata.dataAccess.model.TypeManyToOneDocOIDParentIgnore;
import test.atriasoft.archidata.dataAccess.model.TypeManyToOneDocOIDParentSetNull;

@ExtendWith(StepwiseExtension.class)
@EnabledIfEnvironmentVariable(named = "INCLUDE_MONGO_SPECIFIC", matches = "true")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestManyToOneDocOID {
	final static private Logger LOGGER = LoggerFactory.getLogger(TestManyToOneDocOID.class);

	@BeforeAll
	public void configureWebServer() throws Exception {
		ConfigureDb.configure();
	}

	@AfterAll
	public void removeDataBase() throws IOException {
		ConfigureDb.clear();
	}

	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	class ChildWillUpdateParents {
		private TypeManyToOneDocOIDParentIgnore insertedParent1;
		private TypeManyToOneDocOIDParentIgnore insertedParent2;
		private TypeManyToOneDocOIDChildTTT insertedChild1;

		@Order(1)
		@Test
		public void createParents() throws Exception {
			TypeManyToOneDocOIDParentIgnore parentToCreate = new TypeManyToOneDocOIDParentIgnore();
			parentToCreate.data = "parent1";
			insertedParent2 = ConfigureDb.da.insert(parentToCreate);
			Assertions.assertEquals(insertedParent2.data, parentToCreate.data);

			parentToCreate = new TypeManyToOneDocOIDParentIgnore();
			parentToCreate.data = "parent2";
			insertedParent1 = ConfigureDb.da.insert(parentToCreate);
			Assertions.assertEquals(insertedParent1.data, parentToCreate.data);
		}

		@Order(2)
		@Test
		public void insertChildWithUpdateParent() throws Exception {
			final TypeManyToOneDocOIDChildTTT childToInsert = new TypeManyToOneDocOIDChildTTT();
			childToInsert.otherData = "kjhlkjlkj";
			childToInsert.parentOid = insertedParent1.oid;
			// insert element
			insertedChild1 = ConfigureDb.da.insert(childToInsert);
			Assertions.assertNotNull(insertedChild1);
			Assertions.assertNotNull(insertedChild1.oid);
			Assertions.assertEquals(childToInsert.otherData, insertedChild1.otherData);
			Assertions.assertEquals(insertedParent1.oid, insertedChild1.parentOid);
		}

		@Order(3)
		@Test
		public void checkIfDataIsWellRetrieve() throws Exception {
			// check if retrieve is correct:
			TypeManyToOneDocOIDChildTTT retrieve = ConfigureDb.da.get(TypeManyToOneDocOIDChildTTT.class,
					insertedChild1.oid);
			Assertions.assertNotNull(retrieve);
			Assertions.assertNotNull(retrieve.oid);
			Assertions.assertEquals(insertedChild1.oid, retrieve.oid);
			Assertions.assertEquals(insertedChild1.otherData, retrieve.otherData);
			Assertions.assertEquals(insertedParent1.oid, retrieve.parentOid);
		}

		@Order(4)
		@Test
		public void checkIfDataIsWellExpand() throws Exception {
			// check if expand data is functional
			TypeManyToOneDocOIDChildExpand retrieve2 = ConfigureDb.da.get(TypeManyToOneDocOIDChildExpand.class,
					insertedChild1.oid);
			Assertions.assertNotNull(retrieve2);
			Assertions.assertNotNull(retrieve2.oid);
			Assertions.assertEquals(insertedChild1.oid, retrieve2.oid);
			Assertions.assertEquals(insertedChild1.otherData, retrieve2.otherData);
			Assertions.assertNotNull(retrieve2.parent);
			Assertions.assertEquals(insertedParent1.oid, retrieve2.parent.oid);
			Assertions.assertEquals(insertedParent1.data, retrieve2.parent.data);
		}

		@Order(5)
		@Test
		public void checkIfParentHasUpdateDatas() throws Exception {
			final TypeManyToOneDocOIDParentIgnore remoteCheck = ConfigureDb.da.get(
					TypeManyToOneDocOIDParentIgnore.class, insertedParent1.oid, new AccessDeletedItems(),
					new ReadAllColumn());
			Assertions.assertNotNull(remoteCheck);
			Assertions.assertNotNull(remoteCheck.oid);
			Assertions.assertEquals(insertedParent1.oid, remoteCheck.oid);
			Assertions.assertNotNull(remoteCheck.childOids);
			Assertions.assertEquals(1, remoteCheck.childOids.size());
			Assertions.assertEquals(insertedChild1.oid, remoteCheck.childOids.get(0));
			Assertions.assertNotNull(remoteCheck.createdAt);
			Assertions.assertNotNull(remoteCheck.updatedAt);
		}

		@Order(6)
		@Test
		public void updateChildWillUpdateParents() throws Exception {
			// Update child:
			TypeManyToOneDocOIDChildTTT childToUpdate = new TypeManyToOneDocOIDChildTTT(insertedChild1.otherData,
					insertedParent2.oid);
			long count = ConfigureDb.da.update(childToUpdate, insertedChild1.oid);
			Assertions.assertEquals(1, count);
			insertedChild1 = ConfigureDb.da.get(TypeManyToOneDocOIDChildTTT.class, insertedChild1.oid);
			Assertions.assertNotNull(insertedChild1);
			Assertions.assertNotNull(insertedChild1.oid);
			Assertions.assertEquals(insertedParent2.oid, insertedChild1.parentOid);

			// check if parent are well updated:
			// no more child:
			TypeManyToOneDocOIDParentIgnore remoteCheck = ConfigureDb.da.get(TypeManyToOneDocOIDParentIgnore.class,
					insertedParent1.oid, new AccessDeletedItems(), new ReadAllColumn());
			Assertions.assertNotNull(remoteCheck);
			Assertions.assertNotNull(remoteCheck.oid);
			Assertions.assertEquals(insertedParent1.oid, remoteCheck.oid);
			Assertions.assertNull(remoteCheck.childOids);
			Assertions.assertNotNull(remoteCheck.createdAt);
			Assertions.assertNotNull(remoteCheck.updatedAt);
			// new child:
			remoteCheck = ConfigureDb.da.get(TypeManyToOneDocOIDParentIgnore.class, insertedParent2.oid,
					new AccessDeletedItems(), new ReadAllColumn());
			Assertions.assertNotNull(remoteCheck);
			Assertions.assertNotNull(remoteCheck.oid);
			Assertions.assertEquals(insertedParent2.oid, remoteCheck.oid);
			Assertions.assertNotNull(remoteCheck.childOids);
			Assertions.assertEquals(1, remoteCheck.childOids.size());
			Assertions.assertEquals(insertedChild1.oid, remoteCheck.childOids.get(0));
			Assertions.assertNotNull(remoteCheck.createdAt);
			Assertions.assertNotNull(remoteCheck.updatedAt);
		}

		@Order(7)
		@Test
		public void deleteChildWillUpdateParents() throws Exception {
			// Update child:
			final long count = ConfigureDb.da.delete(TypeManyToOneDocOIDChildTTT.class, insertedChild1.oid);
			Assertions.assertEquals(1, count);

			// check if parent are well updated:
			// no more child:
			TypeManyToOneDocOIDParentIgnore remoteCheck = ConfigureDb.da.get(TypeManyToOneDocOIDParentIgnore.class,
					insertedParent2.oid, new AccessDeletedItems(), new ReadAllColumn());
			Assertions.assertNotNull(remoteCheck);
			Assertions.assertNotNull(remoteCheck.oid);
			Assertions.assertNull(remoteCheck.childOids);
			Assertions.assertNotNull(remoteCheck.createdAt);
			Assertions.assertNotNull(remoteCheck.updatedAt);
		}
	}

	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	class ChildWillNotUpdateParents {
		private TypeManyToOneDocOIDParentIgnore insertedParent1;
		private TypeManyToOneDocOIDParentIgnore insertedParent2;
		private TypeManyToOneDocOIDChildFFF insertedChild1;

		@Order(1)
		@Test
		public void createParents() throws Exception {
			TypeManyToOneDocOIDParentIgnore parentToCreate = new TypeManyToOneDocOIDParentIgnore();
			parentToCreate.data = "parent1";
			insertedParent2 = ConfigureDb.da.insert(parentToCreate);
			Assertions.assertEquals(insertedParent2.data, parentToCreate.data);

			parentToCreate = new TypeManyToOneDocOIDParentIgnore();
			parentToCreate.data = "parent2";
			insertedParent1 = ConfigureDb.da.insert(parentToCreate);
			Assertions.assertEquals(insertedParent1.data, parentToCreate.data);
		}

		@Order(2)
		@Test
		public void insertChildWithoutUpdateParent() throws Exception {
			final TypeManyToOneDocOIDChildFFF childToInsert = new TypeManyToOneDocOIDChildFFF();
			childToInsert.otherData = "kjhlkjlkj";
			childToInsert.parentOid = insertedParent1.oid;
			// insert element
			insertedChild1 = ConfigureDb.da.insert(childToInsert);
			Assertions.assertNotNull(insertedChild1);
			Assertions.assertNotNull(insertedChild1.oid);
			Assertions.assertEquals(childToInsert.otherData, insertedChild1.otherData);
			Assertions.assertEquals(insertedParent1.oid, insertedChild1.parentOid);
		}

		@Order(3)
		@Test
		public void checkIfDataIsWellRetrieve() throws Exception {
			// check if retrieve is correct:
			TypeManyToOneDocOIDChildFFF retrieve = ConfigureDb.da.get(TypeManyToOneDocOIDChildFFF.class,
					insertedChild1.oid);
			Assertions.assertNotNull(retrieve);
			Assertions.assertNotNull(retrieve.oid);
			Assertions.assertEquals(insertedChild1.oid, retrieve.oid);
			Assertions.assertEquals(insertedChild1.otherData, retrieve.otherData);
			Assertions.assertEquals(insertedParent1.oid, retrieve.parentOid);
		}

		@Order(5)
		@Test
		public void checkIfParentHasNotUpdateDatas() throws Exception {
			final TypeManyToOneDocOIDParentIgnore parentCheck = ConfigureDb.da.get(
					TypeManyToOneDocOIDParentIgnore.class, insertedParent1.oid, new AccessDeletedItems(),
					new ReadAllColumn());
			Assertions.assertNotNull(parentCheck);
			Assertions.assertNotNull(parentCheck.oid);
			Assertions.assertEquals(insertedParent1.oid, parentCheck.oid);
			Assertions.assertNull(parentCheck.childOids);
			Assertions.assertNotNull(parentCheck.createdAt);
			Assertions.assertNotNull(parentCheck.updatedAt);
		}

		@Order(6)
		@Test
		public void updateChildWillNotUpdateParents() throws Exception {
			// need force the update of the parent to be sure it not not update nothing..
			TypeManyToOneDocOIDParentIgnore parentUpdate = new TypeManyToOneDocOIDParentIgnore("parent1",
					List.of(insertedChild1.oid));
			long count = ConfigureDb.da.update(parentUpdate, insertedParent1.oid);
			Assertions.assertEquals(1, count);

			// Update child (migrate parent1 to Parent2):
			TypeManyToOneDocOIDChildFFF childToUpdate = new TypeManyToOneDocOIDChildFFF(insertedChild1.otherData,
					insertedParent2.oid);
			count = ConfigureDb.da.update(childToUpdate, insertedChild1.oid);
			Assertions.assertEquals(1, count);
			insertedChild1 = ConfigureDb.da.get(TypeManyToOneDocOIDChildFFF.class, insertedChild1.oid);
			Assertions.assertNotNull(insertedChild1);
			Assertions.assertNotNull(insertedChild1.oid);
			Assertions.assertEquals(insertedParent2.oid, insertedChild1.parentOid);

			// check if parent are well updated:
			// no more child but no update then present:
			TypeManyToOneDocOIDParentIgnore parentCheck = ConfigureDb.da.get(TypeManyToOneDocOIDParentIgnore.class,
					insertedParent1.oid, new AccessDeletedItems(), new ReadAllColumn());
			Assertions.assertNotNull(parentCheck);
			Assertions.assertNotNull(parentCheck.oid);
			Assertions.assertEquals(insertedParent1.oid, parentCheck.oid);
			Assertions.assertNotNull(parentCheck.childOids);
			Assertions.assertEquals(1, parentCheck.childOids.size()); // keep the previous value..
			Assertions.assertEquals(insertedChild1.oid, parentCheck.childOids.get(0));
			Assertions.assertNotNull(parentCheck.createdAt);
			Assertions.assertNotNull(parentCheck.updatedAt);
			// new child but no update
			parentCheck = ConfigureDb.da.get(TypeManyToOneDocOIDParentIgnore.class, insertedParent2.oid,
					new AccessDeletedItems(), new ReadAllColumn());
			Assertions.assertNotNull(parentCheck);
			Assertions.assertNotNull(parentCheck.oid);
			Assertions.assertEquals(insertedParent2.oid, parentCheck.oid);
			Assertions.assertNull(parentCheck.childOids);
			Assertions.assertNotNull(parentCheck.createdAt);
			Assertions.assertNotNull(parentCheck.updatedAt);

			// set back on first for the next step
			childToUpdate = new TypeManyToOneDocOIDChildFFF(insertedChild1.otherData, insertedParent1.oid);
			count = ConfigureDb.da.update(childToUpdate, insertedChild1.oid);
		}

		@Order(7)
		@Test
		public void deleteChildWillNotUpdateParents() throws Exception {
			// Update child:
			long count = ConfigureDb.da.delete(TypeManyToOneDocOIDChildFFF.class, insertedChild1.oid);
			Assertions.assertEquals(1, count);

			// check if parent are well updated:
			// no more child but not update then 1 present:
			TypeManyToOneDocOIDParentIgnore parentCheck = ConfigureDb.da.get(TypeManyToOneDocOIDParentIgnore.class,
					insertedParent1.oid, new AccessDeletedItems(), new ReadAllColumn());
			Assertions.assertNotNull(parentCheck);
			Assertions.assertNotNull(parentCheck.oid);
			Assertions.assertEquals(insertedParent1.oid, parentCheck.oid);
			Assertions.assertNotNull(parentCheck.childOids);
			Assertions.assertEquals(1, parentCheck.childOids.size());
			Assertions.assertEquals(insertedChild1.oid, parentCheck.childOids.get(0));
			Assertions.assertNotNull(parentCheck.createdAt);
			Assertions.assertNotNull(parentCheck.updatedAt);
		}
	}

	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	class ParentWillNotUpdateTheChilds {
		private TypeManyToOneDocOIDChildTTT insertedChild1;
		private TypeManyToOneDocOIDChildTTT insertedChild2;
		private TypeManyToOneDocOIDParentIgnore insertedParent1;

		@Order(1)
		@Test
		public void createEmptyChilds() throws Exception {
			TypeManyToOneDocOIDChildTTT childToInsert = new TypeManyToOneDocOIDChildTTT("child 1", null);
			// insert element
			insertedChild1 = ConfigureDb.da.insert(childToInsert);
			Assertions.assertNotNull(insertedChild1);
			childToInsert = new TypeManyToOneDocOIDChildTTT("child 2", null);
			// insert element
			insertedChild2 = ConfigureDb.da.insert(childToInsert);
			Assertions.assertNotNull(insertedChild2);
		}

		@Order(2)
		@Test
		public void insertParentWillNotUpdateChilds() throws Exception {
			TypeManyToOneDocOIDParentIgnore parentToInsert = new TypeManyToOneDocOIDParentIgnore("parent 1",
					List.of(insertedChild1.oid));
			// insert element
			insertedParent1 = ConfigureDb.da.insert(parentToInsert);
			Assertions.assertNotNull(insertedParent1);
			Assertions.assertNotNull(insertedParent1.oid);
			Assertions.assertEquals(parentToInsert.data, insertedParent1.data);
			Assertions.assertNotNull(insertedParent1.childOids);
			Assertions.assertEquals(1, insertedParent1.childOids.size());
			Assertions.assertEquals(insertedChild1.oid, insertedParent1.childOids.get(0));

			// check if child is update
			TypeManyToOneDocOIDChildTTT child1Check = ConfigureDb.da.get(TypeManyToOneDocOIDChildTTT.class,
					insertedChild1.oid, new AccessDeletedItems(), new ReadAllColumn());
			Assertions.assertNotNull(child1Check);
			Assertions.assertNull(child1Check.parentOid);

		}

		@Order(3)
		@Test
		public void updateParentWillNotUpdateChilds() throws Exception {
			TypeManyToOneDocOIDParentIgnore parentToUpdate = new TypeManyToOneDocOIDParentIgnore("parent 1",
					List.of(insertedChild2.oid));
			// insert element
			insertedParent1 = ConfigureDb.da.insert(parentToUpdate);
			long count = ConfigureDb.da.update(parentToUpdate, insertedParent1.oid);
			Assertions.assertEquals(1, count);
			Assertions.assertNotNull(insertedParent1);
			Assertions.assertNotNull(insertedParent1.oid);
			Assertions.assertEquals(insertedParent1.data, parentToUpdate.data);
			Assertions.assertNotNull(insertedParent1.childOids);
			Assertions.assertEquals(1, insertedParent1.childOids.size());
			Assertions.assertEquals(insertedChild2.oid, insertedParent1.childOids.get(0));
			// check if child is update
			TypeManyToOneDocOIDChildTTT child1Check = ConfigureDb.da.get(TypeManyToOneDocOIDChildTTT.class,
					insertedChild1.oid, new AccessDeletedItems(), new ReadAllColumn());
			Assertions.assertNotNull(child1Check);
			Assertions.assertNull(child1Check.parentOid);

			// check if child is update
			TypeManyToOneDocOIDChildTTT child2Check = ConfigureDb.da.get(TypeManyToOneDocOIDChildTTT.class,
					insertedChild1.oid, new AccessDeletedItems(), new ReadAllColumn());
			Assertions.assertNotNull(child2Check);
			Assertions.assertNull(child2Check.parentOid);
		}

		@Order(4)
		@Test
		public void deleteParentWillNotUpdateChilds() throws Exception {

			long count = ConfigureDb.da.delete(TypeManyToOneDocOIDParentIgnore.class, insertedParent1.oid);
			Assertions.assertEquals(1, count);

			// check if child is update
			TypeManyToOneDocOIDChildTTT child1Check = ConfigureDb.da.get(TypeManyToOneDocOIDChildTTT.class,
					insertedChild1.oid, new AccessDeletedItems(), new ReadAllColumn());
			Assertions.assertNotNull(child1Check);
			Assertions.assertNull(child1Check.parentOid);

			// check if child is update
			TypeManyToOneDocOIDChildTTT child2Check = ConfigureDb.da.get(TypeManyToOneDocOIDChildTTT.class,
					insertedChild1.oid, new AccessDeletedItems(), new ReadAllColumn());
			Assertions.assertNotNull(child2Check);
			Assertions.assertNull(child2Check.parentOid);
		}

	}

	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	class ParentWillUpdateTheChilds {
		private TypeManyToOneDocOIDChildTTT insertedChild1;
		private TypeManyToOneDocOIDChildTTT insertedChild2;
		private TypeManyToOneDocOIDParentSetNull insertedParent1;

		@Order(1)
		@Test
		public void createEmptyChilds() throws Exception {
			TypeManyToOneDocOIDChildTTT childToInsert = new TypeManyToOneDocOIDChildTTT("child 1", null);
			// insert element
			insertedChild1 = ConfigureDb.da.insert(childToInsert);
			Assertions.assertNotNull(insertedChild1);
			childToInsert = new TypeManyToOneDocOIDChildTTT("child 2", null);
			// insert element
			insertedChild2 = ConfigureDb.da.insert(childToInsert);
			Assertions.assertNotNull(insertedChild2);
		}

		@Order(2)
		@Test
		public void insertParentWillUpdateChilds() throws Exception {
			TypeManyToOneDocOIDParentSetNull parentToInsert = new TypeManyToOneDocOIDParentSetNull("parent 1",
					List.of(insertedChild1.oid));
			// insert element
			insertedParent1 = ConfigureDb.da.insert(parentToInsert);
			Assertions.assertNotNull(insertedParent1);
			Assertions.assertNotNull(insertedParent1.oid);
			Assertions.assertEquals(parentToInsert.data, insertedParent1.data);
			Assertions.assertNotNull(insertedParent1.childOids);
			Assertions.assertEquals(1, insertedParent1.childOids.size());
			Assertions.assertEquals(insertedChild1.oid, insertedParent1.childOids.get(0));

			// check if child is update
			TypeManyToOneDocOIDChildTTT child1Check = ConfigureDb.da.get(TypeManyToOneDocOIDChildTTT.class,
					insertedChild1.oid, new AccessDeletedItems(), new ReadAllColumn());
			Assertions.assertNotNull(child1Check);
			Assertions.assertNotNull(child1Check.parentOid);
			Assertions.assertEquals(insertedParent1.oid, child1Check.parentOid);

		}

		@Order(3)
		@Test
		public void updateParentWillUpdateChilds() throws Exception {
			TypeManyToOneDocOIDParentSetNull parentToUpdate = new TypeManyToOneDocOIDParentSetNull("parent 1",
					List.of(insertedChild2.oid));
			// insert element
			long count = ConfigureDb.da.update(parentToUpdate, insertedParent1.oid);
			Assertions.assertEquals(1, count);
			insertedParent1 = ConfigureDb.da.get(TypeManyToOneDocOIDParentSetNull.class, insertedParent1.oid);
			Assertions.assertNotNull(insertedParent1);
			Assertions.assertNotNull(insertedParent1.oid);
			Assertions.assertEquals(insertedParent1.data, parentToUpdate.data);
			Assertions.assertNotNull(insertedParent1.childOids);
			Assertions.assertEquals(1, insertedParent1.childOids.size());
			Assertions.assertEquals(insertedChild2.oid, insertedParent1.childOids.get(0));
			// check if child is update
			TypeManyToOneDocOIDChildTTT child1Check = ConfigureDb.da.get(TypeManyToOneDocOIDChildTTT.class,
					insertedChild1.oid, new AccessDeletedItems(), new ReadAllColumn());
			Assertions.assertNotNull(child1Check);
			Assertions.assertNull(child1Check.parentOid);

			// check if child is update
			TypeManyToOneDocOIDChildTTT child2Check = ConfigureDb.da.get(TypeManyToOneDocOIDChildTTT.class,
					insertedChild2.oid, new AccessDeletedItems(), new ReadAllColumn());
			Assertions.assertNotNull(child2Check);
			Assertions.assertNotNull(child2Check.parentOid);
			Assertions.assertEquals(insertedParent1.oid, child2Check.parentOid);
		}

		@Order(4)
		@Test
		public void deleteParentWillUpdateChilds() throws Exception {

			long count = ConfigureDb.da.delete(TypeManyToOneDocOIDParentSetNull.class, insertedParent1.oid);
			Assertions.assertEquals(1, count);

			// check if child is update
			TypeManyToOneDocOIDChildTTT child1Check = ConfigureDb.da.get(TypeManyToOneDocOIDChildTTT.class,
					insertedChild1.oid, new AccessDeletedItems(), new ReadAllColumn());
			Assertions.assertNotNull(child1Check);
			Assertions.assertNull(child1Check.parentOid);

			// check if child is update
			TypeManyToOneDocOIDChildTTT child2Check = ConfigureDb.da.get(TypeManyToOneDocOIDChildTTT.class,
					insertedChild2.oid, new AccessDeletedItems(), new ReadAllColumn());
			Assertions.assertNotNull(child2Check);
			Assertions.assertNull(child2Check.parentOid);
		}

	}

	@Nested
	@TestInstance(TestInstance.Lifecycle.PER_CLASS)
	@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
	class ParentWillDeleteTheChilds {
		private TypeManyToOneDocOIDChildTTT insertedChild1;
		private TypeManyToOneDocOIDChildTTT insertedChild2;
		private TypeManyToOneDocOIDParentDelete insertedParent1;

		@Order(1)
		@Test
		public void createEmptyChilds() throws Exception {
			TypeManyToOneDocOIDChildTTT childToInsert = new TypeManyToOneDocOIDChildTTT("child 1", null);
			// insert element
			insertedChild1 = ConfigureDb.da.insert(childToInsert);
			Assertions.assertNotNull(insertedChild1);
			childToInsert = new TypeManyToOneDocOIDChildTTT("child 2", null);
			// insert element
			insertedChild2 = ConfigureDb.da.insert(childToInsert);
			Assertions.assertNotNull(insertedChild2);
		}

		@Order(2)
		@Test
		public void insertParentWillUpdateChilds() throws Exception {
			TypeManyToOneDocOIDParentDelete parentToInsert = new TypeManyToOneDocOIDParentDelete("parent 1",
					List.of(insertedChild1.oid));
			// insert element
			insertedParent1 = ConfigureDb.da.insert(parentToInsert);
			Assertions.assertNotNull(insertedParent1);
			Assertions.assertNotNull(insertedParent1.oid);
			Assertions.assertEquals(parentToInsert.data, insertedParent1.data);
			Assertions.assertNotNull(insertedParent1.childOids);
			Assertions.assertEquals(1, insertedParent1.childOids.size());
			Assertions.assertEquals(insertedChild1.oid, insertedParent1.childOids.get(0));

			// check if child is update
			TypeManyToOneDocOIDChildTTT child1Check = ConfigureDb.da.get(TypeManyToOneDocOIDChildTTT.class,
					insertedChild1.oid, new AccessDeletedItems(), new ReadAllColumn());
			Assertions.assertNotNull(child1Check);
			Assertions.assertNotNull(child1Check.parentOid);
			Assertions.assertEquals(insertedParent1.oid, child1Check.parentOid);

		}

		@Order(3)
		@Test
		public void updateParentWillDeleteChilds() throws Exception {
			TypeManyToOneDocOIDParentDelete parentToUpdate = new TypeManyToOneDocOIDParentDelete("parent 1",
					List.of(insertedChild2.oid));
			// insert element
			long count = ConfigureDb.da.update(parentToUpdate, insertedParent1.oid);
			Assertions.assertEquals(1, count);
			insertedParent1 = ConfigureDb.da.get(TypeManyToOneDocOIDParentDelete.class, insertedParent1.oid);
			Assertions.assertNotNull(insertedParent1);
			Assertions.assertNotNull(insertedParent1.oid);
			Assertions.assertEquals(insertedParent1.data, parentToUpdate.data);
			Assertions.assertNotNull(insertedParent1.childOids);
			Assertions.assertEquals(1, insertedParent1.childOids.size());
			Assertions.assertEquals(insertedChild2.oid, insertedParent1.childOids.get(0));
			// check if child is removed
			TypeManyToOneDocOIDChildTTT child1Check = ConfigureDb.da.get(TypeManyToOneDocOIDChildTTT.class,
					insertedChild1.oid, new AccessDeletedItems(), new ReadAllColumn());
			Assertions.assertNull(child1Check);

			// check if child is update
			TypeManyToOneDocOIDChildTTT child2Check = ConfigureDb.da.get(TypeManyToOneDocOIDChildTTT.class,
					insertedChild2.oid, new AccessDeletedItems(), new ReadAllColumn());
			Assertions.assertNotNull(child2Check);
			Assertions.assertNotNull(child2Check.parentOid);
			Assertions.assertEquals(insertedParent1.oid, child2Check.parentOid);
		}

		@Order(4)
		@Test
		public void deleteParentWillNotUpdateChilds() throws Exception {
			long count = ConfigureDb.da.delete(TypeManyToOneDocOIDParentDelete.class, insertedParent1.oid);
			Assertions.assertEquals(1, count);
			// check if child is removed
			TypeManyToOneDocOIDChildTTT child2Check = ConfigureDb.da.get(TypeManyToOneDocOIDChildTTT.class,
					insertedChild1.oid, new AccessDeletedItems(), new ReadAllColumn());
			Assertions.assertNull(child2Check);
		}

	}

}
