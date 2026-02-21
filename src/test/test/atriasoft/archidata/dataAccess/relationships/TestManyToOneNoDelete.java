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
import test.atriasoft.archidata.dataAccess.model.TypeManyToOneDocLongChildTFF;
import test.atriasoft.archidata.dataAccess.model.TypeManyToOneDocLongParentIgnore;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TestManyToOneNoDelete {

	private static TypeManyToOneDocLongParentIgnore insertedParent;
	private static TypeManyToOneDocLongChildTFF insertedChild;

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
		parent.data = "parent_nodelete";
		insertedParent = ConfigureDb.da.insert(parent);
		Assertions.assertNotNull(insertedParent);
	}

	@Order(2)
	@Test
	void testInsertChildAddsLink() throws Exception {
		final TypeManyToOneDocLongChildTFF child = new TypeManyToOneDocLongChildTFF("child_nodelete", insertedParent.id);
		insertedChild = ConfigureDb.da.insert(child);
		Assertions.assertNotNull(insertedChild);

		// addLinkWhenCreate=true, so parent should have the link
		final TypeManyToOneDocLongParentIgnore parentCheck = ConfigureDb.da.getById(
				TypeManyToOneDocLongParentIgnore.class, insertedParent.id, new AccessDeletedItems(), new ReadAllColumn());
		Assertions.assertNotNull(parentCheck);
		Assertions.assertNotNull(parentCheck.childIds);
		Assertions.assertEquals(1, parentCheck.childIds.size());
		Assertions.assertEquals(insertedChild.id, parentCheck.childIds.get(0));
	}

	@Order(3)
	@Test
	void testDeleteChildKeepsLink() throws Exception {
		final long count = ConfigureDb.da.deleteById(TypeManyToOneDocLongChildTFF.class, insertedChild.id);
		Assertions.assertEquals(1, count);

		// removeLinkWhenDelete=false, so parent should STILL have the link
		final TypeManyToOneDocLongParentIgnore parentCheck = ConfigureDb.da.getById(
				TypeManyToOneDocLongParentIgnore.class, insertedParent.id, new AccessDeletedItems(), new ReadAllColumn());
		Assertions.assertNotNull(parentCheck);
		Assertions.assertNotNull(parentCheck.childIds);
		Assertions.assertEquals(1, parentCheck.childIds.size());
		Assertions.assertEquals(insertedChild.id, parentCheck.childIds.get(0));
	}
}
