package org.atriasoft.archidata.crypto;

import java.security.PrivateKey;
import java.security.PublicKey;

import javax.crypto.Cipher;

/**
 * Asymmetric {@link CipherProvider} backed by RSA-OAEP (SHA-256).
 *
 * <p>The public key encrypts and the private key decrypts. A provider holding only the public key
 * can encrypt but not decrypt (typical of a front-end server that must store secrets it cannot read
 * back).</p>
 *
 * <p>Note: RSA-OAEP can only encrypt payloads smaller than the modulus. For larger field values
 * prefer the {@link X25519SealedBoxProvider} or {@link AesGcmCipherProvider} schemes.</p>
 */
public final class RsaCipherProvider implements CipherProvider {

	private static final String TRANSFORMATION = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";

	private final PublicKey publicKey;
	private final PrivateKey privateKey;

	/**
	 * Builds an RSA provider.
	 *
	 * @param publicKey the public key used to encrypt (may be {@code null} if only decrypting).
	 * @param privateKey the private key used to decrypt (may be {@code null} if only encrypting).
	 */
	public RsaCipherProvider(final PublicKey publicKey, final PrivateKey privateKey) {
		if (publicKey == null && privateKey == null) {
			throw new IllegalArgumentException("RSA provider requires at least one key");
		}
		this.publicKey = publicKey;
		this.privateKey = privateKey;
	}

	@Override
	public EncryptionScheme getScheme() {
		return EncryptionScheme.RSA;
	}

	@Override
	public boolean canEncrypt() {
		return this.publicKey != null;
	}

	@Override
	public boolean canDecrypt() {
		return this.privateKey != null;
	}

	@Override
	public byte[] encrypt(final byte[] clear) throws Exception {
		if (this.publicKey == null) {
			throw new IllegalStateException("RSA provider has no public key: cannot encrypt");
		}
		final Cipher cipher = Cipher.getInstance(TRANSFORMATION);
		cipher.init(Cipher.ENCRYPT_MODE, this.publicKey);
		return cipher.doFinal(clear);
	}

	@Override
	public byte[] decrypt(final byte[] payload) throws Exception {
		if (this.privateKey == null) {
			throw new IllegalStateException("RSA provider has no private key: cannot decrypt");
		}
		final Cipher cipher = Cipher.getInstance(TRANSFORMATION);
		cipher.init(Cipher.DECRYPT_MODE, this.privateKey);
		return cipher.doFinal(payload);
	}
}
