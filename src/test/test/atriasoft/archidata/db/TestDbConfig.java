package test.atriasoft.archidata.db;

import java.util.ArrayList;
import java.util.List;

import org.atriasoft.archidata.db.DbConfig;
import org.atriasoft.archidata.exception.DataAccessException;
import org.atriasoft.archidata.tools.ConfigBaseVariable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Port handling of {@link DbConfig}: range validation ({@link DataAccessException}) and the deprecated
 * {@code Short} constructor.
 */
class TestDbConfig {

	private static DbConfig withPort(final Integer port) throws DataAccessException {
		return new DbConfig("localhost", port, "root", "pass", "db", false, List.of());
	}

	@Test
	void testNullPortDefaultsTo27017() throws DataAccessException {
		Assertions.assertEquals(27017, withPort((Integer) null).getPort());
	}

	@Test
	void testPortBoundsAreAccepted() throws DataAccessException {
		Assertions.assertEquals(1, withPort(1).getPort());
		Assertions.assertEquals(65535, withPort(65535).getPort());
	}

	@Test
	void testPortOutOfRangeIsRejected() {
		Assertions.assertThrows(DataAccessException.class, () -> withPort(0));
		Assertions.assertThrows(DataAccessException.class, () -> withPort(-1));
		Assertions.assertThrows(DataAccessException.class, () -> withPort(65536));
	}

	@SuppressWarnings("deprecation")
	@Test
	void testDeprecatedShortConstructorDelegates() throws DataAccessException {
		final DbConfig config = new DbConfig("localhost", Short.valueOf((short) 27018), "root", "pass", "db", false,
				List.of());
		Assertions.assertEquals(27018, config.getPort());
		Assertions.assertEquals(27017,
				new DbConfig("localhost", (Short) null, "root", "pass", "db", false, List.of()).getPort());
		Assertions.assertThrows(DataAccessException.class,
				() -> new DbConfig("localhost", Short.valueOf((short) -1), "root", "pass", "db", false, List.of()));
	}

	@Test
	void testInvalidConfiguredPortIsDataAccessException() {
		ConfigBaseVariable.setDbPort("70000");
		final DataAccessException ex = Assertions.assertThrows(DataAccessException.class, DbConfig::new);
		Assertions.assertInstanceOf(IllegalArgumentException.class, ex.getCause());
	}

	@AfterEach
	void restoreConfiguration() {
		ConfigBaseVariable.clearAllValue();
	}

	@Test
	void testClassesAreCopied() throws DataAccessException {
		final List<Class<?>> classes = new ArrayList<>(List.of(String.class));
		final DbConfig config = new DbConfig("localhost", 27017, "root", "pass", "db", false, classes);
		classes.add(Long.class);
		Assertions.assertEquals(List.of(String.class), config.getClasses());
		Assertions.assertThrows(UnsupportedOperationException.class, () -> config.getClasses().add(Long.class));
	}

	@Test
	void testNullClassesStayNull() throws DataAccessException {
		Assertions.assertNull(new DbConfig("localhost", 27017, "root", "pass", "db", false, null).getClasses());
	}

	@Test
	void testCloneIsAnIndependentEqualCopy() throws DataAccessException {
		final DbConfig config = withPort(27018);
		config.setTls(true);
		config.setAuthSource("admin");
		final DbConfig copy = config.clone();
		Assertions.assertNotSame(config, copy);
		Assertions.assertEquals(config, copy);
		Assertions.assertEquals(config.hashCode(), copy.hashCode());
		copy.setDbName("other");
		Assertions.assertEquals("db", config.getDbName());
		Assertions.assertTrue(copy.isTls());
		Assertions.assertEquals("admin", copy.getAuthSource());
	}
}
