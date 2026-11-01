package test.atriasoft.archidata.db;

import java.io.IOException;
import java.util.List;

import org.atriasoft.archidata.db.DbConfig;
import org.atriasoft.archidata.db.DbIoMongo;
import org.atriasoft.archidata.exception.DataAccessException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TestDbIoMongo {

	/** A missing name was logged, then passed to the driver, which refused it after the shared client was created. */
	@Test
	void openWithoutADatabaseNameFailsBeforeCreatingAClient() throws DataAccessException, IOException {
		final DbConfig config = new DbConfig("localhost", 27017, "user", "pass", null, false, List.of());
		final int clientsBefore = DbIoMongo.getSharedClientCount();
		try (DbIoMongo io = new DbIoMongo(config)) {
			final IOException ex = Assertions.assertThrows(IOException.class, io::open);
			Assertions.assertTrue(ex.getMessage().contains("No database name"), ex.getMessage());
		}
		Assertions.assertEquals(clientsBefore, DbIoMongo.getSharedClientCount());
	}

	@Test
	void theConfigurationOfAnOpenConnectionCannotBeChanged() throws DataAccessException, IOException {
		final DbConfig config = new DbConfig("localhost", 27017, "user", "pass", "db", false, List.of());
		try (DbIoMongo io = new DbIoMongo(config)) {
			io.getConfig().setDbName("other");
			Assertions.assertEquals("db", io.getConfig().getDbName());
			Assertions.assertTrue(io.compatible(config));
		}
	}
}
