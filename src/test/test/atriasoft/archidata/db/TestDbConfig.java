package test.atriasoft.archidata.db;

import java.util.List;

import org.atriasoft.archidata.db.DbConfig;
import org.atriasoft.archidata.exception.DataAccessException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Port handling of {@link DbConfig}: range validation and the deprecated {@code Short} constructor.
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
		Assertions.assertThrows(IllegalArgumentException.class, () -> withPort(0));
		Assertions.assertThrows(IllegalArgumentException.class, () -> withPort(-1));
		Assertions.assertThrows(IllegalArgumentException.class, () -> withPort(65536));
	}

	@SuppressWarnings("deprecation")
	@Test
	void testDeprecatedShortConstructorDelegates() throws DataAccessException {
		final DbConfig config = new DbConfig("localhost", Short.valueOf((short) 27018), "root", "pass", "db", false,
				List.of());
		Assertions.assertEquals(27018, config.getPort());
		Assertions.assertEquals(27017,
				new DbConfig("localhost", (Short) null, "root", "pass", "db", false, List.of()).getPort());
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> new DbConfig("localhost", Short.valueOf((short) -1), "root", "pass", "db", false, List.of()));
	}
}
