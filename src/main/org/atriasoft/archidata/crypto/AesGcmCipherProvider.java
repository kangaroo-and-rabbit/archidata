package org.atriasoft.archidata.crypto;

/**
 * Symmetric {@link CipherProvider} backed by AES-256-GCM.
 *
 * <p>A single key is used for both encryption and decryption. The produced payload is
 * {@code nonce(12) || ciphertext+tag}.</p>
 */
public final class AesGcmCipherProvider implements CipherProvider {

	private final byte[] key;

	/**
	 * Builds an AES-GCM provider from raw key bytes.
	 *
	 * @param key the AES key (must be 16, 24 or 32 bytes long).
	 */
	public AesGcmCipherProvider(final byte[] key) {
		if (key == null || !CryptoTools.isValidAesKeyLength(key.length)) {
			throw new IllegalArgumentException(
					"AES key must be 16, 24 or 32 bytes long (got " + (key == null ? 0 : key.length) + ")");
		}
		this.key = key.clone();
	}

	@Override
	public EncryptionScheme getScheme() {
		return EncryptionScheme.AES_GCM;
	}

	@Override
	public boolean canEncrypt() {
		return true;
	}

	@Override
	public boolean canDecrypt() {
		return true;
	}

	@Override
	public byte[] encrypt(final byte[] clear) throws Exception {
		return CryptoTools.aesGcmEncrypt(this.key, clear);
	}

	@Override
	public byte[] decrypt(final byte[] payload) throws Exception {
		return CryptoTools.aesGcmDecrypt(this.key, payload);
	}
}
