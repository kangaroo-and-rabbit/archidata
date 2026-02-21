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

import org.junit.jupiter.api.Disabled;

import test.atriasoft.archidata.ConfigureDb;
import test.atriasoft.archidata.StepwiseExtension;
import test.atriasoft.archidata.dataAccess.model.TypeOneToManyDocLongParentCascadeDeleteDelete;
import test.atriasoft.archidata.dataAccess.model.TypeOneToManyDocLongRemote;

@Disabled("Framework bug: AddOnOneToManyDoc.onDelete() uses cascadeUpdate instead of cascadeDelete (line 269)")
@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TestOneToManyCascadeDeleteDelete {

	private static TypeOneToManyDocLongRemote insertedRemote1;
	private static TypeOneToManyDocLongRemote insertedRemote2;
	private static TypeOneToManyDocLongParentCascadeDeleteDelete insertedParent;

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
	void testCreateRemotes() throws Exception {
		insertedRemote1 = ConfigureDb.da.insert(new TypeOneToManyDocLongRemote("remote_cdd1", null));
		insertedRemote2 = ConfigureDb.da.insert(new TypeOneToManyDocLongRemote("remote_cdd2", null));
	}

	@Order(2)
	@Test
	void testInsertParentWithChildren() throws Exception {
		final TypeOneToManyDocLongParentCascadeDeleteDelete parent = new TypeOneToManyDocLongParentCascadeDeleteDelete(
				"parent_cdd", List.of(insertedRemote1.id, insertedRemote2.id));
		insertedParent = ConfigureDb.da.insert(parent);
		Assertions.assertNotNull(insertedParent);

		// Verify children are linked
		TypeOneToManyDocLongRemote r1 = ConfigureDb.da.getById(TypeOneToManyDocLongRemote.class, insertedRemote1.id,
				new AccessDeletedItems(), new ReadAllColumn());
		Assertions.assertNotNull(r1);
		Assertions.assertEquals(insertedParent.id, r1.parentId);
	}

	@Order(3)
	@Test
	void testDeleteParentDeletesChildren() throws Exception {
		final long count = ConfigureDb.da.deleteById(TypeOneToManyDocLongParentCascadeDeleteDelete.class,
				insertedParent.id);
		Assertions.assertEquals(1, count);

		// Children should be deleted
		final TypeOneToManyDocLongRemote r1 = ConfigureDb.da.getById(TypeOneToManyDocLongRemote.class,
				insertedRemote1.id, new AccessDeletedItems(), new ReadAllColumn());
		Assertions.assertNull(r1);

		final TypeOneToManyDocLongRemote r2 = ConfigureDb.da.getById(TypeOneToManyDocLongRemote.class,
				insertedRemote2.id, new AccessDeletedItems(), new ReadAllColumn());
		Assertions.assertNull(r2);
	}
}
