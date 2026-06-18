package org.atriasoft.archidata.crypto;

import java.util.Base64;

/**
 * Common framing for an encrypted field value.
 *
 * <p>The binary layout is {@code [ 'A', 'E', version, schemeId, payload... ]} and the whole frame
 * is Base64-encoded so it can be stored as-is in place of a {@code String} field value.</p>
 *
 * <p>The scheme identifier embedded in the frame allows the correct {@link CipherProvider} to be
 * selected at decryption time, independently of the field annotation.</p>
 */
public final class EncryptedEnvelope {

	private static final byte MAGIC_0 = (byte) 'A';
	private static final byte MAGIC_1 = (byte) 'E';
	private static final byte VERSION = (byte) 1;
	private static final int HEADER_LENGTH = 4;

	private EncryptedEnvelope() {
		// Utility class.
	}

	/**
	 * Parsed view of an encrypted envelope.
	 *
	 * @param scheme the encryption scheme that produced the payload.
	 * @param payload the scheme-specific encrypted body.
	 */
	public record Parsed(
			EncryptionScheme scheme,
			byte[] payload) {}

	/**
	 * Builds the Base64-encoded envelope for an encrypted payload.
	 *
	 * @param scheme the scheme that produced the payload.
	 * @param payload the scheme-specific encrypted body.
	 * @return the Base64-encoded envelope.
	 */
	public static String encode(final EncryptionScheme scheme, final byte[] payload) {
		final byte[] frame = new byte[HEADER_LENGTH + payload.length];
		frame[0] = MAGIC_0;
		frame[1] = MAGIC_1;
		frame[2] = VERSION;
		frame[3] = scheme.getId();
		System.arraycopy(payload, 0, frame, HEADER_LENGTH, payload.length);
		return Base64.getEncoder().encodeToString(frame);
	}

	/**
	 * Indicates whether a stored value looks like an encrypted envelope.
	 *
	 * @param value the stored value (may be {@code null}).
	 * @return {@code true} if the value is a well-formed envelope.
	 */
	public static boolean isEnvelope(final String value) {
		if (value == null) {
			return false;
		}
		try {
			final byte[] frame = Base64.getDecoder().decode(value);
			return frame.length >= HEADER_LENGTH && frame[0] == MAGIC_0 && frame[1] == MAGIC_1 && frame[2] == VERSION;
		} catch (final IllegalArgumentException e) {
			return false;
		}
	}

	/**
	 * Parses a Base64-encoded envelope.
	 *
	 * @param value the stored envelope value.
	 * @return the parsed scheme and payload.
	 * @throws IllegalArgumentException if the value is not a valid envelope.
	 */
	public static Parsed decode(final String value) {
		final byte[] frame = Base64.getDecoder().decode(value);
		if (frame.length < HEADER_LENGTH || frame[0] != MAGIC_0 || frame[1] != MAGIC_1) {
			throw new IllegalArgumentException("Invalid encrypted envelope: bad magic header");
		}
		if (frame[2] != VERSION) {
			throw new IllegalArgumentException("Unsupported encrypted envelope version: " + frame[2]);
		}
		final EncryptionScheme scheme = EncryptionScheme.fromId(frame[3]);
		final byte[] payload = new byte[frame.length - HEADER_LENGTH];
		System.arraycopy(frame, HEADER_LENGTH, payload, 0, payload.length);
		return new Parsed(scheme, payload);
	}
}
