package org.atriasoft.archidata.crypto;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds a {@link CipherProvider} from raw key-file content, auto-detecting the format.
 *
 * <p>Supported formats:</p>
 * <ul>
 * <li>A raw or Base64 symmetric key (16, 24 or 32 bytes) → {@link AesGcmCipherProvider}.</li>
 * <li>One or more PEM blocks ({@code PUBLIC KEY} / {@code PRIVATE KEY}) carrying an RSA or X25519
 * key → {@link RsaCipherProvider} or {@link X25519SealedBoxProvider}.</li>
 * </ul>
 */
public final class KeyMaterialLoader {

	private static final Pattern PEM_BLOCK = Pattern.compile("-----BEGIN ([^-]+)-----(.*?)-----END \\1-----",
			Pattern.DOTALL);

	private static final String[] CANDIDATE_ALGORITHMS = { "RSA", "X25519", "Ed25519" };

	private KeyMaterialLoader() {
		// Utility class.
	}

	/**
	 * Builds a provider from key-file content.
	 *
	 * @param content the raw bytes of the key file.
	 * @return the matching cipher provider.
	 * @throws Exception if the content cannot be interpreted as a supported key.
	 */
	public static CipherProvider fromBytes(final byte[] content) throws Exception {
		final String text = new String(content, java.nio.charset.StandardCharsets.UTF_8);
		if (text.contains("-----BEGIN ")) {
			return fromPem(text);
		}
		return fromSymmetric(content, text);
	}

	private static CipherProvider fromSymmetric(final byte[] content, final String text) {
		// Try Base64 first (typical for a key stored as text).
		final String trimmed = text.trim();
		try {
			final byte[] decoded = Base64.getDecoder().decode(trimmed);
			if (CryptoTools.isValidAesKeyLength(decoded.length)) {
				return new AesGcmCipherProvider(decoded);
			}
		} catch (final IllegalArgumentException e) {
			// Not Base64, fall through to raw bytes.
		}
		if (CryptoTools.isValidAesKeyLength(content.length)) {
			return new AesGcmCipherProvider(content);
		}
		throw new IllegalArgumentException("Unsupported symmetric key: expected 16/24/32 bytes (raw or Base64), got "
				+ content.length + " raw bytes");
	}

	private static CipherProvider fromPem(final String text) throws Exception {
		PublicKey publicKey = null;
		PrivateKey privateKey = null;
		String algorithm = null;
		final Matcher matcher = PEM_BLOCK.matcher(text);
		while (matcher.find()) {
			final String label = matcher.group(1).trim();
			final byte[] der = Base64.getMimeDecoder().decode(matcher.group(2).replaceAll("\\s", ""));
			if (label.contains("PRIVATE")) {
				privateKey = decodePrivate(der);
				algorithm = privateKey.getAlgorithm();
			} else if (label.contains("PUBLIC")) {
				publicKey = decodePublic(der);
				algorithm = publicKey.getAlgorithm();
			}
		}
		if (algorithm == null) {
			throw new IllegalArgumentException("No PUBLIC/PRIVATE KEY block found in PEM content");
		}
		return providerFor(algorithm, publicKey, privateKey);
	}

	private static CipherProvider providerFor(
			final String algorithm,
			final PublicKey publicKey,
			final PrivateKey privateKey) {
		final String alg = algorithm.toUpperCase();
		if (alg.contains("RSA")) {
			return new RsaCipherProvider(publicKey, privateKey);
		}
		if (alg.contains("XDH") || alg.contains("X25519")) {
			return new X25519SealedBoxProvider(publicKey, privateKey);
		}
		if (alg.contains("ED25519") || alg.contains("EDDSA")) {
			throw new IllegalArgumentException(
					"Ed25519 keys are signature keys and cannot encrypt data. Generate X25519 keys instead.");
		}
		throw new IllegalArgumentException("Unsupported asymmetric key algorithm: " + algorithm);
	}

	private static PublicKey decodePublic(final byte[] der) {
		final X509EncodedKeySpec spec = new X509EncodedKeySpec(der);
		for (final String algorithm : CANDIDATE_ALGORITHMS) {
			try {
				return KeyFactory.getInstance(algorithm).generatePublic(spec);
			} catch (final Exception e) {
				// Try the next candidate algorithm.
			}
		}
		throw new IllegalArgumentException("Unable to decode public key (unsupported algorithm)");
	}

	private static PrivateKey decodePrivate(final byte[] der) {
		final PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);
		for (final String algorithm : CANDIDATE_ALGORITHMS) {
			try {
				return KeyFactory.getInstance(algorithm).generatePrivate(spec);
			} catch (final Exception e) {
				// Try the next candidate algorithm.
			}
		}
		throw new IllegalArgumentException("Unable to decode private key (unsupported algorithm)");
	}
}
