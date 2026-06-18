package org.atriasoft.archidata.crypto;

/**
 * A single encryption key (or key pair) able to encrypt and/or decrypt opaque byte payloads.
 *
 * <p>Implementations are immutable and thread-safe after construction. A provider may be able
 * to only encrypt (e.g. an asymmetric provider holding just the public key) or only decrypt.</p>
 *
 * <p>The {@code payload} exchanged with {@link #encrypt(byte[])} / {@link #decrypt(byte[])} is the
 * scheme-specific body only (nonce, ephemeral key, ciphertext...). The scheme identifier and the
 * common framing are handled separately by {@link EncryptedEnvelope}.</p>
 */
public interface CipherProvider {

	/**
	 * Returns the scheme implemented by this provider.
	 *
	 * @return the encryption scheme.
	 */
	EncryptionScheme getScheme();

	/**
	 * Indicates whether this provider holds the material required to encrypt.
	 *
	 * @return {@code true} if {@link #encrypt(byte[])} can be called.
	 */
	boolean canEncrypt();

	/**
	 * Indicates whether this provider holds the material required to decrypt.
	 *
	 * @return {@code true} if {@link #decrypt(byte[])} can be called.
	 */
	boolean canDecrypt();

	/**
	 * Encrypts a clear-text payload.
	 *
	 * @param clear the clear-text bytes.
	 * @return the scheme-specific encrypted payload (without the envelope framing).
	 * @throws Exception if encryption fails or the provider cannot encrypt.
	 */
	byte[] encrypt(byte[] clear) throws Exception;

	/**
	 * Decrypts a scheme-specific payload previously produced by {@link #encrypt(byte[])}.
	 *
	 * @param payload the encrypted payload (without the envelope framing).
	 * @return the clear-text bytes.
	 * @throws Exception if decryption fails or the provider cannot decrypt.
	 */
	byte[] decrypt(byte[] payload) throws Exception;
}
