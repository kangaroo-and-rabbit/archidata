package test.atriasoft.archidata.tools;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

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
}
