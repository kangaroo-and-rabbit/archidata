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
import org.atriasoft.archidata.dataAccess.options.FilterValue;
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

/**
 * Value corner cases for {@code @DataEncrypt}: null, empty, large/unicode payloads and the update path.
 */
@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestSecuredFieldValues {

	@BeforeAll
	public static void configureWebServer() throws Exception {
		ConfigureDb.configure();
		EncryptionKeyStore.reset();
		final byte[] aes = Base64.getDecoder().decode(EncryptionKeyGenerator.generateAesKeyBase64());
		EncryptionKeyStore.register(EncryptionKeyStore.DEFAULT_KEY_NAME, new AesGcmCipherProvider(aes));
		final KeyPair x = EncryptionKeyGenerator.generateX25519();
		EncryptionKeyStore.register("x-pub", new X25519SealedBoxProvider(x.getPublic(), null));
		EncryptionKeyStore.register("x-priv", new X25519SealedBoxProvider(null, x.getPrivate()));
		final KeyPair rsa = EncryptionKeyGenerator.generateRsa(2048);
		EncryptionKeyStore.register("rsa-pub", new RsaCipherProvider(rsa.getPublic(), null));
		EncryptionKeyStore.register("rsa-priv", new RsaCipherProvider(null, rsa.getPrivate()));
		ConfigBaseVariable.setDataDecryptEnable("true");
	}

	@AfterAll
	public static void removeDataBase() throws IOException {
		EncryptionKeyStore.reset();
		ConfigureDb.clear();
	}

	@Order(1)
	@Test
	public void testNullEncryptedFieldsRoundTrip() throws Exception {
		// null encrypted fields are never encrypted (no key needed) and read back as null.
		final SecuredTable entry = new SecuredTable();
		entry.clearData = "only-clear";
		final SecuredTable inserted = ConfigureDb.da.insert(entry);
		final Long id = inserted.getId();

		final SecuredTable retrieve = ConfigureDb.da.getById(SecuredTable.class, id);
		Assertions.assertEquals("only-clear", retrieve.clearData);
		Assertions.assertNull(retrieve.symmetricData);
		Assertions.assertNull(retrieve.asymX25519);
		Assertions.assertNull(retrieve.asymRsa);

		// Nothing was stored for the null fields.
		final SecuredTableRaw raw = ConfigureDb.da.getById(SecuredTableRaw.class, id);
		Assertions.assertNull(raw.symmetricData);
		Assertions.assertNull(raw.asymX25519);
	}

	@Order(2)
	@Test
	public void testEmptyStringRoundTrip() throws Exception {
		final SecuredTable entry = new SecuredTable();
		entry.clearData = "c";
		entry.symmetricData = "";
		entry.asymX25519 = "";
		entry.asymRsa = "";
		final Long id = ConfigureDb.da.insert(entry).getId();

		// Empty string is a real value: it is encrypted (stored envelope is not empty).
		final SecuredTableRaw raw = ConfigureDb.da.getById(SecuredTableRaw.class, id);
		Assertions.assertTrue(EncryptedEnvelope.isEnvelope(raw.symmetricData));

		final SecuredTable retrieve = ConfigureDb.da.getById(SecuredTable.class, id);
		Assertions.assertEquals("", retrieve.symmetricData);
		Assertions.assertEquals("", retrieve.asymX25519);
		Assertions.assertEquals("", retrieve.asymRsa);
	}

	@Order(3)
	@Test
	public void testLargeAndUnicodeRoundTrip() throws Exception {
		final StringBuilder big = new StringBuilder();
		for (int i = 0; i < 2000; i++) {
			big.append("éàçù-secret-🔐-").append(i).append('\n');
		}
		final String large = big.toString();
		final SecuredTable entry = new SecuredTable();
		entry.clearData = "c";
		entry.symmetricData = large;
		entry.asymX25519 = large;
		entry.asymRsa = "short-rsa-é"; // RSA cannot hold large payloads
		final Long id = ConfigureDb.da.insert(entry).getId();

		final SecuredTable retrieve = ConfigureDb.da.getById(SecuredTable.class, id);
		Assertions.assertEquals(large, retrieve.symmetricData);
		Assertions.assertEquals(large, retrieve.asymX25519);
		Assertions.assertEquals("short-rsa-é", retrieve.asymRsa);
	}

	@Order(4)
	@Test
	public void testUpdateEncryptedField() throws Exception {
		final SecuredTable entry = new SecuredTable();
		entry.clearData = "c";
		entry.symmetricData = "first-value";
		final Long id = ConfigureDb.da.insert(entry).getId();
		final SecuredTableRaw rawBefore = ConfigureDb.da.getById(SecuredTableRaw.class, id);

		final SecuredTable patch = new SecuredTable();
		patch.symmetricData = "second-value";
		ConfigureDb.da.updateById(patch, id, new FilterValue("symmetricData"));

		final SecuredTable retrieve = ConfigureDb.da.getById(SecuredTable.class, id);
		Assertions.assertEquals("second-value", retrieve.symmetricData);

		// The stored ciphertext changed and is still an opaque envelope.
		final SecuredTableRaw rawAfter = ConfigureDb.da.getById(SecuredTableRaw.class, id);
		Assertions.assertTrue(EncryptedEnvelope.isEnvelope(rawAfter.symmetricData));
		Assertions.assertNotEquals(rawBefore.symmetricData, rawAfter.symmetricData);
		Assertions.assertNotEquals("second-value", rawAfter.symmetricData);
	}
}
