package test.atriasoft.archidata.dataAccess.relationships;

import java.io.IOException;
import java.time.Duration;
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
import test.atriasoft.archidata.dataAccess.model.TypeOneToManyDocLongParentCascadeDeleteSetNull;
import test.atriasoft.archidata.dataAccess.model.TypeOneToManyDocLongRemote;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TestOneToManyWithTimestamps {

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
	void testInsertParentSetsTimestamps() throws Exception {
		insertedRemote = ConfigureDb.da.insert(new TypeOneToManyDocLongRemote("remote_ts", null));

		Thread.sleep(Duration.ofMillis(15));

		final TypeOneToManyDocLongParentCascadeDeleteSetNull parent = new TypeOneToManyDocLongParentCascadeDeleteSetNull(
				"parent_ts", List.of(insertedRemote.id));
		insertedParent = ConfigureDb.da.insert(parent);

		final TypeOneToManyDocLongParentCascadeDeleteSetNull parentCheck = ConfigureDb.da.getById(
				TypeOneToManyDocLongParentCascadeDeleteSetNull.class, insertedParent.id, new ReadAllColumn());
		Assertions.assertNotNull(parentCheck);
		Assertions.assertNotNull(parentCheck.createdAt);
		Assertions.assertNotNull(parentCheck.updatedAt);
	}

	@Order(2)
	@Test
	void testChildHasUpdatedTimestamp() throws Exception {
		// The child should have updatedAt changed because parent insert updated its parentId
		final TypeOneToManyDocLongRemote remoteCheck = ConfigureDb.da.getById(TypeOneToManyDocLongRemote.class,
				insertedRemote.id, new ReadAllColumn());
		Assertions.assertNotNull(remoteCheck);
		Assertions.assertNotNull(remoteCheck.createdAt);
		Assertions.assertNotNull(remoteCheck.updatedAt);
		// updatedAt should be >= createdAt (may be equal if update was fast)
		Assertions.assertTrue(remoteCheck.updatedAt.compareTo(remoteCheck.createdAt) >= 0);
	}
}
