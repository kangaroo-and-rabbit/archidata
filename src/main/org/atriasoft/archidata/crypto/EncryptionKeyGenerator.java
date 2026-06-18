package org.atriasoft.archidata.crypto;

import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

/**
 * Helper to generate encryption keys for the supported schemes, in the formats accepted by
 * {@link KeyMaterialLoader} (Base64 for symmetric keys, PEM for asymmetric keys).
 *
 * <p>Intended to produce the key files mounted as Docker secrets in development. Never commit
 * real production keys to the repository.</p>
 */
public final class EncryptionKeyGenerator {

	private static final int PEM_LINE_LENGTH = 64;

	private EncryptionKeyGenerator() {
		// Utility class.
	}

	/**
	 * Generates a random AES-256 key encoded as Base64 (suitable for the symmetric scheme).
	 *
	 * @return the Base64-encoded 32-byte key.
	 */
	public static String generateAesKeyBase64() {
		return Base64.getEncoder().encodeToString(CryptoTools.randomBytes(32));
	}

	/**
	 * Generates an X25519 key pair for the sealed-box asymmetric scheme.
	 *
	 * @return the generated key pair.
	 * @throws Exception if key generation fails.
	 */
	public static KeyPair generateX25519() throws Exception {
		return KeyPairGenerator.getInstance("X25519").generateKeyPair();
	}

	/**
	 * Generates an RSA key pair for the RSA asymmetric scheme.
	 *
	 * @param bits the modulus size in bits (e.g. 2048 or 4096).
	 * @return the generated key pair.
	 * @throws Exception if key generation fails.
	 */
	public static KeyPair generateRsa(final int bits) throws Exception {
		final KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
		generator.initialize(bits);
		return generator.generateKeyPair();
	}

	/**
	 * Encodes a public key as a PEM {@code PUBLIC KEY} block (X.509 SubjectPublicKeyInfo).
	 *
	 * @param key the public key.
	 * @return the PEM text.
	 */
	public static String toPublicPem(final Key key) {
		return toPem("PUBLIC KEY", key.getEncoded());
	}

	/**
	 * Encodes a private key as a PEM {@code PRIVATE KEY} block (PKCS#8).
	 *
	 * @param key the private key.
	 * @return the PEM text.
	 */
	public static String toPrivatePem(final Key key) {
		return toPem("PRIVATE KEY", key.getEncoded());
	}

	private static String toPem(final String label, final byte[] der) {
		final String base64 = Base64.getEncoder().encodeToString(der);
		final StringBuilder builder = new StringBuilder();
		builder.append("-----BEGIN ").append(label).append("-----\n");
		for (int i = 0; i < base64.length(); i += PEM_LINE_LENGTH) {
			builder.append(base64, i, Math.min(i + PEM_LINE_LENGTH, base64.length())).append('\n');
		}
		builder.append("-----END ").append(label).append("-----\n");
		return builder.toString();
	}
}
