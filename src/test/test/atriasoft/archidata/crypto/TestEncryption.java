package test.atriasoft.archidata.crypto;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import org.atriasoft.archidata.crypto.AesGcmCipherProvider;
import org.atriasoft.archidata.crypto.CipherProvider;
import org.atriasoft.archidata.crypto.EncryptedEnvelope;
import org.atriasoft.archidata.crypto.EncryptionKeyGenerator;
import org.atriasoft.archidata.crypto.EncryptionKeyStore;
import org.atriasoft.archidata.crypto.EncryptionScheme;
import org.atriasoft.archidata.crypto.FieldEncryptionContext;
import org.atriasoft.archidata.crypto.KeyMaterialLoader;
import org.atriasoft.archidata.crypto.RsaCipherProvider;
import org.atriasoft.archidata.crypto.X25519SealedBoxProvider;
import org.atriasoft.archidata.dataAccess.model.DbClassModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import test.atriasoft.archidata.dataAccess.model.BadEncryptedTable;

/**
 * Pure cryptographic-layer tests, independent of any database.
 */
public class TestEncryption {

	private static final byte[] MESSAGE = "Some secret value with accents éàçù and emojis 🔐"
			.getBytes(StandardCharsets.UTF_8);

	@Test
	public void testAesGcmRoundTrip() throws Exception {
		final byte[] key = Base64.getDecoder().decode(EncryptionKeyGenerator.generateAesKeyBase64());
		final CipherProvider provider = new AesGcmCipherProvider(key);
		Assertions.assertTrue(provider.canEncrypt());
		Assertions.assertTrue(provider.canDecrypt());
		final byte[] payload = provider.encrypt(MESSAGE);
		Assertions.assertArrayEquals(MESSAGE, provider.decrypt(payload));
		// Two encryptions of the same message must differ (random nonce).
		Assertions.assertFalse(java.util.Arrays.equals(payload, provider.encrypt(MESSAGE)));
	}

	@Test
	public void testX25519SealedBoxRoundTrip() throws Exception {
		final KeyPair pair = EncryptionKeyGenerator.generateX25519();
		final CipherProvider encryptOnly = new X25519SealedBoxProvider(pair.getPublic(), null);
		final CipherProvider decryptOnly = new X25519SealedBoxProvider(null, pair.getPrivate());
		Assertions.assertTrue(encryptOnly.canEncrypt());
		Assertions.assertFalse(encryptOnly.canDecrypt());
		Assertions.assertTrue(decryptOnly.canDecrypt());
		final byte[] payload = encryptOnly.encrypt(MESSAGE);
		Assertions.assertArrayEquals(MESSAGE, decryptOnly.decrypt(payload));
	}

	@Test
	public void testRsaRoundTrip() throws Exception {
		final KeyPair pair = EncryptionKeyGenerator.generateRsa(2048);
		final CipherProvider provider = new RsaCipherProvider(pair.getPublic(), pair.getPrivate());
		final byte[] payload = provider.encrypt(MESSAGE);
		Assertions.assertArrayEquals(MESSAGE, provider.decrypt(payload));
	}

	@Test
	public void testEnvelopeCarriesScheme() throws Exception {
		final byte[] key = Base64.getDecoder().decode(EncryptionKeyGenerator.generateAesKeyBase64());
		final CipherProvider provider = new AesGcmCipherProvider(key);
		final String envelope = EncryptedEnvelope.encode(provider.getScheme(), provider.encrypt(MESSAGE));
		Assertions.assertTrue(EncryptedEnvelope.isEnvelope(envelope));
		final EncryptedEnvelope.Parsed parsed = EncryptedEnvelope.decode(envelope);
		Assertions.assertEquals(EncryptionScheme.AES_GCM, parsed.scheme());
		Assertions.assertArrayEquals(MESSAGE, provider.decrypt(parsed.payload()));
	}

	@Test
	public void testKeyMaterialLoaderSymmetricBase64() throws Exception {
		final String base64Key = EncryptionKeyGenerator.generateAesKeyBase64();
		final CipherProvider provider = KeyMaterialLoader.fromBytes(base64Key.getBytes(StandardCharsets.UTF_8));
		Assertions.assertEquals(EncryptionScheme.AES_GCM, provider.getScheme());
		Assertions.assertArrayEquals(MESSAGE, provider.decrypt(provider.encrypt(MESSAGE)));
	}

	@Test
	public void testKeyMaterialLoaderX25519Pem() throws Exception {
		final KeyPair pair = EncryptionKeyGenerator.generateX25519();
		final CipherProvider pub = KeyMaterialLoader
				.fromBytes(EncryptionKeyGenerator.toPublicPem(pair.getPublic()).getBytes(StandardCharsets.UTF_8));
		final CipherProvider priv = KeyMaterialLoader
				.fromBytes(EncryptionKeyGenerator.toPrivatePem(pair.getPrivate()).getBytes(StandardCharsets.UTF_8));
		Assertions.assertEquals(EncryptionScheme.X25519, pub.getScheme());
		Assertions.assertTrue(pub.canEncrypt());
		Assertions.assertTrue(priv.canDecrypt());
		Assertions.assertArrayEquals(MESSAGE, priv.decrypt(pub.encrypt(MESSAGE)));
	}

	@Test
	public void testKeyMaterialLoaderRsaPem() throws Exception {
		final KeyPair pair = EncryptionKeyGenerator.generateRsa(2048);
		final CipherProvider pub = KeyMaterialLoader
				.fromBytes(EncryptionKeyGenerator.toPublicPem(pair.getPublic()).getBytes(StandardCharsets.UTF_8));
		final CipherProvider priv = KeyMaterialLoader
				.fromBytes(EncryptionKeyGenerator.toPrivatePem(pair.getPrivate()).getBytes(StandardCharsets.UTF_8));
		Assertions.assertEquals(EncryptionScheme.RSA, pub.getScheme());
		Assertions.assertArrayEquals(MESSAGE, priv.decrypt(pub.encrypt(MESSAGE)));
	}

	@Test
	public void testEd25519KeyRejected() throws Exception {
		final KeyPair pair = KeyPairGenerator.getInstance("Ed25519").generateKeyPair();
		final byte[] pem = EncryptionKeyGenerator.toPublicPem(pair.getPublic()).getBytes(StandardCharsets.UTF_8);
		Assertions.assertThrows(IllegalArgumentException.class, () -> KeyMaterialLoader.fromBytes(pem));
	}

	@Test
	public void testFieldEncryptionContextSymmetric() throws Exception {
		EncryptionKeyStore.reset();
		try {
			final byte[] key = Base64.getDecoder().decode(EncryptionKeyGenerator.generateAesKeyBase64());
			EncryptionKeyStore.register("ctx-sym", new AesGcmCipherProvider(key));
			final FieldEncryptionContext context = new FieldEncryptionContext("ctx-sym", "ctx-sym");
			final String clear = "hello world";
			final String envelope = context.encrypt(clear);
			Assertions.assertTrue(EncryptedEnvelope.isEnvelope(envelope));
			Assertions.assertTrue(context.canDecrypt());
			Assertions.assertEquals(clear, context.decrypt(envelope));
		} finally {
			EncryptionKeyStore.reset();
		}
	}

	@Test
	public void testFieldEncryptionContextEncryptOnlyCannotDecrypt() throws Exception {
		EncryptionKeyStore.reset();
		try {
			final KeyPair pair = EncryptionKeyGenerator.generateX25519();
			EncryptionKeyStore.register("pub-only", new X25519SealedBoxProvider(pair.getPublic(), null));
			final FieldEncryptionContext context = new FieldEncryptionContext("pub-only", "pub-only");
			final String envelope = context.encrypt("data");
			Assertions.assertTrue(EncryptedEnvelope.isEnvelope(envelope));
			Assertions.assertFalse(context.canDecrypt());
		} finally {
			EncryptionKeyStore.reset();
		}
	}

	// ========== Error / edge cases ==========

	@Test
	public void testWrongAesKeyFailsDecryption() throws Exception {
		final byte[] keyA = Base64.getDecoder().decode(EncryptionKeyGenerator.generateAesKeyBase64());
		final byte[] keyB = Base64.getDecoder().decode(EncryptionKeyGenerator.generateAesKeyBase64());
		final byte[] payload = new AesGcmCipherProvider(keyA).encrypt(MESSAGE);
		// Wrong key: GCM authentication tag verification must fail.
		Assertions.assertThrows(Exception.class, () -> new AesGcmCipherProvider(keyB).decrypt(payload));
	}

	@Test
	public void testWrongX25519KeyFailsDecryption() throws Exception {
		final KeyPair pairA = EncryptionKeyGenerator.generateX25519();
		final KeyPair pairB = EncryptionKeyGenerator.generateX25519();
		final byte[] payload = new X25519SealedBoxProvider(pairA.getPublic(), null).encrypt(MESSAGE);
		Assertions.assertThrows(Exception.class,
				() -> new X25519SealedBoxProvider(null, pairB.getPrivate()).decrypt(payload));
	}

	@Test
	public void testWrongRsaKeyFailsDecryption() throws Exception {
		final KeyPair pairA = EncryptionKeyGenerator.generateRsa(2048);
		final KeyPair pairB = EncryptionKeyGenerator.generateRsa(2048);
		final byte[] payload = new RsaCipherProvider(pairA.getPublic(), null).encrypt(MESSAGE);
		Assertions.assertThrows(Exception.class,
				() -> new RsaCipherProvider(null, pairB.getPrivate()).decrypt(payload));
	}

	@Test
	public void testTamperedPayloadFailsDecryption() throws Exception {
		final byte[] key = Base64.getDecoder().decode(EncryptionKeyGenerator.generateAesKeyBase64());
		final AesGcmCipherProvider provider = new AesGcmCipherProvider(key);
		final byte[] payload = provider.encrypt(MESSAGE);
		// Flip one bit in the ciphertext body.
		payload[payload.length - 1] ^= 0x01;
		Assertions.assertThrows(Exception.class, () -> provider.decrypt(payload));
	}

	@Test
	public void testCorruptedEnvelopeRejected() {
		Assertions.assertFalse(EncryptedEnvelope.isEnvelope("not even base64 !!"));
		Assertions.assertFalse(EncryptedEnvelope.isEnvelope(Base64.getEncoder().encodeToString(new byte[] { 1, 2 })));
		Assertions.assertThrows(IllegalArgumentException.class, () -> EncryptedEnvelope.decode("####"));
	}

	@Test
	public void testSchemeMismatchOnDecryptThrows() throws Exception {
		EncryptionKeyStore.reset();
		try {
			final byte[] aes = Base64.getDecoder().decode(EncryptionKeyGenerator.generateAesKeyBase64());
			final KeyPair rsa = EncryptionKeyGenerator.generateRsa(2048);
			EncryptionKeyStore.register("enc-aes", new AesGcmCipherProvider(aes));
			EncryptionKeyStore.register("dec-rsa", new RsaCipherProvider(rsa.getPublic(), rsa.getPrivate()));
			final FieldEncryptionContext context = new FieldEncryptionContext("enc-aes", "dec-rsa");
			final String envelope = context.encrypt("data"); // encrypted with AES
			// Decryption key is RSA: scheme mismatch must be detected.
			Assertions.assertThrows(IllegalStateException.class, () -> context.decrypt(envelope));
		} finally {
			EncryptionKeyStore.reset();
		}
	}

	@Test
	public void testMissingEncryptKeyThrows() throws Exception {
		EncryptionKeyStore.reset();
		try {
			final FieldEncryptionContext context = new FieldEncryptionContext("does-not-exist", "does-not-exist");
			Assertions.assertThrows(IllegalStateException.class, () -> context.encrypt("data"));
			Assertions.assertFalse(context.canDecrypt());
		} finally {
			EncryptionKeyStore.reset();
		}
	}

	@Test
	public void testMissingDecryptKeyDirectDecryptThrows() throws Exception {
		EncryptionKeyStore.reset();
		try {
			final byte[] key = Base64.getDecoder().decode(EncryptionKeyGenerator.generateAesKeyBase64());
			EncryptionKeyStore.register("only-enc", new AesGcmCipherProvider(key));
			// Encrypt with a known key, but decrypt name points to a missing key.
			final String envelope = new FieldEncryptionContext("only-enc", "only-enc").encrypt("data");
			final FieldEncryptionContext context = new FieldEncryptionContext("only-enc", "missing");
			Assertions.assertFalse(context.canDecrypt());
			Assertions.assertThrows(IllegalStateException.class, () -> context.decrypt(envelope));
		} finally {
			EncryptionKeyStore.reset();
		}
	}

	// ========== Provider capability matrix ==========

	@Test
	public void testAesKeyLengthValidation() {
		Assertions.assertDoesNotThrow(() -> new AesGcmCipherProvider(new byte[16]));
		Assertions.assertDoesNotThrow(() -> new AesGcmCipherProvider(new byte[24]));
		Assertions.assertDoesNotThrow(() -> new AesGcmCipherProvider(new byte[32]));
		Assertions.assertThrows(IllegalArgumentException.class, () -> new AesGcmCipherProvider(null));
		Assertions.assertThrows(IllegalArgumentException.class, () -> new AesGcmCipherProvider(new byte[0]));
		Assertions.assertThrows(IllegalArgumentException.class, () -> new AesGcmCipherProvider(new byte[31]));
		Assertions.assertThrows(IllegalArgumentException.class, () -> new AesGcmCipherProvider(new byte[64]));
	}

	@Test
	public void testAsymmetricProvidersRequireAtLeastOneKey() throws Exception {
		final KeyPair x = EncryptionKeyGenerator.generateX25519();
		final KeyPair rsa = EncryptionKeyGenerator.generateRsa(2048);
		Assertions.assertThrows(IllegalArgumentException.class, () -> new X25519SealedBoxProvider(null, null));
		Assertions.assertThrows(IllegalArgumentException.class, () -> new RsaCipherProvider(null, null));
		// sanity: single-key construction is allowed
		Assertions.assertDoesNotThrow(() -> new X25519SealedBoxProvider(x.getPublic(), null));
		Assertions.assertDoesNotThrow(() -> new RsaCipherProvider(null, rsa.getPrivate()));
	}

	@Test
	public void testEncryptWithDecryptOnlyProviderThrows() throws Exception {
		final KeyPair x = EncryptionKeyGenerator.generateX25519();
		final KeyPair rsa = EncryptionKeyGenerator.generateRsa(2048);
		final CipherProvider xDecOnly = new X25519SealedBoxProvider(null, x.getPrivate());
		final CipherProvider rsaDecOnly = new RsaCipherProvider(null, rsa.getPrivate());
		Assertions.assertFalse(xDecOnly.canEncrypt());
		Assertions.assertFalse(rsaDecOnly.canEncrypt());
		Assertions.assertThrows(IllegalStateException.class, () -> xDecOnly.encrypt(MESSAGE));
		Assertions.assertThrows(IllegalStateException.class, () -> rsaDecOnly.encrypt(MESSAGE));
	}

	@Test
	public void testDecryptWithEncryptOnlyProviderThrows() throws Exception {
		final KeyPair x = EncryptionKeyGenerator.generateX25519();
		final KeyPair rsa = EncryptionKeyGenerator.generateRsa(2048);
		final CipherProvider xEncOnly = new X25519SealedBoxProvider(x.getPublic(), null);
		final CipherProvider rsaEncOnly = new RsaCipherProvider(rsa.getPublic(), null);
		final byte[] xPayload = xEncOnly.encrypt(MESSAGE);
		final byte[] rsaPayload = rsaEncOnly.encrypt(MESSAGE);
		Assertions.assertThrows(IllegalStateException.class, () -> xEncOnly.decrypt(xPayload));
		Assertions.assertThrows(IllegalStateException.class, () -> rsaEncOnly.decrypt(rsaPayload));
	}

	// ========== Payload corner cases ==========

	@Test
	public void testEmptyPayloadAllSchemes() throws Exception {
		final byte[] empty = new byte[0];
		final byte[] aes = Base64.getDecoder().decode(EncryptionKeyGenerator.generateAesKeyBase64());
		final KeyPair x = EncryptionKeyGenerator.generateX25519();
		final KeyPair rsa = EncryptionKeyGenerator.generateRsa(2048);
		final CipherProvider aesP = new AesGcmCipherProvider(aes);
		final CipherProvider xP = new X25519SealedBoxProvider(x.getPublic(), x.getPrivate());
		final CipherProvider rsaP = new RsaCipherProvider(rsa.getPublic(), rsa.getPrivate());
		Assertions.assertArrayEquals(empty, aesP.decrypt(aesP.encrypt(empty)));
		Assertions.assertArrayEquals(empty, xP.decrypt(xP.encrypt(empty)));
		Assertions.assertArrayEquals(empty, rsaP.decrypt(rsaP.encrypt(empty)));
	}

	@Test
	public void testLargePayloadAesAndX25519() throws Exception {
		final byte[] large = new byte[200_000];
		for (int i = 0; i < large.length; i++) {
			large[i] = (byte) (i * 7);
		}
		final byte[] aes = Base64.getDecoder().decode(EncryptionKeyGenerator.generateAesKeyBase64());
		final KeyPair x = EncryptionKeyGenerator.generateX25519();
		final CipherProvider aesP = new AesGcmCipherProvider(aes);
		final CipherProvider xP = new X25519SealedBoxProvider(x.getPublic(), x.getPrivate());
		Assertions.assertArrayEquals(large, aesP.decrypt(aesP.encrypt(large)));
		Assertions.assertArrayEquals(large, xP.decrypt(xP.encrypt(large)));
	}

	@Test
	public void testRsaRejectsTooLargePayload() throws Exception {
		final KeyPair rsa = EncryptionKeyGenerator.generateRsa(2048);
		final CipherProvider rsaP = new RsaCipherProvider(rsa.getPublic(), rsa.getPrivate());
		// RSA-2048 OAEP-SHA256 supports at most ~190 bytes.
		final byte[] tooBig = new byte[256];
		Assertions.assertThrows(Exception.class, () -> rsaP.encrypt(tooBig));
	}

	// ========== X25519 raw-encoding robustness ==========

	@Test
	public void testX25519RoundTripStress() throws Exception {
		for (int i = 0; i < 25; i++) {
			final KeyPair pair = EncryptionKeyGenerator.generateX25519();
			final CipherProvider provider = new X25519SealedBoxProvider(pair.getPublic(), pair.getPrivate());
			Assertions.assertArrayEquals(MESSAGE, provider.decrypt(provider.encrypt(MESSAGE)));
		}
	}

	// ========== Envelope / scheme edge cases ==========

	@Test
	public void testEnvelopeNullAndEmpty() {
		Assertions.assertFalse(EncryptedEnvelope.isEnvelope(null));
		Assertions.assertFalse(EncryptedEnvelope.isEnvelope(""));
	}

	@Test
	public void testEnvelopeEmptyPayloadRoundTrip() {
		final String envelope = EncryptedEnvelope.encode(EncryptionScheme.AES_GCM, new byte[0]);
		Assertions.assertTrue(EncryptedEnvelope.isEnvelope(envelope));
		final EncryptedEnvelope.Parsed parsed = EncryptedEnvelope.decode(envelope);
		Assertions.assertEquals(EncryptionScheme.AES_GCM, parsed.scheme());
		Assertions.assertEquals(0, parsed.payload().length);
	}

	@Test
	public void testEnvelopeBadVersionRejected() {
		// magic 'A','E', version=9, scheme=1
		final String frame = Base64.getEncoder().encodeToString(new byte[] { 'A', 'E', 9, 1, 0 });
		Assertions.assertFalse(EncryptedEnvelope.isEnvelope(frame));
		Assertions.assertThrows(IllegalArgumentException.class, () -> EncryptedEnvelope.decode(frame));
	}

	@Test
	public void testEnvelopeUnknownSchemeRejected() {
		// magic 'A','E', version=1, scheme=99 (unknown)
		final String frame = Base64.getEncoder().encodeToString(new byte[] { 'A', 'E', 1, 99, 0 });
		Assertions.assertThrows(IllegalArgumentException.class, () -> EncryptedEnvelope.decode(frame));
	}

	@Test
	public void testEnvelopeBadMagicRejected() {
		final String frame = Base64.getEncoder().encodeToString(new byte[] { 'X', 'Y', 1, 1, 0 });
		Assertions.assertFalse(EncryptedEnvelope.isEnvelope(frame));
		Assertions.assertThrows(IllegalArgumentException.class, () -> EncryptedEnvelope.decode(frame));
	}

	@Test
	public void testEncryptionSchemeFromId() {
		for (final EncryptionScheme scheme : EncryptionScheme.values()) {
			Assertions.assertEquals(scheme, EncryptionScheme.fromId(scheme.getId()));
		}
		Assertions.assertThrows(IllegalArgumentException.class, () -> EncryptionScheme.fromId((byte) 42));
	}

	// ========== KeyMaterialLoader edge cases ==========

	@Test
	public void testKeyLoaderRawBytes() throws Exception {
		final byte[] raw = new byte[32];
		for (int i = 0; i < raw.length; i++) {
			raw[i] = (byte) (i + 1);
		}
		final CipherProvider provider = KeyMaterialLoader.fromBytes(raw);
		Assertions.assertEquals(EncryptionScheme.AES_GCM, provider.getScheme());
		Assertions.assertArrayEquals(MESSAGE, provider.decrypt(provider.encrypt(MESSAGE)));
	}

	@Test
	public void testKeyLoaderInvalidLengthRejected() {
		Assertions.assertThrows(IllegalArgumentException.class, () -> KeyMaterialLoader.fromBytes(new byte[31]));
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> KeyMaterialLoader.fromBytes("hello".getBytes(StandardCharsets.UTF_8)));
	}

	@Test
	public void testKeyLoaderPemCombinedPublicAndPrivate() throws Exception {
		final KeyPair pair = EncryptionKeyGenerator.generateX25519();
		final String combined = EncryptionKeyGenerator.toPublicPem(pair.getPublic())
				+ EncryptionKeyGenerator.toPrivatePem(pair.getPrivate());
		final CipherProvider provider = KeyMaterialLoader.fromBytes(combined.getBytes(StandardCharsets.UTF_8));
		Assertions.assertTrue(provider.canEncrypt());
		Assertions.assertTrue(provider.canDecrypt());
		Assertions.assertArrayEquals(MESSAGE, provider.decrypt(provider.encrypt(MESSAGE)));
	}

	@Test
	public void testKeyLoaderPemWithCrlf() throws Exception {
		final KeyPair pair = EncryptionKeyGenerator.generateRsa(2048);
		final String pubCrlf = EncryptionKeyGenerator.toPublicPem(pair.getPublic()).replace("\n", "\r\n");
		final String privCrlf = EncryptionKeyGenerator.toPrivatePem(pair.getPrivate()).replace("\n", "\r\n");
		final CipherProvider pub = KeyMaterialLoader.fromBytes(pubCrlf.getBytes(StandardCharsets.UTF_8));
		final CipherProvider priv = KeyMaterialLoader.fromBytes(privCrlf.getBytes(StandardCharsets.UTF_8));
		Assertions.assertArrayEquals(MESSAGE, priv.decrypt(pub.encrypt(MESSAGE)));
	}

	// ========== Codec build guard ==========

	@Test
	public void testNonStringFieldRejectedAtModelBuild() {
		Assertions.assertThrows(Exception.class, () -> DbClassModel.of(BadEncryptedTable.class));
	}
}
