/**
 * Transparent field-level encryption: cipher providers, key store and envelope format.
 *
 * <p>A {@code String} field annotated with {@link org.atriasoft.archidata.annotation.DataEncrypt}
 * is encrypted by the data access layer before it is written and decrypted when it is read (the
 * decryption must be authorised by the {@code DATA_DECRYPT_ENABLE} configuration flag). This package
 * holds the machinery behind that annotation; applications mainly interact with the key store.</p>
 *
 * <p>Entry points:</p>
 * <ul>
 * <li>{@link org.atriasoft.archidata.crypto.EncryptionKeyStore}: registry of the named keys
 * ({@link org.atriasoft.archidata.crypto.EncryptionKeyStore#DEFAULT_KEY_NAME} is the base key),
 * filled lazily from the configuration and extensible at runtime with
 * {@link org.atriasoft.archidata.crypto.EncryptionKeyStore#register(String, CipherProvider)}.</li>
 * <li>{@link org.atriasoft.archidata.crypto.CipherProvider}: one key able to encrypt and/or
 * decrypt, implemented by {@link org.atriasoft.archidata.crypto.AesGcmCipherProvider} (AES-256-GCM),
 * {@link org.atriasoft.archidata.crypto.X25519SealedBoxProvider} and
 * {@link org.atriasoft.archidata.crypto.RsaCipherProvider}.</li>
 * <li>{@link org.atriasoft.archidata.crypto.EncryptedEnvelope}: the Base64 frame stored in the
 * field, which embeds the {@link org.atriasoft.archidata.crypto.EncryptionScheme} identifier.</li>
 * <li>{@link org.atriasoft.archidata.crypto.KeyMaterialLoader} and
 * {@link org.atriasoft.archidata.crypto.EncryptionKeyGenerator}: load key files (raw/Base64 or PEM)
 * and generate new keys.</li>
 * </ul>
 *
 * <p>Example, declaring an encrypted field and injecting a per-tenant key:</p>
 * {@snippet lang = java :
 * public class Contact extends OIDGenericDataSoftDelete {
 * 	@DataEncrypt
 * 	public String phone;
 * 	@DataEncrypt("tenant-a")
 * 	public String note;
 * }
 *
 * EncryptionKeyStore.register("tenant-a", KeyMaterialLoader.fromBytes(Files.readAllBytes(keyPath)));
 * }
 *
 * <p>See <code>doc/field_encryption.md</code> in the repository
 * (<a href="https://github.com/kangaroo-and-rabbit/archidata/blob/develop/doc/field_encryption.md">online</a>).</p>
 */
package org.atriasoft.archidata.crypto;
