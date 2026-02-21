package test.atriasoft.archidata.dataAccess.relationships;

import java.io.IOException;
import java.util.List;

import org.atriasoft.archidata.dataAccess.options.AccessDeletedItems;
import org.atriasoft.archidata.dataAccess.options.ReadAllColumn;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import test.atriasoft.archidata.ConfigureDb;
import test.atriasoft.archidata.StepwiseExtension;
import test.atriasoft.archidata.dataAccess.model.TypeManyToOneDocLongChildFFT;
import test.atriasoft.archidata.dataAccess.model.TypeManyToOneDocLongParentIgnore;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TestManyToOneNoUpdate {

	private static TypeManyToOneDocLongParentIgnore insertedParent1;
	private static TypeManyToOneDocLongParentIgnore insertedParent2;
	private static TypeManyToOneDocLongChildFFT insertedChild;

	@BeforeAll
	static void setup() throws Exception {
		ConfigureDb.configure();
	}

	@AfterAll
	static void cleanup() throws IOException {
		ConfigureDb.clear();
	}

	@Order(1)
	@Test
	void testCreateParents() throws Exception {
		TypeManyToOneDocLongParentIgnore parent = new TypeManyToOneDocLongParentIgnore();
		parent.data = "parent1_noupdate";
		insertedParent1 = ConfigureDb.da.insert(parent);
		Assertions.assertNotNull(insertedParent1);

		parent = new TypeManyToOneDocLongParentIgnore();
		parent.data = "parent2_noupdate";
		insertedParent2 = ConfigureDb.da.insert(parent);
		Assertions.assertNotNull(insertedParent2);
	}

	@Order(2)
	@Test
	void testInsertChildDoesNotAddLink() throws Exception {
		// addLinkWhenCreate=false
		final TypeManyToOneDocLongChildFFT child = new TypeManyToOneDocLongChildFFT("child_noupdate", insertedParent1.id);
		insertedChild = ConfigureDb.da.insert(child);
		Assertions.assertNotNull(insertedChild);

		final TypeManyToOneDocLongParentIgnore parentCheck = ConfigureDb.da.getById(
				TypeManyToOneDocLongParentIgnore.class, insertedParent1.id, new AccessDeletedItems(), new ReadAllColumn());
		Assertions.assertNotNull(parentCheck);
		Assertions.assertNull(parentCheck.childIds);
	}

	@Order(3)
	@Test
	void testManuallyAddLinkToParent() throws Exception {
		// Manually add link so we can test update behavior
		final TypeManyToOneDocLongParentIgnore parentUpdate = new TypeManyToOneDocLongParentIgnore("parent1_noupdate", List.of(insertedChild.id));
		ConfigureDb.da.updateById(parentUpdate, insertedParent1.id);

		// Verify link is added
		final TypeManyToOneDocLongParentIgnore parentCheck = ConfigureDb.da.getById(
				TypeManyToOneDocLongParentIgnore.class, insertedParent1.id, new AccessDeletedItems(), new ReadAllColumn());
		Assertions.assertNotNull(parentCheck);
		Assertions.assertNotNull(parentCheck.childIds);
		Assertions.assertEquals(1, parentCheck.childIds.size());
	}

	@Order(4)
	@Test
	void testUpdateChildMovesLink() throws Exception {
		// updateLinkWhenUpdate=true — changing parent should update both parents
		final TypeManyToOneDocLongChildFFT childUpdate = new TypeManyToOneDocLongChildFFT("child_noupdate", insertedParent2.id);
		final long count = ConfigureDb.da.updateById(childUpdate, insertedChild.id);
		Assertions.assertEquals(1, count);

		// Old parent should lose link
		TypeManyToOneDocLongParentIgnore parent1Check = ConfigureDb.da.getById(
				TypeManyToOneDocLongParentIgnore.class, insertedParent1.id, new AccessDeletedItems(), new ReadAllColumn());
		Assertions.assertNotNull(parent1Check);
		Assertions.assertNull(parent1Check.childIds);

		// New parent should get link
		TypeManyToOneDocLongParentIgnore parent2Check = ConfigureDb.da.getById(
				TypeManyToOneDocLongParentIgnore.class, insertedParent2.id, new AccessDeletedItems(), new ReadAllColumn());
		Assertions.assertNotNull(parent2Check);
		Assertions.assertNotNull(parent2Check.childIds);
		Assertions.assertEquals(1, parent2Check.childIds.size());
		Assertions.assertEquals(insertedChild.id, parent2Check.childIds.get(0));
	}
}
