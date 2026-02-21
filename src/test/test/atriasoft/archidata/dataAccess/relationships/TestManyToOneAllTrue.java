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
import test.atriasoft.archidata.dataAccess.model.TypeManyToOneDocLongChildTTT;
import test.atriasoft.archidata.dataAccess.model.TypeManyToOneDocLongParentIgnore;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TestManyToOneAllTrue {

	private static TypeManyToOneDocLongParentIgnore insertedParent1;
	private static TypeManyToOneDocLongParentIgnore insertedParent2;
	private static TypeManyToOneDocLongChildTTT insertedChild;

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
		parent.data = "parent1";
		insertedParent1 = ConfigureDb.da.insert(parent);
		Assertions.assertNotNull(insertedParent1);

		parent = new TypeManyToOneDocLongParentIgnore();
		parent.data = "parent2";
		insertedParent2 = ConfigureDb.da.insert(parent);
		Assertions.assertNotNull(insertedParent2);
	}

	@Order(2)
	@Test
	void testInsertChildAddsLinkToParent() throws Exception {
		final TypeManyToOneDocLongChildTTT child = new TypeManyToOneDocLongChildTTT("child1", insertedParent1.id);
		insertedChild = ConfigureDb.da.insert(child);
		Assertions.assertNotNull(insertedChild);
		Assertions.assertEquals(insertedParent1.id, insertedChild.parentId);

		// Parent should now have the child link
		final TypeManyToOneDocLongParentIgnore parentCheck = ConfigureDb.da.getById(
				TypeManyToOneDocLongParentIgnore.class, insertedParent1.id, new AccessDeletedItems(), new ReadAllColumn());
		Assertions.assertNotNull(parentCheck);
		Assertions.assertNotNull(parentCheck.childIds);
		Assertions.assertEquals(1, parentCheck.childIds.size());
		Assertions.assertEquals(insertedChild.id, parentCheck.childIds.get(0));
	}

	@Order(3)
	@Test
	void testUpdateChildMovesLink() throws Exception {
		final TypeManyToOneDocLongChildTTT childUpdate = new TypeManyToOneDocLongChildTTT("child1", insertedParent2.id);
		final long count = ConfigureDb.da.updateById(childUpdate, insertedChild.id);
		Assertions.assertEquals(1, count);

		// Old parent should lose the link
		TypeManyToOneDocLongParentIgnore parent1Check = ConfigureDb.da.getById(
				TypeManyToOneDocLongParentIgnore.class, insertedParent1.id, new AccessDeletedItems(), new ReadAllColumn());
		Assertions.assertNotNull(parent1Check);
		Assertions.assertNull(parent1Check.childIds);

		// New parent should get the link
		TypeManyToOneDocLongParentIgnore parent2Check = ConfigureDb.da.getById(
				TypeManyToOneDocLongParentIgnore.class, insertedParent2.id, new AccessDeletedItems(), new ReadAllColumn());
		Assertions.assertNotNull(parent2Check);
		Assertions.assertNotNull(parent2Check.childIds);
		Assertions.assertEquals(1, parent2Check.childIds.size());
		Assertions.assertEquals(insertedChild.id, parent2Check.childIds.get(0));
	}

	@Order(4)
	@Test
	void testDeleteChildRemovesLink() throws Exception {
		final long count = ConfigureDb.da.deleteById(TypeManyToOneDocLongChildTTT.class, insertedChild.id);
		Assertions.assertEquals(1, count);

		// Parent should lose the link
		final TypeManyToOneDocLongParentIgnore parent2Check = ConfigureDb.da.getById(
				TypeManyToOneDocLongParentIgnore.class, insertedParent2.id, new AccessDeletedItems(), new ReadAllColumn());
		Assertions.assertNotNull(parent2Check);
		Assertions.assertNull(parent2Check.childIds);
	}
}
