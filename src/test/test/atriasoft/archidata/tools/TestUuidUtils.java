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
	void nextUuidAreUnique() {
		final Set<UUID> all = new HashSet<>();
		for (int iii = 0; iii < 1000; iii++) {
			Assertions.assertTrue(all.add(UuidUtils.nextUUID()));
		}
	}

	@Test
	void counterIsStrictlyIncreasingOnceBytesAreReversed() {
		long previous = Long.reverseBytes(UuidUtils.nextUUID().getMostSignificantBits());
		for (int iii = 0; iii < 1000; iii++) {
			final long current = Long.reverseBytes(UuidUtils.nextUUID().getMostSignificantBits());
			Assertions.assertTrue(current > previous);
			previous = current;
		}
	}

	@Test
	void uuidNaturalOrderIsNotCreationOrder() {
		// Documented behavior: the most significant half is byte-reversed, so UUIDs are not sortable.
		UUID previous = UuidUtils.nextUUID();
		boolean outOfOrder = false;
		for (int iii = 0; iii < 1000; iii++) {
			final UUID current = UuidUtils.nextUUID();
			if (current.compareTo(previous) < 0) {
				outOfOrder = true;
			}
			previous = current;
		}
		Assertions.assertTrue(outOfOrder);
	}

	@Test
	void bytesRoundTrip() throws Exception {
		final UUID uuid = UuidUtils.nextUUID();
		Assertions.assertEquals(uuid, UuidUtils.asUuid(UuidUtils.asBytes(uuid)));
	}

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
	void testRoundTripMultipleUuids() throws DataAccessException {
		for (int i = 0; i < 100; i++) {
			final UUID original = UUID.randomUUID();
			final byte[] bytes = UuidUtils.asBytes(original);
			final UUID restored = UuidUtils.asUuid(bytes);
			Assertions.assertEquals(original, restored, "Round-trip failed at iteration " + i);
		}
	}
}
