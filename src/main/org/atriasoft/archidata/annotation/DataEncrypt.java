package org.atriasoft.archidata.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field whose value must be encrypted when serialized to the database and decrypted when
 * read back from the server.
 *
 * <p>The actual key material is resolved at runtime from the
 * {@link org.atriasoft.archidata.crypto.EncryptionKeyStore} using the logical key names below, so a
 * key can be injected from a Docker secret or registered dynamically (per organisation/sub-domain).</p>
 *
 * <p>Two usage styles are supported:</p>
 * <ul>
 * <li>{@code @DataEncrypt} — uses the base key (symmetric, one key encrypts and decrypts).</li>
 * <li>{@code @DataEncrypt(encryptKey = "org-pub", decryptKey = "org-priv")} — uses distinct
 * encryption and decryption keys (asymmetric), which also supports the encrypt-only case where the
 * server holds only the public key.</li>
 * </ul>
 *
 * <p>Decryption on read only happens when archidata is explicitly authorized through the
 * {@code DATA_DECRYPT_ENABLE} configuration flag; otherwise the field is left unset on read while
 * the stored value stays encrypted.</p>
 *
 * <p>Scope: currently supported on fields whose database representation is a {@code String}.</p>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DataEncrypt {

	/**
	 * Logical name of the base key, used for both encryption and decryption when no explicit
	 * {@link #encryptKey()} / {@link #decryptKey()} is provided.
	 *
	 * @return the base key name (defaults to the store's default key).
	 */
	String value() default org.atriasoft.archidata.crypto.EncryptionKeyStore.DEFAULT_KEY_NAME;

	/**
	 * Logical name of the key used to encrypt. When non-empty, takes precedence over {@link #value()}.
	 *
	 * @return the encryption key name, or empty to use {@link #value()}.
	 */
	String encryptKey() default "";

	/**
	 * Logical name of the key used to decrypt. When non-empty, takes precedence over {@link #value()}.
	 *
	 * @return the decryption key name, or empty to use {@link #value()}.
	 */
	String decryptKey() default "";
}
