package org.atriasoft.archidata.crypto;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.XECPublicKey;
import java.security.spec.NamedParameterSpec;
import java.security.spec.XECPublicKeySpec;

import javax.crypto.KeyAgreement;

/**
 * Asymmetric {@link CipherProvider} implementing a sealed box over Curve25519 (X25519).
 *
 * <p>Encryption generates an ephemeral X25519 key pair, performs an ECDH with the recipient public
 * key, derives an AES-256 key with HKDF-SHA256 and encrypts the payload with AES-GCM. The resulting
 * payload is {@code ephemeralPublicKey(32) || nonce(12) || ciphertext+tag}.</p>
 *
 * <p>The recipient public key encrypts; the recipient private key decrypts. A provider holding only
 * the public key can encrypt but not read the data back.</p>
 *
 * <p>Keys are native X25519 keys (JEP/JDK {@code XDH}). True Ed25519 keys cannot be used directly
 * for key agreement and would require an Edwards-to-Montgomery conversion that the JDK does not
 * provide; generate dedicated X25519 keys instead.</p>
 */
public final class X25519SealedBoxProvider implements CipherProvider {

	private static final int RAW_KEY_LENGTH = 32;
	private static final int AES_KEY_LENGTH = 32;

	private final PublicKey recipientPublic;
	private final PrivateKey recipientPrivate;

	/**
	 * Builds an X25519 sealed-box provider.
	 *
	 * @param recipientPublic the recipient public key (may be {@code null} if only decrypting).
	 * @param recipientPrivate the recipient private key (may be {@code null} if only encrypting).
	 */
	public X25519SealedBoxProvider(final PublicKey recipientPublic, final PrivateKey recipientPrivate) {
		if (recipientPublic == null && recipientPrivate == null) {
			throw new IllegalArgumentException("X25519 provider requires at least one key");
		}
		this.recipientPublic = recipientPublic;
		this.recipientPrivate = recipientPrivate;
	}

	@Override
	public EncryptionScheme getScheme() {
		return EncryptionScheme.X25519;
	}

	@Override
	public boolean canEncrypt() {
		return this.recipientPublic != null;
	}

	@Override
	public boolean canDecrypt() {
		return this.recipientPrivate != null;
	}

	@Override
	public byte[] encrypt(final byte[] clear) throws Exception {
		if (this.recipientPublic == null) {
			throw new IllegalStateException("X25519 provider has no public key: cannot encrypt");
		}
		final KeyPairGenerator generator = KeyPairGenerator.getInstance("X25519");
		final KeyPair ephemeral = generator.generateKeyPair();
		final byte[] ephemeralRaw = encodeRawPublicKey((XECPublicKey) ephemeral.getPublic());

		final KeyAgreement agreement = KeyAgreement.getInstance("XDH");
		agreement.init(ephemeral.getPrivate());
		agreement.doPhase(this.recipientPublic, true);
		final byte[] shared = agreement.generateSecret();

		final byte[] aesKey = CryptoTools.hkdfSha256(shared, null, ephemeralRaw, AES_KEY_LENGTH);
		final byte[] body = CryptoTools.aesGcmEncrypt(aesKey, clear);

		final byte[] payload = new byte[ephemeralRaw.length + body.length];
		System.arraycopy(ephemeralRaw, 0, payload, 0, ephemeralRaw.length);
		System.arraycopy(body, 0, payload, ephemeralRaw.length, body.length);
		return payload;
	}

	@Override
	public byte[] decrypt(final byte[] payload) throws Exception {
		if (this.recipientPrivate == null) {
			throw new IllegalStateException("X25519 provider has no private key: cannot decrypt");
		}
		if (payload.length < RAW_KEY_LENGTH + CryptoTools.GCM_NONCE_LENGTH) {
			throw new IllegalArgumentException("X25519 payload too short");
		}
		final byte[] ephemeralRaw = new byte[RAW_KEY_LENGTH];
		System.arraycopy(payload, 0, ephemeralRaw, 0, RAW_KEY_LENGTH);
		final byte[] body = new byte[payload.length - RAW_KEY_LENGTH];
		System.arraycopy(payload, RAW_KEY_LENGTH, body, 0, body.length);

		final PublicKey ephemeralPublic = decodeRawPublicKey(ephemeralRaw);
		final KeyAgreement agreement = KeyAgreement.getInstance("XDH");
		agreement.init(this.recipientPrivate);
		agreement.doPhase(ephemeralPublic, true);
		final byte[] shared = agreement.generateSecret();

		final byte[] aesKey = CryptoTools.hkdfSha256(shared, null, ephemeralRaw, AES_KEY_LENGTH);
		return CryptoTools.aesGcmDecrypt(aesKey, body);
	}

	/**
	 * Encodes an X25519 public key as its 32-byte little-endian {@code u}-coordinate (wire format).
	 *
	 * @param key the public key.
	 * @return the raw 32-byte representation.
	 */
	private static byte[] encodeRawPublicKey(final XECPublicKey key) {
		final BigInteger u = key.getU();
		final byte[] big = u.toByteArray(); // big-endian, possibly with a sign byte
		final byte[] bigFixed = new byte[RAW_KEY_LENGTH];
		final int copy = Math.min(big.length, RAW_KEY_LENGTH);
		System.arraycopy(big, big.length - copy, bigFixed, RAW_KEY_LENGTH - copy, copy);
		final byte[] little = new byte[RAW_KEY_LENGTH];
		for (int i = 0; i < RAW_KEY_LENGTH; i++) {
			little[i] = bigFixed[RAW_KEY_LENGTH - 1 - i];
		}
		return little;
	}

	/**
	 * Rebuilds an X25519 public key from its 32-byte little-endian {@code u}-coordinate.
	 *
	 * @param raw the raw 32-byte representation.
	 * @return the reconstructed public key.
	 * @throws Exception if the key cannot be reconstructed.
	 */
	private static PublicKey decodeRawPublicKey(final byte[] raw) throws Exception {
		final byte[] big = new byte[RAW_KEY_LENGTH];
		for (int i = 0; i < RAW_KEY_LENGTH; i++) {
			big[i] = raw[RAW_KEY_LENGTH - 1 - i];
		}
		final BigInteger u = new BigInteger(1, big);
		final KeyFactory keyFactory = KeyFactory.getInstance("XDH");
		return keyFactory.generatePublic(new XECPublicKeySpec(NamedParameterSpec.X25519, u));
	}
}
