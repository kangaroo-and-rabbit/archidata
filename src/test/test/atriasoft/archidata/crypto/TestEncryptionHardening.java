package test.atriasoft.archidata.crypto;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.Base64;

import javax.crypto.Cipher;

import org.atriasoft.archidata.crypto.AesGcmCipherProvider;
import org.atriasoft.archidata.crypto.EncryptionKeyGenerator;
import org.atriasoft.archidata.crypto.EncryptionKeyStore;
import org.atriasoft.archidata.crypto.RsaCipherProvider;
import org.atriasoft.archidata.tools.ConfigBaseVariable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** D-20: no hot swap of a key once the configuration is locked; RSA-OAEP parameters explicit and unchanged. */
class TestEncryptionHardening {

	@AfterEach
	void restore() {
		if (ConfigBaseVariable.isLocked()) {
			ConfigBaseVariable.allowReconfiguration(true);
			ConfigBaseVariable.unlock();
			ConfigBaseVariable.allowReconfiguration(false);
		}
		EncryptionKeyStore.reset();
	}

	private static AesGcmCipherProvider aes() {
		return new AesGcmCipherProvider(Base64.getDecoder().decode(EncryptionKeyGenerator.generateAesKeyBase64()));
	}

	@Test
	void testExistingKeyCannotBeReplacedOnceLocked() {
		final AesGcmCipherProvider first = aes();
		EncryptionKeyStore.register(EncryptionKeyStore.DEFAULT_KEY_NAME, first);
		// Unlocked: replacement allowed (tests, startup).
		EncryptionKeyStore.register(EncryptionKeyStore.DEFAULT_KEY_NAME, first);
		ConfigBaseVariable.lock();
		Assertions.assertThrows(IllegalStateException.class,
				() -> EncryptionKeyStore.register(EncryptionKeyStore.DEFAULT_KEY_NAME, aes()));
		Assertions.assertSame(first, EncryptionKeyStore.get(EncryptionKeyStore.DEFAULT_KEY_NAME));
		// A new organisation key can still be added at runtime.
		EncryptionKeyStore.register("org-42", aes());
		Assertions.assertTrue(EncryptionKeyStore.contains("org-42"));
	}

	@Test
	void testRsaParametersStayCompatibleWithStoredData() throws Exception {
		final KeyPair pair = EncryptionKeyGenerator.generateRsa(2048);
		final RsaCipherProvider provider = new RsaCipherProvider(pair.getPublic(), pair.getPrivate());
		final byte[] clear = "secret-value".getBytes(StandardCharsets.UTF_8);
		// Data written by the previous versions: the SunJCE default of this transformation name.
		final Cipher legacy = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
		legacy.init(Cipher.ENCRYPT_MODE, pair.getPublic());
		Assertions.assertArrayEquals(clear, provider.decrypt(legacy.doFinal(clear)));
		// And the data written now is readable by the previous versions.
		final Cipher legacyDecrypt = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding");
		legacyDecrypt.init(Cipher.DECRYPT_MODE, pair.getPrivate());
		Assertions.assertArrayEquals(clear, legacyDecrypt.doFinal(provider.encrypt(clear)));
	}
}
