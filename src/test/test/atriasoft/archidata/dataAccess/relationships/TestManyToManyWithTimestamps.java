package test.atriasoft.archidata.dataAccess.relationships;

import java.io.IOException;
import java.time.Duration;

import org.atriasoft.archidata.dataAccess.commonTools.ManyToManyTools;
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
import test.atriasoft.archidata.dataAccess.model.TypeManyToManyDocLongRemote;
import test.atriasoft.archidata.dataAccess.model.TypeManyToManyDocLongRoot;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TestManyToManyWithTimestamps {

	private static TypeManyToManyDocLongRoot insertedRoot;
	private static TypeManyToManyDocLongRemote insertedRemote;

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
	void testCreateEntities() throws Exception {
		final TypeManyToManyDocLongRoot root = new TypeManyToManyDocLongRoot();
		root.otherData = "root_ts";
		insertedRoot = ConfigureDb.da.insert(root);

		final TypeManyToManyDocLongRemote remote = new TypeManyToManyDocLongRemote();
		remote.data = "remote_ts";
		insertedRemote = ConfigureDb.da.insert(remote);
	}

	@Order(2)
	@Test
	void testAddLinkUpdatesTimestamps() throws Exception {
		Thread.sleep(Duration.ofMillis(150));

		ManyToManyTools.addLink(ConfigureDb.da, TypeManyToManyDocLongRoot.class,
				insertedRoot.id, "remote", insertedRemote.id);

		final TypeManyToManyDocLongRoot rootCheck = ConfigureDb.da.getById(TypeManyToManyDocLongRoot.class,
				insertedRoot.id, new AccessDeletedItems(), new ReadAllColumn());
		Assertions.assertNotNull(rootCheck);
		Assertions.assertNotNull(rootCheck.createdAt);
		Assertions.assertNotNull(rootCheck.updatedAt);
		Assertions.assertTrue(rootCheck.updatedAt.after(rootCheck.createdAt));
	}

	@Order(3)
	@Test
	void testRemoteLinkUpdatesTimestamps() throws Exception {
		final TypeManyToManyDocLongRemote remoteCheck = ConfigureDb.da.getById(TypeManyToManyDocLongRemote.class,
				insertedRemote.id, new AccessDeletedItems(), new ReadAllColumn());
		Assertions.assertNotNull(remoteCheck);
		Assertions.assertNotNull(remoteCheck.createdAt);
		Assertions.assertNotNull(remoteCheck.updatedAt);
		Assertions.assertTrue(remoteCheck.updatedAt.after(remoteCheck.createdAt));
	}
}
