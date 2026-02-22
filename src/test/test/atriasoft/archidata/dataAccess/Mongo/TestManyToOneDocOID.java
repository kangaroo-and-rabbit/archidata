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
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestManyToOneDocOID {
	private static final Logger LOGGER = LoggerFactory.getLogger(TestManyToOneDocOID.class);

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
			this.insertedParent2 = ConfigureDb.da.insert(parentToCreate);
			Assertions.assertEquals(this.insertedParent2.data, parentToCreate.data);

			parentToCreate = new TypeManyToOneDocOIDParentIgnore();
			parentToCreate.data = "parent2";
			this.insertedParent1 = ConfigureDb.da.insert(parentToCreate);
			Assertions.assertEquals(this.insertedParent1.data, parentToCreate.data);
		}

		@Order(2)
		@Test
		public void insertChildWithUpdateParent() throws Exception {
			final TypeManyToOneDocOIDChildTTT childToInsert = new TypeManyToOneDocOIDChildTTT();
			childToInsert.otherData = "kjhlkjlkj";
			childToInsert.parentOid = this.insertedParent1.oid;
			// insert element
			this.insertedChild1 = ConfigureDb.da.insert(childToInsert);
			Assertions.assertNotNull(this.insertedChild1);
			Assertions.assertNotNull(this.insertedChild1.oid);
			Assertions.assertEquals(childToInsert.otherData, this.insertedChild1.otherData);
			Assertions.assertEquals(this.insertedParent1.oid, this.insertedChild1.parentOid);
		}

		@Order(3)
		@Test
		public void checkIfDataIsWellRetrieve() throws Exception {
			// check if retrieve is correct:
			final TypeManyToOneDocOIDChildTTT retrieve = ConfigureDb.da.getById(TypeManyToOneDocOIDChildTTT.class,
					this.insertedChild1.oid);
			Assertions.assertNotNull(retrieve);
			Assertions.assertNotNull(retrieve.oid);
			Assertions.assertEquals(this.insertedChild1.oid, retrieve.oid);
			Assertions.assertEquals(this.insertedChild1.otherData, retrieve.otherData);
			Assertions.assertEquals(this.insertedParent1.oid, retrieve.parentOid);
		}

		@Order(4)
		@Test
		public void checkIfDataIsWellExpand() throws Exception {
			// check if expand data is functional
			final TypeManyToOneDocOIDChildExpand retrieve2 = ConfigureDb.da
					.getById(TypeManyToOneDocOIDChildExpand.class, this.insertedChild1.oid);
			Assertions.assertNotNull(retrieve2);
			Assertions.assertNotNull(retrieve2.oid);
			Assertions.assertEquals(this.insertedChild1.oid, retrieve2.oid);
			Assertions.assertEquals(this.insertedChild1.otherData, retrieve2.otherData);
			Assertions.assertNotNull(retrieve2.parent);
			Assertions.assertEquals(this.insertedParent1.oid, retrieve2.parent.oid);
			Assertions.assertEquals(this.insertedParent1.data, retrieve2.parent.data);
		}

		@Order(5)
		@Test
		public void checkIfParentHasUpdateDatas() throws Exception {
			final TypeManyToOneDocOIDParentIgnore remoteCheck = ConfigureDb.da.getById(
					TypeManyToOneDocOIDParentIgnore.class, this.insertedParent1.oid, new AccessDeletedItems(),
					new ReadAllColumn());
			Assertions.assertNotNull(remoteCheck);
			Assertions.assertNotNull(remoteCheck.oid);
			Assertions.assertEquals(this.insertedParent1.oid, remoteCheck.oid);
			Assertions.assertNotNull(remoteCheck.childOids);
			Assertions.assertEquals(1, remoteCheck.childOids.size());
			Assertions.assertEquals(this.insertedChild1.oid, remoteCheck.childOids.get(0));
			Assertions.assertNotNull(remoteCheck.createdAt);
			Assertions.assertNotNull(remoteCheck.updatedAt);
		}

		@Order(6)
		@Test
		public void updateChildWillUpdateParents() throws Exception {
			// Update child:
			final TypeManyToOneDocOIDChildTTT childToUpdate = new TypeManyToOneDocOIDChildTTT(
					this.insertedChild1.otherData, this.insertedParent2.oid);
			final long count = ConfigureDb.da.updateById(childToUpdate, this.insertedChild1.oid);
			Assertions.assertEquals(1, count);
			this.insertedChild1 = ConfigureDb.da.getById(TypeManyToOneDocOIDChildTTT.class, this.insertedChild1.oid);
			Assertions.assertNotNull(this.insertedChild1);
			Assertions.assertNotNull(this.insertedChild1.oid);
			Assertions.assertEquals(this.insertedParent2.oid, this.insertedChild1.parentOid);

			// check if parent are well updated:
			// no more child:
			TypeManyToOneDocOIDParentIgnore remoteCheck = ConfigureDb.da.getById(TypeManyToOneDocOIDParentIgnore.class,
					this.insertedParent1.oid, new AccessDeletedItems(), new ReadAllColumn());
			Assertions.assertNotNull(remoteCheck);
			Assertions.assertNotNull(remoteCheck.oid);
			Assertions.assertEquals(this.insertedParent1.oid, remoteCheck.oid);
			Assertions.assertNull(remoteCheck.childOids);
			Assertions.assertNotNull(remoteCheck.createdAt);
			Assertions.assertNotNull(remoteCheck.updatedAt);
			// new child:
			remoteCheck = ConfigureDb.da.getById(TypeManyToOneDocOIDParentIgnore.class, this.insertedParent2.oid,
					new AccessDeletedItems(), new ReadAllColumn());
			Assertions.assertNotNull(remoteCheck);
			Assertions.assertNotNull(remoteCheck.oid);
			Assertions.assertEquals(this.insertedParent2.oid, remoteCheck.oid);
			Assertions.assertNotNull(remoteCheck.childOids);
			Assertions.assertEquals(1, remoteCheck.childOids.size());
			Assertions.assertEquals(this.insertedChild1.oid, remoteCheck.childOids.get(0));
			Assertions.assertNotNull(remoteCheck.createdAt);
			Assertions.assertNotNull(remoteCheck.updatedAt);
		}

		@Order(7)
		@Test
		public void deleteChildWillUpdateParents() throws Exception {
			// Update child:
			final long count = ConfigureDb.da.deleteById(TypeManyToOneDocOIDChildTTT.class, this.insertedChild1.oid);
			Assertions.assertEquals(1, count);

			// check if parent are well updated:
			// no more child:
			final TypeManyToOneDocOIDParentIgnore remoteCheck = ConfigureDb.da.getById(
					TypeManyToOneDocOIDParentIgnore.class, this.insertedParent2.oid, new AccessDeletedItems(),
					new ReadAllColumn());
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
			this.insertedParent2 = ConfigureDb.da.insert(parentToCreate);
			Assertions.assertEquals(this.insertedParent2.data, parentToCreate.data);

			parentToCreate = new TypeManyToOneDocOIDParentIgnore();
			parentToCreate.data = "parent2";
			this.insertedParent1 = ConfigureDb.da.insert(parentToCreate);
			Assertions.assertEquals(this.insertedParent1.data, parentToCreate.data);
		}

		@Order(2)
		@Test
		public void insertChildWithoutUpdateParent() throws Exception {
			final TypeManyToOneDocOIDChildFFF childToInsert = new TypeManyToOneDocOIDChildFFF();
			childToInsert.otherData = "kjhlkjlkj";
			childToInsert.parentOid = this.insertedParent1.oid;
			// insert element
			this.insertedChild1 = ConfigureDb.da.insert(childToInsert);
			Assertions.assertNotNull(this.insertedChild1);
			Assertions.assertNotNull(this.insertedChild1.oid);
			Assertions.assertEquals(childToInsert.otherData, this.insertedChild1.otherData);
			Assertions.assertEquals(this.insertedParent1.oid, this.insertedChild1.parentOid);
		}

		@Order(3)
		@Test
		public void checkIfDataIsWellRetrieve() throws Exception {
			// check if retrieve is correct:
			final TypeManyToOneDocOIDChildFFF retrieve = ConfigureDb.da.getById(TypeManyToOneDocOIDChildFFF.class,
					this.insertedChild1.oid);
			Assertions.assertNotNull(retrieve);
			Assertions.assertNotNull(retrieve.oid);
			Assertions.assertEquals(this.insertedChild1.oid, retrieve.oid);
			Assertions.assertEquals(this.insertedChild1.otherData, retrieve.otherData);
			Assertions.assertEquals(this.insertedParent1.oid, retrieve.parentOid);
		}

		@Order(5)
		@Test
		public void checkIfParentHasNotUpdateDatas() throws Exception {
			final TypeManyToOneDocOIDParentIgnore parentCheck = ConfigureDb.da.getById(
					TypeManyToOneDocOIDParentIgnore.class, this.insertedParent1.oid, new AccessDeletedItems(),
					new ReadAllColumn());
			Assertions.assertNotNull(parentCheck);
			Assertions.assertNotNull(parentCheck.oid);
			Assertions.assertEquals(this.insertedParent1.oid, parentCheck.oid);
			Assertions.assertNull(parentCheck.childOids);
			Assertions.assertNotNull(parentCheck.createdAt);
			Assertions.assertNotNull(parentCheck.updatedAt);
		}

		@Order(6)
		@Test
		public void updateChildWillNotUpdateParents() throws Exception {
			// need force the update of the parent to be sure it not not update nothing..
			final TypeManyToOneDocOIDParentIgnore parentUpdate = new TypeManyToOneDocOIDParentIgnore("parent1",
					List.of(this.insertedChild1.oid));
			long count = ConfigureDb.da.updateById(parentUpdate, this.insertedParent1.oid);
			Assertions.assertEquals(1, count);

			// Update child (migrate parent1 to Parent2):
			TypeManyToOneDocOIDChildFFF childToUpdate = new TypeManyToOneDocOIDChildFFF(this.insertedChild1.otherData,
					this.insertedParent2.oid);
			count = ConfigureDb.da.updateById(childToUpdate, this.insertedChild1.oid);
			Assertions.assertEquals(1, count);
			this.insertedChild1 = ConfigureDb.da.getById(TypeManyToOneDocOIDChildFFF.class, this.insertedChild1.oid);
			Assertions.assertNotNull(this.insertedChild1);
			Assertions.assertNotNull(this.insertedChild1.oid);
			Assertions.assertEquals(this.insertedParent2.oid, this.insertedChild1.parentOid);

			// check if parent are well updated:
			// no more child but no update then present:
			TypeManyToOneDocOIDParentIgnore parentCheck = ConfigureDb.da.getById(TypeManyToOneDocOIDParentIgnore.class,
					this.insertedParent1.oid, new AccessDeletedItems(), new ReadAllColumn());
			Assertions.assertNotNull(parentCheck);
			Assertions.assertNotNull(parentCheck.oid);
			Assertions.assertEquals(this.insertedParent1.oid, parentCheck.oid);
			Assertions.assertNotNull(parentCheck.childOids);
			Assertions.assertEquals(1, parentCheck.childOids.size()); // keep the previous value..
			Assertions.assertEquals(this.insertedChild1.oid, parentCheck.childOids.get(0));
			Assertions.assertNotNull(parentCheck.createdAt);
			Assertions.assertNotNull(parentCheck.updatedAt);
			// new child but no update
			parentCheck = ConfigureDb.da.getById(TypeManyToOneDocOIDParentIgnore.class, this.insertedParent2.oid,
					new AccessDeletedItems(), new ReadAllColumn());
			Assertions.assertNotNull(parentCheck);
			Assertions.assertNotNull(parentCheck.oid);
			Assertions.assertEquals(this.insertedParent2.oid, parentCheck.oid);
			Assertions.assertNull(parentCheck.childOids);
			Assertions.assertNotNull(parentCheck.createdAt);
			Assertions.assertNotNull(parentCheck.updatedAt);

			// set back on first for the next step
			childToUpdate = new TypeManyToOneDocOIDChildFFF(this.insertedChild1.otherData, this.insertedParent1.oid);
			count = ConfigureDb.da.updateById(childToUpdate, this.insertedChild1.oid);
		}

		@Order(7)
		@Test
		public void deleteChildWillNotUpdateParents() throws Exception {
			// Update child:
			final long count = ConfigureDb.da.deleteById(TypeManyToOneDocOIDChildFFF.class, this.insertedChild1.oid);
			Assertions.assertEquals(1, count);

			// check if parent are well updated:
			// no more child but not update then 1 present:
			final TypeManyToOneDocOIDParentIgnore parentCheck = ConfigureDb.da.getById(
					TypeManyToOneDocOIDParentIgnore.class, this.insertedParent1.oid, new AccessDeletedItems(),
					new ReadAllColumn());
			Assertions.assertNotNull(parentCheck);
			Assertions.assertNotNull(parentCheck.oid);
			Assertions.assertEquals(this.insertedParent1.oid, parentCheck.oid);
			Assertions.assertNotNull(parentCheck.childOids);
			Assertions.assertEquals(1, parentCheck.childOids.size());
			Assertions.assertEquals(this.insertedChild1.oid, parentCheck.childOids.get(0));
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
			this.insertedChild1 = ConfigureDb.da.insert(childToInsert);
			Assertions.assertNotNull(this.insertedChild1);
			childToInsert = new TypeManyToOneDocOIDChildTTT("child 2", null);
			// insert element
			this.insertedChild2 = ConfigureDb.da.insert(childToInsert);
			Assertions.assertNotNull(this.insertedChild2);
		}

		@Order(2)
		@Test
		public void insertParentWillNotUpdateChilds() throws Exception {
			final TypeManyToOneDocOIDParentIgnore parentToInsert = new TypeManyToOneDocOIDParentIgnore("parent 1",
					List.of(this.insertedChild1.oid));
			// insert element
			this.insertedParent1 = ConfigureDb.da.insert(parentToInsert);
			Assertions.assertNotNull(this.insertedParent1);
			Assertions.assertNotNull(this.insertedParent1.oid);
			Assertions.assertEquals(parentToInsert.data, this.insertedParent1.data);
			Assertions.assertNotNull(this.insertedParent1.childOids);
			Assertions.assertEquals(1, this.insertedParent1.childOids.size());
			Assertions.assertEquals(this.insertedChild1.oid, this.insertedParent1.childOids.get(0));

			// check if child is update
			final TypeManyToOneDocOIDChildTTT child1Check = ConfigureDb.da.getById(TypeManyToOneDocOIDChildTTT.class,
					this.insertedChild1.oid, new AccessDeletedItems(), new ReadAllColumn());
			Assertions.assertNotNull(child1Check);
			Assertions.assertNull(child1Check.parentOid);

		}

		@Order(3)
		@Test
		public void updateParentWillNotUpdateChilds() throws Exception {
			final TypeManyToOneDocOIDParentIgnore parentToUpdate = new TypeManyToOneDocOIDParentIgnore("parent 1",
					List.of(this.insertedChild2.oid));
			// insert element
			this.insertedParent1 = ConfigureDb.da.insert(parentToUpdate);
			final long count = ConfigureDb.da.updateById(parentToUpdate, this.insertedParent1.oid);
			Assertions.assertEquals(1, count);
			Assertions.assertNotNull(this.insertedParent1);
			Assertions.assertNotNull(this.insertedParent1.oid);
			Assertions.assertEquals(this.insertedParent1.data, parentToUpdate.data);
			Assertions.assertNotNull(this.insertedParent1.childOids);
			Assertions.assertEquals(1, this.insertedParent1.childOids.size());
			Assertions.assertEquals(this.insertedChild2.oid, this.insertedParent1.childOids.get(0));
			// check if child is update
			final TypeManyToOneDocOIDChildTTT child1Check = ConfigureDb.da.getById(TypeManyToOneDocOIDChildTTT.class,
					this.insertedChild1.oid, new AccessDeletedItems(), new ReadAllColumn());
			Assertions.assertNotNull(child1Check);
			Assertions.assertNull(child1Check.parentOid);

			// check if child is update
			final TypeManyToOneDocOIDChildTTT child2Check = ConfigureDb.da.getById(TypeManyToOneDocOIDChildTTT.class,
					this.insertedChild1.oid, new AccessDeletedItems(), new ReadAllColumn());
			Assertions.assertNotNull(child2Check);
			Assertions.assertNull(child2Check.parentOid);
		}

		@Order(4)
		@Test
		public void deleteParentWillNotUpdateChilds() throws Exception {

			final long count = ConfigureDb.da.deleteById(TypeManyToOneDocOIDParentIgnore.class,
					this.insertedParent1.oid);
			Assertions.assertEquals(1, count);

			// check if child is update
			final TypeManyToOneDocOIDChildTTT child1Check = ConfigureDb.da.getById(TypeManyToOneDocOIDChildTTT.class,
					this.insertedChild1.oid, new AccessDeletedItems(), new ReadAllColumn());
			Assertions.assertNotNull(child1Check);
			Assertions.assertNull(child1Check.parentOid);

			// check if child is update
			final TypeManyToOneDocOIDChildTTT child2Check = ConfigureDb.da.getById(TypeManyToOneDocOIDChildTTT.class,
					this.insertedChild1.oid, new AccessDeletedItems(), new ReadAllColumn());
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
			this.insertedChild1 = ConfigureDb.da.insert(childToInsert);
			Assertions.assertNotNull(this.insertedChild1);
			childToInsert = new TypeManyToOneDocOIDChildTTT("child 2", null);
			// insert element
			this.insertedChild2 = ConfigureDb.da.insert(childToInsert);
			Assertions.assertNotNull(this.insertedChild2);
		}

		@Order(2)
		@Test
		public void insertParentWillUpdateChilds() throws Exception {
			final TypeManyToOneDocOIDParentSetNull parentToInsert = new TypeManyToOneDocOIDParentSetNull("parent 1",
					List.of(this.insertedChild1.oid));
			// insert element
			this.insertedParent1 = ConfigureDb.da.insert(parentToInsert);
			Assertions.assertNotNull(this.insertedParent1);
			Assertions.assertNotNull(this.insertedParent1.oid);
			Assertions.assertEquals(parentToInsert.data, this.insertedParent1.data);
			Assertions.assertNotNull(this.insertedParent1.childOids);
			Assertions.assertEquals(1, this.insertedParent1.childOids.size());
			Assertions.assertEquals(this.insertedChild1.oid, this.insertedParent1.childOids.get(0));

			// check if child is update
			final TypeManyToOneDocOIDChildTTT child1Check = ConfigureDb.da.getById(TypeManyToOneDocOIDChildTTT.class,
					this.insertedChild1.oid, new AccessDeletedItems(), new ReadAllColumn());
			Assertions.assertNotNull(child1Check);
			Assertions.assertNotNull(child1Check.parentOid);
			Assertions.assertEquals(this.insertedParent1.oid, child1Check.parentOid);

		}

		@Order(3)
		@Test
		public void updateParentWillUpdateChilds() throws Exception {
			final TypeManyToOneDocOIDParentSetNull parentToUpdate = new TypeManyToOneDocOIDParentSetNull("parent 1",
					List.of(this.insertedChild2.oid));
			// insert element
			final long count = ConfigureDb.da.updateById(parentToUpdate, this.insertedParent1.oid);
			Assertions.assertEquals(1, count);
			this.insertedParent1 = ConfigureDb.da.getById(TypeManyToOneDocOIDParentSetNull.class,
					this.insertedParent1.oid);
			Assertions.assertNotNull(this.insertedParent1);
			Assertions.assertNotNull(this.insertedParent1.oid);
			Assertions.assertEquals(this.insertedParent1.data, parentToUpdate.data);
			Assertions.assertNotNull(this.insertedParent1.childOids);
			Assertions.assertEquals(1, this.insertedParent1.childOids.size());
			Assertions.assertEquals(this.insertedChild2.oid, this.insertedParent1.childOids.get(0));
			// check if child is update
			final TypeManyToOneDocOIDChildTTT child1Check = ConfigureDb.da.getById(TypeManyToOneDocOIDChildTTT.class,
					this.insertedChild1.oid, new AccessDeletedItems(), new ReadAllColumn());
			Assertions.assertNotNull(child1Check);
			Assertions.assertNull(child1Check.parentOid);

			// check if child is update
			final TypeManyToOneDocOIDChildTTT child2Check = ConfigureDb.da.getById(TypeManyToOneDocOIDChildTTT.class,
					this.insertedChild2.oid, new AccessDeletedItems(), new ReadAllColumn());
			Assertions.assertNotNull(child2Check);
			Assertions.assertNotNull(child2Check.parentOid);
			Assertions.assertEquals(this.insertedParent1.oid, child2Check.parentOid);
		}

		@Order(4)
		@Test
		public void deleteParentWillUpdateChilds() throws Exception {

			final long count = ConfigureDb.da.deleteById(TypeManyToOneDocOIDParentSetNull.class,
					this.insertedParent1.oid);
			Assertions.assertEquals(1, count);

			// check if child is update
			final TypeManyToOneDocOIDChildTTT child1Check = ConfigureDb.da.getById(TypeManyToOneDocOIDChildTTT.class,
					this.insertedChild1.oid, new AccessDeletedItems(), new ReadAllColumn());
			Assertions.assertNotNull(child1Check);
			Assertions.assertNull(child1Check.parentOid);

			// check if child is update
			final TypeManyToOneDocOIDChildTTT child2Check = ConfigureDb.da.getById(TypeManyToOneDocOIDChildTTT.class,
					this.insertedChild2.oid, new AccessDeletedItems(), new ReadAllColumn());
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
			this.insertedChild1 = ConfigureDb.da.insert(childToInsert);
			Assertions.assertNotNull(this.insertedChild1);
			childToInsert = new TypeManyToOneDocOIDChildTTT("child 2", null);
			// insert element
			this.insertedChild2 = ConfigureDb.da.insert(childToInsert);
			Assertions.assertNotNull(this.insertedChild2);
		}

		@Order(2)
		@Test
		public void insertParentWillUpdateChilds() throws Exception {
			final TypeManyToOneDocOIDParentDelete parentToInsert = new TypeManyToOneDocOIDParentDelete("parent 1",
					List.of(this.insertedChild1.oid));
			// insert element
			this.insertedParent1 = ConfigureDb.da.insert(parentToInsert);
			Assertions.assertNotNull(this.insertedParent1);
			Assertions.assertNotNull(this.insertedParent1.oid);
			Assertions.assertEquals(parentToInsert.data, this.insertedParent1.data);
			Assertions.assertNotNull(this.insertedParent1.childOids);
			Assertions.assertEquals(1, this.insertedParent1.childOids.size());
			Assertions.assertEquals(this.insertedChild1.oid, this.insertedParent1.childOids.get(0));

			// check if child is update
			final TypeManyToOneDocOIDChildTTT child1Check = ConfigureDb.da.getById(TypeManyToOneDocOIDChildTTT.class,
					this.insertedChild1.oid, new AccessDeletedItems(), new ReadAllColumn());
			Assertions.assertNotNull(child1Check);
			Assertions.assertNotNull(child1Check.parentOid);
			Assertions.assertEquals(this.insertedParent1.oid, child1Check.parentOid);

		}

		@Order(3)
		@Test
		public void updateParentWillDeleteChilds() throws Exception {
			final TypeManyToOneDocOIDParentDelete parentToUpdate = new TypeManyToOneDocOIDParentDelete("parent 1",
					List.of(this.insertedChild2.oid));
			// insert element
			final long count = ConfigureDb.da.updateById(parentToUpdate, this.insertedParent1.oid);
			Assertions.assertEquals(1, count);
			this.insertedParent1 = ConfigureDb.da.getById(TypeManyToOneDocOIDParentDelete.class,
					this.insertedParent1.oid);
			Assertions.assertNotNull(this.insertedParent1);
			Assertions.assertNotNull(this.insertedParent1.oid);
			Assertions.assertEquals(this.insertedParent1.data, parentToUpdate.data);
			Assertions.assertNotNull(this.insertedParent1.childOids);
			Assertions.assertEquals(1, this.insertedParent1.childOids.size());
			Assertions.assertEquals(this.insertedChild2.oid, this.insertedParent1.childOids.get(0));
			// check if child is removed
			final TypeManyToOneDocOIDChildTTT child1Check = ConfigureDb.da.getById(TypeManyToOneDocOIDChildTTT.class,
					this.insertedChild1.oid, new AccessDeletedItems(), new ReadAllColumn());
			Assertions.assertNull(child1Check);

			// check if child is update
			final TypeManyToOneDocOIDChildTTT child2Check = ConfigureDb.da.getById(TypeManyToOneDocOIDChildTTT.class,
					this.insertedChild2.oid, new AccessDeletedItems(), new ReadAllColumn());
			Assertions.assertNotNull(child2Check);
			Assertions.assertNotNull(child2Check.parentOid);
			Assertions.assertEquals(this.insertedParent1.oid, child2Check.parentOid);
		}

		@Order(4)
		@Test
		public void deleteParentWillNotUpdateChilds() throws Exception {
			final long count = ConfigureDb.da.deleteById(TypeManyToOneDocOIDParentDelete.class,
					this.insertedParent1.oid);
			Assertions.assertEquals(1, count);
			// check if child is removed
			final TypeManyToOneDocOIDChildTTT child2Check = ConfigureDb.da.getById(TypeManyToOneDocOIDChildTTT.class,
					this.insertedChild1.oid, new AccessDeletedItems(), new ReadAllColumn());
			Assertions.assertNull(child2Check);
		}

	}

}
