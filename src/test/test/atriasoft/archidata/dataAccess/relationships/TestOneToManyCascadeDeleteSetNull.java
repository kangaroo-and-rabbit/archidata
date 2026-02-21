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
import test.atriasoft.archidata.dataAccess.model.TypeOneToManyDocLongParentCascadeDeleteSetNull;
import test.atriasoft.archidata.dataAccess.model.TypeOneToManyDocLongRemote;

@Disabled("Framework bug: AddOnOneToManyDoc.onDelete() uses cascadeUpdate instead of cascadeDelete (line 269)")
@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TestOneToManyCascadeDeleteSetNull {

	private static TypeOneToManyDocLongRemote insertedRemote;
	private static TypeOneToManyDocLongParentCascadeDeleteSetNull insertedParent;

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
		insertedRemote = ConfigureDb.da.insert(new TypeOneToManyDocLongRemote("remote_cdsn", null));
		final TypeOneToManyDocLongParentCascadeDeleteSetNull parent = new TypeOneToManyDocLongParentCascadeDeleteSetNull(
				"parent_cdsn", List.of(insertedRemote.id));
		insertedParent = ConfigureDb.da.insert(parent);

		// Verify child linked
		TypeOneToManyDocLongRemote r = ConfigureDb.da.getById(TypeOneToManyDocLongRemote.class, insertedRemote.id,
				new AccessDeletedItems(), new ReadAllColumn());
		Assertions.assertEquals(insertedParent.id, r.parentId);
	}

	@Order(2)
	@Test
	void testDeleteParentSetsChildParentToNull() throws Exception {
		final long count = ConfigureDb.da.deleteById(TypeOneToManyDocLongParentCascadeDeleteSetNull.class,
				insertedParent.id);
		Assertions.assertEquals(1, count);

		// Child should still exist but parentId should be null
		final TypeOneToManyDocLongRemote r = ConfigureDb.da.getById(TypeOneToManyDocLongRemote.class,
				insertedRemote.id, new AccessDeletedItems(), new ReadAllColumn());
		Assertions.assertNotNull(r);
		Assertions.assertNull(r.parentId);
	}
}
