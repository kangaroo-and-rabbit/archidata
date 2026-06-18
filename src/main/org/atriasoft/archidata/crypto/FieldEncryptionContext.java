package org.atriasoft.archidata.crypto;

import java.nio.charset.StandardCharsets;

/**
 * Resolved encryption configuration for a single field.
 *
 * <p>Holds the logical encryption and decryption key names (resolved from {@code @DataEncrypt}).
 * The actual {@link CipherProvider} is looked up lazily in the {@link EncryptionKeyStore} at each
 * call, so runtime-injected keys (per organisation/sub-domain) are honoured.</p>
 */
public final class FieldEncryptionContext {

	private final String encryptKeyName;
	private final String decryptKeyName;

	/**
	 * Builds a context from the resolved key names.
	 *
	 * @param encryptKeyName the logical name of the key used to encrypt.
	 * @param decryptKeyName the logical name of the key used to decrypt.
	 */
	public FieldEncryptionContext(final String encryptKeyName, final String decryptKeyName) {
		this.encryptKeyName = encryptKeyName;
		this.decryptKeyName = decryptKeyName;
	}

	/**
	 * Encrypts a clear-text string into a Base64 envelope.
	 *
	 * @param clear the clear-text value.
	 * @return the encrypted envelope string.
	 * @throws Exception if no usable encryption key is available or encryption fails.
	 */
	public String encrypt(final String clear) throws Exception {
		final CipherProvider provider = require(this.encryptKeyName, true);
		final byte[] payload = provider.encrypt(clear.getBytes(StandardCharsets.UTF_8));
		return EncryptedEnvelope.encode(provider.getScheme(), payload);
	}

	/**
	 * Indicates whether a decryption key is currently available for this field.
	 *
	 * @return {@code true} if the data can be decrypted.
	 */
	public boolean canDecrypt() {
		final CipherProvider provider = EncryptionKeyStore.get(this.decryptKeyName);
		return provider != null && provider.canDecrypt();
	}

	/**
	 * Decrypts a Base64 envelope string back to its clear-text value.
	 *
	 * @param stored the encrypted envelope string.
	 * @return the clear-text value.
	 * @throws Exception if no usable decryption key is available or decryption fails.
	 */
	public String decrypt(final String stored) throws Exception {
		final EncryptedEnvelope.Parsed parsed = EncryptedEnvelope.decode(stored);
		final CipherProvider provider = require(this.decryptKeyName, false);
		if (provider.getScheme() != parsed.scheme()) {
			throw new IllegalStateException("Decryption key '" + this.decryptKeyName + "' uses scheme "
					+ provider.getScheme() + " but the stored value was encrypted with " + parsed.scheme());
		}
		final byte[] clear = provider.decrypt(parsed.payload());
		return new String(clear, StandardCharsets.UTF_8);
	}

	/**
	 * Looks up the provider registered under a name and checks it can perform the requested operation.
	 *
	 * @param name the logical key name.
	 * @param forEncrypt {@code true} to require encryption capability, {@code false} for decryption.
	 * @return the usable provider.
	 * @throws IllegalStateException if no provider is registered or it lacks the required capability.
	 */
	private static CipherProvider require(final String name, final boolean forEncrypt) {
		final CipherProvider provider = EncryptionKeyStore.get(name);
		if (provider == null) {
			throw new IllegalStateException("No encryption key registered under name '" + name + "'");
		}
		if (forEncrypt && !provider.canEncrypt()) {
			throw new IllegalStateException("Encryption key '" + name + "' cannot encrypt (missing public/secret key)");
		}
		if (!forEncrypt && !provider.canDecrypt()) {
			throw new IllegalStateException("No decryption key available under name '" + name + "'");
		}
		return provider;
	}
}
