package org.atriasoft.archidata.crypto;

import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Low-level cryptographic primitives shared by the {@link CipherProvider} implementations.
 *
 * <p>All algorithms used here are provided natively by the JDK (AES-GCM, HMAC-SHA256).</p>
 */
final class CryptoTools {

	/** GCM nonce length in bytes (96 bits, the recommended value). */
	static final int GCM_NONCE_LENGTH = 12;
	/** GCM authentication tag length in bits. */
	static final int GCM_TAG_BITS = 128;

	private static final SecureRandom RANDOM = new SecureRandom();

	private CryptoTools() {
		// Utility class.
	}

	/**
	 * Tells whether a byte length is a valid AES key length (128/192/256-bit).
	 *
	 * @param length the candidate key length in bytes.
	 * @return {@code true} for 16, 24 or 32 bytes.
	 */
	static boolean isValidAesKeyLength(final int length) {
		return length == 16 || length == 24 || length == 32;
	}

	/**
	 * Generates a cryptographically strong random byte array.
	 *
	 * @param length the number of bytes.
	 * @return the random bytes.
	 */
	static byte[] randomBytes(final int length) {
		final byte[] out = new byte[length];
		RANDOM.nextBytes(out);
		return out;
	}

	/**
	 * Encrypts a payload with AES-GCM. The output is {@code nonce || ciphertext+tag}.
	 *
	 * @param key the AES key bytes (16, 24 or 32 bytes).
	 * @param clear the clear-text bytes.
	 * @return the nonce-prefixed ciphertext.
	 * @throws Exception if encryption fails.
	 */
	static byte[] aesGcmEncrypt(final byte[] key, final byte[] clear) throws Exception {
		final byte[] nonce = randomBytes(GCM_NONCE_LENGTH);
		final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
		cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(GCM_TAG_BITS, nonce));
		final byte[] cipherText = cipher.doFinal(clear);
		final byte[] out = new byte[nonce.length + cipherText.length];
		System.arraycopy(nonce, 0, out, 0, nonce.length);
		System.arraycopy(cipherText, 0, out, nonce.length, cipherText.length);
		return out;
	}

	/**
	 * Decrypts a {@code nonce || ciphertext+tag} payload produced by {@link #aesGcmEncrypt}.
	 *
	 * @param key the AES key bytes.
	 * @param payload the nonce-prefixed ciphertext.
	 * @return the clear-text bytes.
	 * @throws Exception if decryption or authentication fails.
	 */
	static byte[] aesGcmDecrypt(final byte[] key, final byte[] payload) throws Exception {
		if (payload.length < GCM_NONCE_LENGTH) {
			throw new IllegalArgumentException("AES-GCM payload too short");
		}
		final byte[] nonce = new byte[GCM_NONCE_LENGTH];
		System.arraycopy(payload, 0, nonce, 0, GCM_NONCE_LENGTH);
		final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
		cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(GCM_TAG_BITS, nonce));
		return cipher.doFinal(payload, GCM_NONCE_LENGTH, payload.length - GCM_NONCE_LENGTH);
	}

	/**
	 * HKDF (RFC 5869) extract-and-expand using HMAC-SHA256.
	 *
	 * @param ikm the input key material.
	 * @param salt the optional salt (may be {@code null}).
	 * @param info the optional context info (may be {@code null}).
	 * @param length the number of output bytes requested.
	 * @return the derived key material.
	 * @throws Exception if the HMAC computation fails.
	 */
	static byte[] hkdfSha256(final byte[] ikm, final byte[] salt, final byte[] info, final int length)
			throws Exception {
		final Mac mac = Mac.getInstance("HmacSHA256");
		final int hashLen = mac.getMacLength();
		// Extract
		final byte[] effectiveSalt = (salt == null || salt.length == 0) ? new byte[hashLen] : salt;
		mac.init(new SecretKeySpec(effectiveSalt, "HmacSHA256"));
		final byte[] prk = mac.doFinal(ikm);
		// Expand
		mac.init(new SecretKeySpec(prk, "HmacSHA256"));
		final byte[] effectiveInfo = info == null ? new byte[0] : info;
		final byte[] out = new byte[length];
		byte[] previousBlock = new byte[0];
		int generated = 0;
		byte counter = 1;
		while (generated < length) {
			mac.update(previousBlock);
			mac.update(effectiveInfo);
			mac.update(counter);
			previousBlock = mac.doFinal();
			final int toCopy = Math.min(previousBlock.length, length - generated);
			System.arraycopy(previousBlock, 0, out, generated, toCopy);
			generated += toCopy;
			counter++;
		}
		return out;
	}
}
