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
import test.atriasoft.archidata.dataAccess.model.TypeOneToManyDocLongParentCascadeUpdateDelete;
import test.atriasoft.archidata.dataAccess.model.TypeOneToManyDocLongRemote;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TestOneToManyCascadeUpdateDelete {

	private static TypeOneToManyDocLongRemote insertedRemote1;
	private static TypeOneToManyDocLongRemote insertedRemote2;
	private static TypeOneToManyDocLongParentCascadeUpdateDelete insertedParent;

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
	void testSetup() throws Exception {
		insertedRemote1 = ConfigureDb.da.insert(new TypeOneToManyDocLongRemote("remote_cud1", null));
		insertedRemote2 = ConfigureDb.da.insert(new TypeOneToManyDocLongRemote("remote_cud2", null));

		final TypeOneToManyDocLongParentCascadeUpdateDelete parent = new TypeOneToManyDocLongParentCascadeUpdateDelete(
				"parent_cud", List.of(insertedRemote1.id, insertedRemote2.id));
		insertedParent = ConfigureDb.da.insert(parent);
		Assertions.assertNotNull(insertedParent);
	}

	@Order(2)
	@Test
	void testUpdateParentRemovingChildDeletesIt() throws Exception {
		// Update parent to only keep remote2, removing remote1
		final TypeOneToManyDocLongParentCascadeUpdateDelete parentUpdate = new TypeOneToManyDocLongParentCascadeUpdateDelete(
				"parent_cud", List.of(insertedRemote2.id));
		final long count = ConfigureDb.da.updateById(parentUpdate, insertedParent.id);
		Assertions.assertEquals(1, count);

		// remote1 should be deleted (cascadeUpdate=DELETE)
		final TypeOneToManyDocLongRemote r1 = ConfigureDb.da.getById(TypeOneToManyDocLongRemote.class,
				insertedRemote1.id, new AccessDeletedItems(), new ReadAllColumn());
		Assertions.assertNull(r1);

		// remote2 should still exist
		final TypeOneToManyDocLongRemote r2 = ConfigureDb.da.getById(TypeOneToManyDocLongRemote.class,
				insertedRemote2.id, new AccessDeletedItems(), new ReadAllColumn());
		Assertions.assertNotNull(r2);
		Assertions.assertEquals(insertedParent.id, r2.parentId);
	}
}
