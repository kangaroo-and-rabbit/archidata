package org.atriasoft.archidata.tools;

/**
 * Simple DTO holding a public key in JSON string format.
 *
 * <p>Used for exchanging RSA public keys between services (e.g. SSO).</p>
 */
public class PublicKey {
	public String key;

	/** Default constructor required for Jackson deserialization by external libraries. */
	public PublicKey() {}

	public PublicKey(final String key) {
		this.key = key;
	}
}
