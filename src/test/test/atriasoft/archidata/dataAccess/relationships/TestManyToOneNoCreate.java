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
class TestManyToOneNoCreate {

	private static TypeManyToOneDocLongParentIgnore insertedParent1;
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
	void testCreateParent() throws Exception {
		final TypeManyToOneDocLongParentIgnore parent = new TypeManyToOneDocLongParentIgnore();
		parent.data = "parent1";
		insertedParent1 = ConfigureDb.da.insert(parent);
		Assertions.assertNotNull(insertedParent1);
	}

	@Order(2)
	@Test
	void testInsertChildDoesNotAddLink() throws Exception {
		final TypeManyToOneDocLongChildFFF child = new TypeManyToOneDocLongChildFFF("child_no_create", insertedParent1.id);
		insertedChild = ConfigureDb.da.insert(child);
		Assertions.assertNotNull(insertedChild);
		Assertions.assertEquals(insertedParent1.id, insertedChild.parentId);

		// Parent should NOT have the child link (addLinkWhenCreate=false)
		final TypeManyToOneDocLongParentIgnore parentCheck = ConfigureDb.da.getById(
				TypeManyToOneDocLongParentIgnore.class, insertedParent1.id, new AccessDeletedItems(), new ReadAllColumn());
		Assertions.assertNotNull(parentCheck);
		Assertions.assertNull(parentCheck.childIds);
	}
}
