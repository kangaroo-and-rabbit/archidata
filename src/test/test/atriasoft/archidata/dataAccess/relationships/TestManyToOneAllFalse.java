package test.atriasoft.archidata.dataAccess.relationships;

import java.io.IOException;

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
import test.atriasoft.archidata.dataAccess.model.TypeManyToOneDocLongChildFFF;
import test.atriasoft.archidata.dataAccess.model.TypeManyToOneDocLongParentIgnore;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TestManyToOneAllFalse {

	private static TypeManyToOneDocLongParentIgnore insertedParent1;
	private static TypeManyToOneDocLongParentIgnore insertedParent2;
	private static TypeManyToOneDocLongChildFFF insertedChild;

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
		parent.data = "parent1_fff";
		insertedParent1 = ConfigureDb.da.insert(parent);

		parent = new TypeManyToOneDocLongParentIgnore();
		parent.data = "parent2_fff";
		insertedParent2 = ConfigureDb.da.insert(parent);
	}

	@Order(2)
	@Test
	void testInsertChildNoLink() throws Exception {
		final TypeManyToOneDocLongChildFFF child = new TypeManyToOneDocLongChildFFF("child_fff", insertedParent1.id);
		insertedChild = ConfigureDb.da.insert(child);
		Assertions.assertNotNull(insertedChild);

		final TypeManyToOneDocLongParentIgnore parentCheck = ConfigureDb.da.getById(
				TypeManyToOneDocLongParentIgnore.class, insertedParent1.id, new AccessDeletedItems(), new ReadAllColumn());
		Assertions.assertNotNull(parentCheck);
		Assertions.assertNull(parentCheck.childIds);
	}

	@Order(3)
	@Test
	void testUpdateChildNoLinkChange() throws Exception {
		final TypeManyToOneDocLongChildFFF childUpdate = new TypeManyToOneDocLongChildFFF("child_fff", insertedParent2.id);
		final long count = ConfigureDb.da.updateById(childUpdate, insertedChild.id);
		Assertions.assertEquals(1, count);

		// Neither parent should have links
		TypeManyToOneDocLongParentIgnore parent1Check = ConfigureDb.da.getById(
				TypeManyToOneDocLongParentIgnore.class, insertedParent1.id, new AccessDeletedItems(), new ReadAllColumn());
		Assertions.assertNull(parent1Check.childIds);

		TypeManyToOneDocLongParentIgnore parent2Check = ConfigureDb.da.getById(
				TypeManyToOneDocLongParentIgnore.class, insertedParent2.id, new AccessDeletedItems(), new ReadAllColumn());
		Assertions.assertNull(parent2Check.childIds);
	}

	@Order(4)
	@Test
	void testDeleteChildNoLinkChange() throws Exception {
		final long count = ConfigureDb.da.deleteById(TypeManyToOneDocLongChildFFF.class, insertedChild.id);
		Assertions.assertEquals(1, count);

		// Parent still no links
		TypeManyToOneDocLongParentIgnore parent2Check = ConfigureDb.da.getById(
				TypeManyToOneDocLongParentIgnore.class, insertedParent2.id, new AccessDeletedItems(), new ReadAllColumn());
		Assertions.assertNull(parent2Check.childIds);
	}
}
