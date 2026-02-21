package test.atriasoft.archidata.dataAccess.relationships;

import java.io.IOException;
import java.time.Duration;
import java.util.Date;

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
class TestManyToOneWithTimestamps {

	private static TypeManyToOneDocLongParentIgnore insertedParent;
	private static TypeManyToOneDocLongChildTTT insertedChild;
	private static Date parentUpdatedAtAfterInsert;

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
		parent.data = "parent_timestamps";
		insertedParent = ConfigureDb.da.insert(parent);
		Assertions.assertNotNull(insertedParent);
	}

	@Order(2)
	@Test
	void testInsertChildUpdatesParentTimestamp() throws Exception {
		Thread.sleep(Duration.ofMillis(15));

		final TypeManyToOneDocLongChildTTT child = new TypeManyToOneDocLongChildTTT("child_ts", insertedParent.id);
		insertedChild = ConfigureDb.da.insert(child);
		Assertions.assertNotNull(insertedChild);

		// Check parent's updatedAt changed
		final TypeManyToOneDocLongParentIgnore parentCheck = ConfigureDb.da.getById(
				TypeManyToOneDocLongParentIgnore.class, insertedParent.id, new AccessDeletedItems(), new ReadAllColumn());
		Assertions.assertNotNull(parentCheck);
		Assertions.assertNotNull(parentCheck.createdAt);
		Assertions.assertNotNull(parentCheck.updatedAt);
		Assertions.assertTrue(parentCheck.updatedAt.after(parentCheck.createdAt));
		parentUpdatedAtAfterInsert = parentCheck.updatedAt;
	}

	@Order(3)
	@Test
	void testDeleteChildUpdatesParentTimestamp() throws Exception {
		Thread.sleep(Duration.ofMillis(15));

		final long count = ConfigureDb.da.deleteById(TypeManyToOneDocLongChildTTT.class, insertedChild.id);
		Assertions.assertEquals(1, count);

		final TypeManyToOneDocLongParentIgnore parentCheck = ConfigureDb.da.getById(
				TypeManyToOneDocLongParentIgnore.class, insertedParent.id, new AccessDeletedItems(), new ReadAllColumn());
		Assertions.assertNotNull(parentCheck);
		Assertions.assertNotNull(parentCheck.updatedAt);
		Assertions.assertTrue(parentCheck.updatedAt.after(parentUpdatedAtAfterInsert));
	}
}
