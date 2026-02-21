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
import test.atriasoft.archidata.dataAccess.model.TypeOneToManyDocLongParentIgnore;
import test.atriasoft.archidata.dataAccess.model.TypeOneToManyDocLongRemote;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TestOneToManyBasic {

	private static TypeOneToManyDocLongRemote insertedRemote1;
	private static TypeOneToManyDocLongRemote insertedRemote2;
	private static TypeOneToManyDocLongParentIgnore insertedParent;

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
		insertedRemote1 = ConfigureDb.da.insert(new TypeOneToManyDocLongRemote("remote1", null));
		Assertions.assertNotNull(insertedRemote1);
		insertedRemote2 = ConfigureDb.da.insert(new TypeOneToManyDocLongRemote("remote2", null));
		Assertions.assertNotNull(insertedRemote2);
	}

	@Order(2)
	@Test
	void testInsertParentLinksChildren() throws Exception {
		// addLinkWhenCreate=true — children should get parentId set
		final TypeOneToManyDocLongParentIgnore parent = new TypeOneToManyDocLongParentIgnore("parent1",
				List.of(insertedRemote1.id));
		insertedParent = ConfigureDb.da.insert(parent);
		Assertions.assertNotNull(insertedParent);
		Assertions.assertNotNull(insertedParent.remoteIds);
		Assertions.assertEquals(1, insertedParent.remoteIds.size());

		// Check remote1 has parentId set
		final TypeOneToManyDocLongRemote remoteCheck = ConfigureDb.da.getById(TypeOneToManyDocLongRemote.class,
				insertedRemote1.id, new AccessDeletedItems(), new ReadAllColumn());
		Assertions.assertNotNull(remoteCheck);
		Assertions.assertNotNull(remoteCheck.parentId);
		Assertions.assertEquals(insertedParent.id, remoteCheck.parentId);
	}

	@Order(3)
	@Test
	void testRetrieveParentWithRemotes() throws Exception {
		final TypeOneToManyDocLongParentIgnore parentCheck = ConfigureDb.da.getById(
				TypeOneToManyDocLongParentIgnore.class, insertedParent.id);
		Assertions.assertNotNull(parentCheck);
		Assertions.assertNotNull(parentCheck.remoteIds);
		Assertions.assertEquals(1, parentCheck.remoteIds.size());
		Assertions.assertEquals(insertedRemote1.id, parentCheck.remoteIds.get(0));
	}

	@Order(4)
	@Test
	void testDeleteParentDoesNotAffectChildren() throws Exception {
		// cascadeDelete=IGNORE
		final long count = ConfigureDb.da.deleteById(TypeOneToManyDocLongParentIgnore.class, insertedParent.id);
		Assertions.assertEquals(1, count);

		// Children should still exist
		final TypeOneToManyDocLongRemote remoteCheck = ConfigureDb.da.getById(TypeOneToManyDocLongRemote.class,
				insertedRemote1.id);
		Assertions.assertNotNull(remoteCheck);
	}
}
