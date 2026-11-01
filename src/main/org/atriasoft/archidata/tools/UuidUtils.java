package org.atriasoft.archidata.tools;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.atriasoft.archidata.exception.DataAccessException;

/**
 * Utility class for UUID generation and conversion.
 *
 * <p>Provides conversions between {@link UUID}, {@code byte[]}, and {@link BigInteger},
 * as well as a time-based unique UUID generator.</p>
 *
 * <p>The generated UUIDs are unique but <b>not sortable</b>: see {@link #nextUUID()}.</p>
 */
public class UuidUtils {
	private UuidUtils() {
		// Utility class
	}

	/**
	 * Converts a {@link BigInteger} to a {@link UUID}.
	 *
	 * <p>The BigInteger is interpreted as an unsigned 128-bit value where the most
	 * significant 64 bits map to {@link UUID#getMostSignificantBits()} and the least
	 * significant 64 bits map to {@link UUID#getLeastSignificantBits()}.</p>
	 * @param bigInteger The BigInteger to convert.
	 * @return The corresponding UUID.
	 */
	public static UUID asUuid(final BigInteger bigInteger) {
		final long mostSignificantBits = bigInteger.shiftRight(64).longValue();
		final long leastSignificantBits = bigInteger.longValue();
		return new UUID(mostSignificantBits, leastSignificantBits);
	}

	/**
	 * Converts a 16-byte array to a {@link UUID}.
	 * @param bytes The byte array (must be exactly 16 bytes).
	 * @return The corresponding UUID.
	 * @throws DataAccessException If the byte array is not 16 bytes long.
	 */
	public static UUID asUuid(final byte[] bytes) throws DataAccessException {
		if (bytes.length != 16) {
			throw new DataAccessException("Try to convert wrong size of UUID: " + bytes.length + " expected 16.");
		}
		final ByteBuffer bb = ByteBuffer.wrap(bytes);
		final long firstLong = bb.getLong();
		final long secondLong = bb.getLong();
		return new UUID(firstLong, secondLong);
	}

	/**
	 * Converts a {@link UUID} to a 16-byte array.
	 * @param uuid The UUID to convert.
	 * @return A 16-byte array representing the UUID.
	 */
	public static byte[] asBytes(final UUID uuid) {
		final ByteBuffer bb = ByteBuffer.allocate(16);
		bb.putLong(uuid.getMostSignificantBits());
		bb.putLong(uuid.getLeastSignificantBits());
		return bb.array();
	}

	private static final class Generator {
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

	/**
	 * Returns the next UUID from the internal generator.
	 *
	 * <p>The most significant 64 bits hold a counter of milliseconds elapsed since the generator
	 * creation (strictly increasing, one step per call even within the same millisecond), stored
	 * <b>byte-reversed</b>. The least significant 64 bits hold the server id ({@code UUID_SERVER_ID},
	 * 16 bits) and the number of seconds between 2024-03-19 and the generator creation.</p>
	 *
	 * <p>The UUIDs are unique within the same JVM instance. Because of the byte reversal, neither
	 * {@link UUID#compareTo(UUID)} nor the textual or binary order of the UUIDs follows the creation
	 * order: do not sort on them to get a chronological order. The format is kept as is because
	 * UUIDs are already stored in databases.</p>
	 * @return A new unique UUID.
	 */
	public static UUID nextUUID() {
		return generator.next();
	}
}
