package org.kar.archidata.tools;

import java.nio.ByteBuffer;
import java.util.UUID;

public class UuidUtils {

	public static UUID asUuid(final byte[] bytes) {
		final ByteBuffer bb = ByteBuffer.wrap(bytes);
		final long firstLong = bb.getLong();
		final long secondLong = bb.getLong();
		return new UUID(firstLong, secondLong);
	}

	public static byte[] asBytes(final UUID uuid) {
		final ByteBuffer bb = ByteBuffer.allocate(16);
		bb.putLong(uuid.getMostSignificantBits());
		bb.putLong(uuid.getLeastSignificantBits());
		return bb.array();
	}
}
