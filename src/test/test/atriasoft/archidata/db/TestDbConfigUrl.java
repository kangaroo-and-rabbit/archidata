package test.atriasoft.archidata.db;

import java.util.List;

import org.atriasoft.archidata.db.DbConfig;
import org.atriasoft.archidata.exception.DataAccessException;
import org.atriasoft.archidata.tools.ConfigBaseVariable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.mongodb.ConnectionString;

/** D-13: the connection URL is escaped, authSource is configurable and TLS can be enabled. */
class TestDbConfigUrl {

	@AfterEach
	void restoreConfiguration() {
		ConfigBaseVariable.setDbTls(null);
		ConfigBaseVariable.setDbAuthSource(null);
	}

	@Test
	void testCredentialsAreEscaped() throws DataAccessException {
		final String password = "p@ss:w/o?r#d%2 +x";
		final DbConfig config = new DbConfig("db.local", 27017, "app user", password, "my_db", false, List.of());
		final ConnectionString parsed = new ConnectionString(config.getUrl());
		Assertions.assertEquals("app user", parsed.getUsername());
		Assertions.assertEquals(password, new String(parsed.getPassword()));
		Assertions.assertEquals("my_db", parsed.getDatabase());
		Assertions.assertEquals(List.of("db.local:27017"), parsed.getHosts());
		Assertions.assertNull(parsed.getSslEnabled());
	}

	@Test
	void testRootDefaultsToAdminAuthSource() throws DataAccessException {
		final DbConfig config = new DbConfig("localhost", 27017, "root", "pass", "db", false, List.of());
		Assertions.assertEquals("mongodb://root:pass@localhost:27017/db?authSource=admin", config.getUrl());
		final DbConfig other = new DbConfig("localhost", 27017, "app", "pass", "db", false, List.of());
		Assertions.assertEquals("mongodb://app:pass@localhost:27017/db", other.getUrl());
	}

	@Test
	void testAuthSourceAndTlsFromTheConfiguration() throws DataAccessException {
		ConfigBaseVariable.setDbTls("true");
		ConfigBaseVariable.setDbAuthSource("auth db");
		final DbConfig config = new DbConfig("localhost", 27017, "app", "pass", "db", false, List.of());
		Assertions.assertTrue(config.isTls());
		Assertions.assertEquals("mongodb://app:pass@localhost:27017/db?authSource=auth%20db&tls=true", config.getUrl());
		final ConnectionString parsed = new ConnectionString(config.getUrl());
		Assertions.assertTrue(parsed.getSslEnabled());
		Assertions.assertEquals("auth db", parsed.getCredential().getSource());
		// The configured authSource also wins for root, and a clone keeps both settings.
		final DbConfig root = new DbConfig("localhost", 27017, "root", "pass", "db", false, List.of());
		Assertions.assertTrue(root.getUrl().contains("authSource=auth%20db"));
		Assertions.assertEquals(config.getUrl(), config.clone().getUrl());
		config.setTls(false);
		config.setAuthSource(null);
		Assertions.assertEquals("mongodb://app:pass@localhost:27017/db", config.getUrl());
	}
}
