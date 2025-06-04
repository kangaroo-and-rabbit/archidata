package test.atriasoft.archidata.dataAccess;

import java.io.IOException;
import java.util.List;

import org.atriasoft.archidata.dataAccess.DBAccessSQL;
import org.atriasoft.archidata.dataAccess.DataFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import test.atriasoft.archidata.ConfigureDb;
import test.atriasoft.archidata.StepwiseExtension;
import test.atriasoft.archidata.dataAccess.model.TypeOneToManyLongRemote;
import test.atriasoft.archidata.dataAccess.model.TypeOneToManyLongRoot;
import test.atriasoft.archidata.dataAccess.model.TypeOneToManyLongRootExpand;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@EnabledIfEnvironmentVariable(named = "INCLUDE_MY_SQL_SPECIFIC", matches = "true")
public class TestOneToManyLong {
	final static private Logger LOGGER = LoggerFactory.getLogger(TestOneToManyLong.class);

	@BeforeAll
	public static void configureWebServer() throws Exception {
		ConfigureDb.configure();
	}

	@AfterAll
	public static void removeDataBase() throws IOException {
		ConfigureDb.clear();
	}

	@Order(1)
	@Test
	public void testCreateTable() throws Exception {
		final List<String> sqlCommand = DataFactory.createTable(TypeOneToManyLongRemote.class);
		sqlCommand.addAll(DataFactory.createTable(TypeOneToManyLongRoot.class));
		if (ConfigureDb.da instanceof final DBAccessSQL daSQL) {
			for (final String elem : sqlCommand) {
				LOGGER.debug("request: '{}'", elem);
				daSQL.executeSimpleQuery(elem);
			}
		}
	}

	@Order(2)
	@Test
	public void testParentLong() throws Exception {
		// create parent:

		final TypeOneToManyLongRoot root = new TypeOneToManyLongRoot();
		root.otherData = "plouf";
		final TypeOneToManyLongRoot insertedRoot = ConfigureDb.da.insert(root);
		Assertions.assertEquals(insertedRoot.otherData, root.otherData);
		Assertions.assertNull(insertedRoot.remoteIds);

		final TypeOneToManyLongRoot root2 = new TypeOneToManyLongRoot();
		root2.otherData = "plouf 2";
		final TypeOneToManyLongRoot insertedRoot2 = ConfigureDb.da.insert(root2);
		Assertions.assertEquals(insertedRoot2.otherData, root2.otherData);
		Assertions.assertNull(insertedRoot2.remoteIds);

		// Create Some Remotes

		final TypeOneToManyLongRemote remote10 = new TypeOneToManyLongRemote();
		remote10.data = "remote10";
		remote10.rootId = insertedRoot.id;
		final TypeOneToManyLongRemote insertedRemote10 = ConfigureDb.da.insert(remote10);
		Assertions.assertEquals(insertedRemote10.data, remote10.data);
		Assertions.assertEquals(insertedRemote10.rootId, remote10.rootId);

		final TypeOneToManyLongRemote remote11 = new TypeOneToManyLongRemote();
		remote11.data = "remote11";
		remote11.rootId = insertedRoot.id;
		final TypeOneToManyLongRemote insertedRemote11 = ConfigureDb.da.insert(remote11);
		Assertions.assertEquals(insertedRemote11.data, remote11.data);
		Assertions.assertEquals(insertedRemote11.rootId, remote11.rootId);

		final TypeOneToManyLongRemote remote20 = new TypeOneToManyLongRemote();
		remote20.data = "remote20";
		remote20.rootId = insertedRoot2.id;
		final TypeOneToManyLongRemote insertedRemote20 = ConfigureDb.da.insert(remote20);
		Assertions.assertEquals(insertedRemote20.data, remote20.data);
		Assertions.assertEquals(insertedRemote20.rootId, remote20.rootId);

		// Check remote are inserted

		final TypeOneToManyLongRoot retreiveRoot1 = ConfigureDb.da.get(TypeOneToManyLongRoot.class, insertedRoot.id);
		Assertions.assertEquals(retreiveRoot1.otherData, insertedRoot.otherData);
		Assertions.assertNotNull(retreiveRoot1.remoteIds);
		Assertions.assertEquals(2, retreiveRoot1.remoteIds.size());
		Assertions.assertEquals(insertedRemote10.id, retreiveRoot1.remoteIds.get(0));
		Assertions.assertEquals(insertedRemote11.id, retreiveRoot1.remoteIds.get(1));

		final TypeOneToManyLongRoot retreiveRoot2 = ConfigureDb.da.get(TypeOneToManyLongRoot.class, insertedRoot2.id);
		Assertions.assertEquals(retreiveRoot2.otherData, insertedRoot2.otherData);
		Assertions.assertNotNull(retreiveRoot2.remoteIds);
		Assertions.assertEquals(1, retreiveRoot2.remoteIds.size());
		Assertions.assertEquals(insertedRemote20.id, retreiveRoot2.remoteIds.get(0));

		// Check remote are inserted and expandable
		final TypeOneToManyLongRootExpand retreiveRootExpand1 = ConfigureDb.da.get(TypeOneToManyLongRootExpand.class,
				insertedRoot.id);
		Assertions.assertEquals(retreiveRootExpand1.otherData, insertedRoot.otherData);
		Assertions.assertNotNull(retreiveRootExpand1.remotes);
		Assertions.assertEquals(2, retreiveRootExpand1.remotes.size());
		Assertions.assertEquals(insertedRemote10.id, retreiveRootExpand1.remotes.get(0).id);
		Assertions.assertEquals(insertedRemote10.rootId, retreiveRootExpand1.remotes.get(0).rootId);
		Assertions.assertEquals(insertedRemote10.data, retreiveRootExpand1.remotes.get(0).data);
		Assertions.assertEquals(insertedRemote11.id, retreiveRootExpand1.remotes.get(1).id);
		Assertions.assertEquals(insertedRemote11.rootId, retreiveRootExpand1.remotes.get(1).rootId);
		Assertions.assertEquals(insertedRemote11.data, retreiveRootExpand1.remotes.get(1).data);

		final TypeOneToManyLongRootExpand retreiveRootExpand2 = ConfigureDb.da.get(TypeOneToManyLongRootExpand.class,
				insertedRoot2.id);
		Assertions.assertEquals(retreiveRootExpand2.otherData, insertedRoot2.otherData);
		Assertions.assertNotNull(retreiveRootExpand2.remotes);
		Assertions.assertEquals(1, retreiveRootExpand2.remotes.size());
		Assertions.assertEquals(insertedRemote20.id, retreiveRootExpand2.remotes.get(0).id);
		Assertions.assertEquals(insertedRemote20.rootId, retreiveRootExpand2.remotes.get(0).rootId);
		Assertions.assertEquals(insertedRemote20.data, retreiveRootExpand2.remotes.get(0).data);

	}
}
