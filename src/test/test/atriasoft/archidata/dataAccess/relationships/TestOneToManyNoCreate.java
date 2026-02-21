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
import test.atriasoft.archidata.dataAccess.model.TypeOneToManyDocLongParentNoCreate;
import test.atriasoft.archidata.dataAccess.model.TypeOneToManyDocLongRemote;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TestOneToManyNoCreate {

	private static TypeOneToManyDocLongRemote insertedRemote;
	private static TypeOneToManyDocLongParentNoCreate insertedParent;

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
	void testCreateRemote() throws Exception {
		insertedRemote = ConfigureDb.da.insert(new TypeOneToManyDocLongRemote("remote_nocreate", null));
		Assertions.assertNotNull(insertedRemote);
	}

	@Order(2)
	@Test
	void testInsertParentDoesNotLinkChildren() throws Exception {
		// addLinkWhenCreate=false
		final TypeOneToManyDocLongParentNoCreate parent = new TypeOneToManyDocLongParentNoCreate("parent_nocreate",
				List.of(insertedRemote.id));
		insertedParent = ConfigureDb.da.insert(parent);
		Assertions.assertNotNull(insertedParent);

		// Check that remote does NOT have parentId set
		final TypeOneToManyDocLongRemote remoteCheck = ConfigureDb.da.getById(TypeOneToManyDocLongRemote.class,
				insertedRemote.id, new AccessDeletedItems(), new ReadAllColumn());
		Assertions.assertNotNull(remoteCheck);
		Assertions.assertNull(remoteCheck.parentId);
	}
}
