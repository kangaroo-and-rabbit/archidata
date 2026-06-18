package test.atriasoft.archidata.dataAccess;

import java.io.IOException;
import java.security.KeyPair;
import java.util.Base64;

import org.atriasoft.archidata.crypto.AesGcmCipherProvider;
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

/**
 * Failure-mode tests for {@code @DataEncrypt}: missing key on write, wrong key on read.
 */
@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestSecuredFieldErrors {

	@BeforeAll
	public static void configureWebServer() throws Exception {
		ConfigureDb.configure();
		ConfigBaseVariable.setDataDecryptEnable("true");
	}

	@AfterAll
	public static void removeDataBase() throws IOException {
		EncryptionKeyStore.reset();
		ConfigureDb.clear();
	}

	private static SecuredTable sample() {
		final SecuredTable entry = new SecuredTable();
		entry.clearData = "clear";
		entry.symmetricData = "sym";
		entry.asymX25519 = "x";
		entry.asymRsa = "rsa";
		entry.encryptOnly = "enc";
		return entry;
	}

	@Order(1)
	@Test
	public void testInsertFailsWhenEncryptKeyMissing() throws Exception {
		EncryptionKeyStore.reset();
		// Register the asymmetric encryption keys but NOT the symmetric base key ("default").
		final KeyPair x = EncryptionKeyGenerator.generateX25519();
		final KeyPair rsa = EncryptionKeyGenerator.generateRsa(2048);
		EncryptionKeyStore.register("x-pub", new X25519SealedBoxProvider(x.getPublic(), null));
		EncryptionKeyStore.register("rsa-pub", new RsaCipherProvider(rsa.getPublic(), null));

		// Insert must fail because the "default" key is missing...
		Assertions.assertThrows(Exception.class, () -> ConfigureDb.da.insert(sample()));
		// ...and nothing must have been written to the database (no clear-text leak).
		Assertions.assertEquals(0L, ConfigureDb.da.count(SecuredTableRaw.class));
	}

	@Order(2)
	@Test
	public void testWrongKeyOnReadThrows() throws Exception {
		EncryptionKeyStore.reset();
		final byte[] keyA = Base64.getDecoder().decode(EncryptionKeyGenerator.generateAesKeyBase64());
		final KeyPair x = EncryptionKeyGenerator.generateX25519();
		final KeyPair rsa = EncryptionKeyGenerator.generateRsa(2048);
		EncryptionKeyStore.register(EncryptionKeyStore.DEFAULT_KEY_NAME, new AesGcmCipherProvider(keyA));
		EncryptionKeyStore.register("x-pub", new X25519SealedBoxProvider(x.getPublic(), null));
		EncryptionKeyStore.register("x-priv", new X25519SealedBoxProvider(null, x.getPrivate()));
		EncryptionKeyStore.register("rsa-pub", new RsaCipherProvider(rsa.getPublic(), null));
		EncryptionKeyStore.register("rsa-priv", new RsaCipherProvider(null, rsa.getPrivate()));

		final SecuredTable inserted = ConfigureDb.da.insert(sample());
		final Long id = inserted.getId();
		Assertions.assertNotNull(id);

		// Replace the base key with a different one: the stored value can no longer be decrypted.
		final byte[] keyB = Base64.getDecoder().decode(EncryptionKeyGenerator.generateAesKeyBase64());
		EncryptionKeyStore.register(EncryptionKeyStore.DEFAULT_KEY_NAME, new AesGcmCipherProvider(keyB));

		Assertions.assertThrows(Exception.class, () -> ConfigureDb.da.getById(SecuredTable.class, id));
	}
}
