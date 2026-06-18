package org.atriasoft.archidata.crypto;

/**
 * Enumeration of the supported field-encryption schemes.
 *
 * <p>Each scheme has a stable 1-byte identifier that is embedded in the
 * {@link EncryptedEnvelope} so the right {@link CipherProvider} can be selected
 * at decryption time regardless of how the field annotation was configured.</p>
 */
public enum EncryptionScheme {
	/** Symmetric AES-256-GCM (one key encrypts and decrypts). */
	AES_GCM((byte) 1),
	/** Asymmetric X25519 sealed box (public key encrypts, private key decrypts). */
	X25519((byte) 2),
	/** Asymmetric RSA-OAEP (public key encrypts, private key decrypts). */
	RSA((byte) 3);

	private final byte id;

	EncryptionScheme(final byte id) {
		this.id = id;
	}

	/**
	 * Returns the stable on-disk identifier of this scheme.
	 *
	 * @return the 1-byte scheme identifier.
	 */
	public byte getId() {
		return this.id;
	}

	/**
	 * Resolves a scheme from its stable identifier.
	 *
	 * @param id the scheme identifier read from an {@link EncryptedEnvelope}.
	 * @return the matching scheme.
	 * @throws IllegalArgumentException if no scheme matches the identifier.
	 */
	public static EncryptionScheme fromId(final byte id) {
		for (final EncryptionScheme scheme : values()) {
			if (scheme.id == id) {
				return scheme;
			}
		}
		throw new IllegalArgumentException("Unknown encryption scheme id: " + id);
	}
}
