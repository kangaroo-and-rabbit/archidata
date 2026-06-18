package test.atriasoft.archidata.dataAccess;

import java.io.IOException;
import java.security.KeyPair;
import java.util.Base64;

import org.atriasoft.archidata.crypto.AesGcmCipherProvider;
import org.atriasoft.archidata.crypto.EncryptedEnvelope;
import org.atriasoft.archidata.crypto.EncryptionKeyGenerator;
import org.atriasoft.archidata.crypto.EncryptionKeyStore;
import org.atriasoft.archidata.crypto.RsaCipherProvider;
import org.atriasoft.archidata.crypto.X25519SealedBoxProvider;
import org.atriasoft.archidata.tools.ConfigBaseVariable;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;

import test.atriasoft.archidata.ConfigureDb;
import test.atriasoft.archidata.StepwiseExtension;
import test.atriasoft.archidata.dataAccess.model.SecuredTable;
import test.atriasoft.archidata.dataAccess.model.SecuredTableRaw;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestSecuredField {

	private static final String CLEAR = "public-non-secret";
	private static final String SECRET_SYM = "symmetric-secret-éà-value";
	private static final String SECRET_X = "x25519-secret-value-with-accents-çù";
	private static final String SECRET_RSA = "rsa-secret-value";
	private static final String SECRET_ENC_ONLY = "write-only-secret";

	private static Long objectId = null;

	@BeforeAll
	public static void configureWebServer() throws Exception {
		objectId = null;
		ConfigureDb.configure();
		// Register the encryption keys (in production these come from Docker secrets).
		final byte[] aesKey = Base64.getDecoder().decode(EncryptionKeyGenerator.generateAesKeyBase64());
		EncryptionKeyStore.register(EncryptionKeyStore.DEFAULT_KEY_NAME, new AesGcmCipherProvider(aesKey));

		final KeyPair x25519 = EncryptionKeyGenerator.generateX25519();
		EncryptionKeyStore.register("x-pub", new X25519SealedBoxProvider(x25519.getPublic(), null));
		EncryptionKeyStore.register("x-priv", new X25519SealedBoxProvider(null, x25519.getPrivate()));

		final KeyPair rsa = EncryptionKeyGenerator.generateRsa(2048);
		EncryptionKeyStore.register("rsa-pub", new RsaCipherProvider(rsa.getPublic(), null));
		EncryptionKeyStore.register("rsa-priv", new RsaCipherProvider(null, rsa.getPrivate()));

		// Authorize decryption on read.
		ConfigBaseVariable.setDataDecryptEnable("true");
	}

	@AfterAll
	public static void removeDataBase() throws IOException {
		EncryptionKeyStore.reset();
		ConfigureDb.clear();
	}

	@Order(1)
	@Test
	public void testInsertAndDecryptRoundTrip() throws Exception {
		final SecuredTable entry = new SecuredTable();
		entry.clearData = CLEAR;
		entry.symmetricData = SECRET_SYM;
		entry.asymX25519 = SECRET_X;
		entry.asymRsa = SECRET_RSA;
		entry.encryptOnly = SECRET_ENC_ONLY;
		final SecuredTable inserted = ConfigureDb.da.insert(entry);
		Assertions.assertNotNull(inserted.getId());
		objectId = inserted.getId();

		final SecuredTable retrieve = ConfigureDb.da.getById(SecuredTable.class, objectId);
		Assertions.assertNotNull(retrieve);
		Assertions.assertEquals(CLEAR, retrieve.clearData);
		Assertions.assertEquals(SECRET_SYM, retrieve.symmetricData);
		Assertions.assertEquals(SECRET_X, retrieve.asymX25519);
		Assertions.assertEquals(SECRET_RSA, retrieve.asymRsa);
		// encryptOnly has no private key registered for decryption => left unset.
		Assertions.assertNull(retrieve.encryptOnly);
	}

	@Order(2)
	@Test
	public void testStoredValuesAreEncrypted() throws Exception {
		final SecuredTableRaw raw = ConfigureDb.da.getById(SecuredTableRaw.class, objectId);
		Assertions.assertNotNull(raw);
		// Clear field is stored verbatim.
		Assertions.assertEquals(CLEAR, raw.clearData);
		// Encrypted fields are stored as opaque envelopes, never in clear.
		Assertions.assertNotEquals(SECRET_SYM, raw.symmetricData);
		Assertions.assertNotEquals(SECRET_X, raw.asymX25519);
		Assertions.assertNotEquals(SECRET_RSA, raw.asymRsa);
		Assertions.assertTrue(EncryptedEnvelope.isEnvelope(raw.symmetricData));
		Assertions.assertTrue(EncryptedEnvelope.isEnvelope(raw.asymX25519));
		Assertions.assertTrue(EncryptedEnvelope.isEnvelope(raw.asymRsa));
	}

	@Order(3)
	@Test
	public void testDecryptionDisabledLeavesFieldsUnset() throws Exception {
		ConfigBaseVariable.setDataDecryptEnable("false");
		try {
			final SecuredTable retrieve = ConfigureDb.da.getById(SecuredTable.class, objectId);
			Assertions.assertNotNull(retrieve);
			// Clear field still readable, encrypted fields are not decrypted.
			Assertions.assertEquals(CLEAR, retrieve.clearData);
			Assertions.assertNull(retrieve.symmetricData);
			Assertions.assertNull(retrieve.asymX25519);
			Assertions.assertNull(retrieve.asymRsa);
		} finally {
			ConfigBaseVariable.setDataDecryptEnable("true");
		}
	}
}
