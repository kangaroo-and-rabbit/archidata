package test.atriasoft.archidata.db;

import java.io.IOException;
import java.util.List;

import org.atriasoft.archidata.dataAccess.DBAccessMongo;
import org.atriasoft.archidata.db.DbConfig;
import org.atriasoft.archidata.db.DbIoFactory;
import org.atriasoft.archidata.db.DbIoMongo;
import org.atriasoft.archidata.exception.DataAccessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** The connections {@link DbIoFactory} keeps for a configuration with {@code keepConnected}. */
class TestDbIoFactory {

	@AfterEach
	void forgetTheKeptConnections() throws IOException {
		DbIoFactory.closeAllForceMode();
	}

	/**
	 * The factory holds one reference on a kept connection, and each user takes and releases its own: a reuse must not
	 * add a reference, otherwise the connection can never be closed again.
	 */
	@Test
	void reusingAKeptConnectionDoesNotLeakAReference() throws IOException, DataAccessException {
		final DbConfig config = new DbConfig("localhost", 27017, "user", "pass", "db_factory_keep", true, List.of());
		DbIoMongo kept = null;
		for (int iii = 0; iii < 3; iii++) {
			try (DBAccessMongo access = DBAccessMongo.createInterface(config)) {
				if (kept == null) {
					kept = access.getInterface();
				}
				Assertions.assertSame(kept, access.getInterface(), "the kept connection is reused");
			}
		}
		Assertions.assertNotNull(kept.getDatabase(), "the factory still holds its own reference");
		DbIoFactory.close();
		Assertions.assertNull(kept.getDatabase(), "the connection is closed once the factory released its reference");
	}
}
