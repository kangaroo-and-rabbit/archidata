package test.atriasoft.archidata.dataAccess;

import java.io.IOException;
import java.util.Base64;

import org.atriasoft.archidata.annotation.Collection;
import org.atriasoft.archidata.annotation.DataEncrypt;
import org.atriasoft.archidata.annotation.Id;
import org.atriasoft.archidata.annotation.InitValue;
import org.atriasoft.archidata.crypto.AesGcmCipherProvider;
import org.atriasoft.archidata.crypto.EncryptionKeyGenerator;
import org.atriasoft.archidata.crypto.EncryptionKeyStore;
import org.atriasoft.archidata.tools.ConfigBaseVariable;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import test.atriasoft.archidata.ConfigureDb;

/** D-18: an {@code @InitValue} written on a {@code @DataEncrypt} field is encrypted like any value. */
public class TestEncryptedInitValue {

	/** Entity whose encrypted field has a default value. */
	@Collection("encryptedInitValue")
	public static class EncryptedDefault {
		@Id
		public ObjectId _id = null;
		@DataEncrypt
		@InitValue("'default-secret'")
		public String secret;
		@InitValue("42")
		public Long counter;
	}

	/** Raw view of the same collection, without decryption. */
	@Collection("encryptedInitValue")
	public static class EncryptedDefaultRaw {
		@Id
		public ObjectId _id = null;
		public String secret;
		public Long counter;
	}

	@BeforeAll
	public static void configureWebServer() throws Exception {
		ConfigureDb.configure();
		final byte[] aesKey = Base64.getDecoder().decode(EncryptionKeyGenerator.generateAesKeyBase64());
		EncryptionKeyStore.register(EncryptionKeyStore.DEFAULT_KEY_NAME, new AesGcmCipherProvider(aesKey));
		ConfigBaseVariable.setDataDecryptEnable("true");
	}

	@AfterAll
	public static void removeDataBase() throws IOException {
		ConfigBaseVariable.setDataDecryptEnable("false");
		EncryptionKeyStore.reset();
		ConfigureDb.clear();
	}

	@Test
	public void testInitValueIsEncrypted() throws Exception {
		final ObjectId id = ConfigureDb.da.insert(new EncryptedDefault())._id;
		final EncryptedDefaultRaw raw = ConfigureDb.da.getById(EncryptedDefaultRaw.class, id);
		Assertions.assertNotNull(raw.secret);
		Assertions.assertNotEquals("default-secret", raw.secret);
		Assertions.assertEquals(42L, raw.counter);
		final EncryptedDefault read = ConfigureDb.da.getById(EncryptedDefault.class, id);
		Assertions.assertEquals("default-secret", read.secret);
		Assertions.assertEquals(42L, read.counter);
	}
}
