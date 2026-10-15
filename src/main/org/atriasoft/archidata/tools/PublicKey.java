package org.atriasoft.archidata.tools;

/**
 * Simple DTO holding a public key in JSON string format.
 *
 * <p>Used for exchanging RSA public keys between services (e.g. SSO).</p>
 *
 * @deprecated Not used by the library: it duplicates {@link JWTWrapper.PublicKey}, the DTO really
 *             used for the public key exchange. Use {@link JWTWrapper.PublicKey} instead.
 */
@Deprecated(forRemoval = true)
public class PublicKey {
	/** The RSA public key in JSON string format. */
	public String key;

	/** Default constructor required for Jackson deserialization by external libraries. */
	public PublicKey() {}

	/**
	 * Creates a new PublicKey with the given key string.
	 * @param key The RSA public key in JSON string format.
	 */
	public PublicKey(final String key) {
		this.key = key;
	}
}
