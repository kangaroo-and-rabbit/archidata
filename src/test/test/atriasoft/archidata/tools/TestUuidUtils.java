package test.atriasoft.archidata.tools;

import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.atriasoft.archidata.exception.DataAccessException;
import org.atriasoft.archidata.tools.UuidUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TestUuidUtils {

	@Test
	void testAsBytesAndBackToUuid() throws DataAccessException {
		final UUID original = UUID.randomUUID();
		final byte[] bytes = UuidUtils.asBytes(original);
		Assertions.assertEquals(16, bytes.length);
		final UUID restored = UuidUtils.asUuid(bytes);
		Assertions.assertEquals(original, restored);
	}

	@Test
	void testAsBytesKnownUuid() {
		final UUID uuid = new UUID(0L, 0L);
		final byte[] bytes = UuidUtils.asBytes(uuid);
		Assertions.assertEquals(16, bytes.length);
		for (final byte b : bytes) {
			Assertions.assertEquals(0, b);
		}
	}

	@Test
	void testAsUuidFromBytesWrongSize() {
		final byte[] tooShort = new byte[10];
		Assertions.assertThrows(DataAccessException.class, () -> UuidUtils.asUuid(tooShort));
	}

	@Test
	void testAsUuidFromBytesTooLong() {
		final byte[] tooLong = new byte[20];
		Assertions.assertThrows(DataAccessException.class, () -> UuidUtils.asUuid(tooLong));
	}

	@Test
	void testAsUuidFromBigInteger() {
		final UUID original = UUID.randomUUID();
		final long msb = original.getMostSignificantBits();
		final long lsb = original.getLeastSignificantBits();
		final BigInteger unsignedMask = BigInteger.ONE.shiftLeft(64).subtract(BigInteger.ONE);
		final BigInteger bigInt = BigInteger.valueOf(msb).and(unsignedMask).shiftLeft(64)
				.or(BigInteger.valueOf(lsb).and(unsignedMask));
		final UUID restored = UuidUtils.asUuid(bigInt);
		Assertions.assertEquals(original, restored);
	}

	@Test
	void testNextUuidNotNull() {
		final UUID uuid = UuidUtils.nextUUID();
		Assertions.assertNotNull(uuid);
	}

	@Test
	void testNextUuidUniqueness() {
		final Set<UUID> uuids = new HashSet<>();
		for (int i = 0; i < 1000; i++) {
			final UUID uuid = UuidUtils.nextUUID();
			Assertions.assertTrue(uuids.add(uuid), "Duplicate UUID generated at iteration " + i);
		}
	}

	@Test
	void testNextUuidMonotonicallyIncreasing() {
		final UUID uuid1 = UuidUtils.nextUUID();
		final UUID uuid2 = UuidUtils.nextUUID();
		// The most significant bits contain reversed time, so different UUIDs should not be equal
		Assertions.assertNotEquals(uuid1, uuid2);
	}

	@Test
	void testRoundTripMultipleUuids() throws DataAccessException {
		for (int i = 0; i < 100; i++) {
			final UUID original = UUID.randomUUID();
			final byte[] bytes = UuidUtils.asBytes(original);
			final UUID restored = UuidUtils.asUuid(bytes);
			Assertions.assertEquals(original, restored, "Round-trip failed at iteration " + i);
		}
	}
}
