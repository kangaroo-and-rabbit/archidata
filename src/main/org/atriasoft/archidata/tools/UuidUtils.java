package org.atriasoft.archidata.tools;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.atriasoft.archidata.exception.DataAccessException;

public class UuidUtils {
	private UuidUtils() {
		// Utility class
	}

	public static UUID asUuid(final BigInteger bigInteger) {
		final long mostSignificantBits = bigInteger.longValue();
		final long leastSignificantBits = bigInteger.shiftRight(64).longValue();
		return new UUID(mostSignificantBits, leastSignificantBits);
	}

	public static UUID asUuid(final byte[] bytes) throws DataAccessException {
		if (bytes.length != 16) {
			throw new DataAccessException("Try to convert wrong size of UUID: " + bytes.length + " expected 16.");
		}
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

	private static class Generator {
		private long base;
		private final long offset;
		private long previous;

		public Generator() {
			this.offset = System.currentTimeMillis();
			// The local method never generate new UUID in the past, then we use the creation function time to prevent 2038 error
			final Instant startingUUID = LocalDate.of(2024, 03, 19).atStartOfDay(ZoneOffset.UTC).toInstant();
			this.base = startingUUID.until(Instant.now(), ChronoUnit.SECONDS);
			final String serveurBaseUUID = System.getenv("UUID_SERVER_ID");
			if (serveurBaseUUID != null) {
				long serverId = Long.parseLong(serveurBaseUUID);
				serverId %= 0xFFFF;
				this.base += (serverId << (64 - 16));
			} else {
				this.base += (1L << (64 - 16));
			}
		}

		public synchronized UUID next() {
			long tmp = System.currentTimeMillis();
			if (this.previous >= tmp) {
				tmp = this.previous + 1;
			}
			this.previous = tmp;
			tmp -= this.offset;
			return new UUID(Long.reverseBytes(tmp), this.base);
		}
	}

	private static Generator generator = new Generator();

	public static UUID nextUUID() {
		return generator.next();
	}
}
