package org.atriasoft.archidata.crypto;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.MGF1ParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;

/**
 * Asymmetric {@link CipherProvider} backed by RSA-OAEP (SHA-256).
 *
 * <p>The OAEP parameters are explicit: SHA-256 digest, MGF1 with SHA-1 (what the SunJCE provider uses for
 * {@code RSA/ECB/OAEPWithSHA-256AndMGF1Padding}). Other providers (BouncyCastle) read that transformation
 * name as MGF1 with SHA-256, so the explicit parameters keep the stored data readable whatever the provider.
 * MGF1-SHA-1 is kept on purpose: moving to MGF1-SHA-256 would make the data already stored unreadable, and
 * the envelope carries no version to tell the two apart (see the pending format change).</p>
 *
 * <p>The public key encrypts and the private key decrypts. A provider holding only the public key
 * can encrypt but not decrypt (typical of a front-end server that must store secrets it cannot read
 * back).</p>
 *
 * <p>Note: RSA-OAEP can only encrypt payloads smaller than the modulus. For larger field values
 * prefer the {@link X25519SealedBoxProvider} or {@link AesGcmCipherProvider} schemes.</p>
 */
public final class RsaCipherProvider implements CipherProvider {

	private static final String TRANSFORMATION = "RSA/ECB/OAEPPadding";
	/** SHA-256 digest, MGF1-SHA-1: the parameters of the data already stored (SunJCE default). */
	static final OAEPParameterSpec OAEP_PARAMETERS = new OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA1,
			PSource.PSpecified.DEFAULT);

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
		cipher.init(Cipher.ENCRYPT_MODE, this.publicKey, OAEP_PARAMETERS);
		return cipher.doFinal(clear);
	}

	@Override
	public byte[] decrypt(final byte[] payload) throws Exception {
		if (this.privateKey == null) {
			throw new IllegalStateException("RSA provider has no private key: cannot decrypt");
		}
		final Cipher cipher = Cipher.getInstance(TRANSFORMATION);
		cipher.init(Cipher.DECRYPT_MODE, this.privateKey, OAEP_PARAMETERS);
		return cipher.doFinal(payload);
	}
}
