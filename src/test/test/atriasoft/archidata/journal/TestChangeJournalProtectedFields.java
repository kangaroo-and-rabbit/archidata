package test.atriasoft.archidata.journal;

import java.io.IOException;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.atriasoft.archidata.annotation.Collection;
import org.atriasoft.archidata.annotation.DataEncrypt;
import org.atriasoft.archidata.annotation.DataNotRead;
import org.atriasoft.archidata.crypto.AesGcmCipherProvider;
import org.atriasoft.archidata.crypto.EncryptionKeyGenerator;
import org.atriasoft.archidata.crypto.EncryptionKeyStore;
import org.atriasoft.archidata.journal.ChangeJournalEngine;
import org.atriasoft.archidata.model.OIDGenericDataSoftDelete;
import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import test.atriasoft.archidata.ConfigureDb;

/** D-03: the journal never copies the {@code @DataNotRead} fields, and keeps {@code @DataEncrypt} encrypted. */
public class TestChangeJournalProtectedFields {
	private static final String COLLECTION = "journalProtected";

	/** Journalized entity with secrets. */
	@Collection(COLLECTION)
	public static class Account extends OIDGenericDataSoftDelete {
		public String login;
		@DataNotRead
		public String passwordHash;
		@DataEncrypt
		public String encrypted;
	}

	@BeforeAll
	public static void configureWebServer() throws Exception {
		ConfigureDb.configure();
		final byte[] aesKey = Base64.getDecoder().decode(EncryptionKeyGenerator.generateAesKeyBase64());
		EncryptionKeyStore.register(EncryptionKeyStore.DEFAULT_KEY_NAME, new AesGcmCipherProvider(aesKey));
		JournalTestTools.dropJournal();
	}

	@AfterAll
	public static void removeDataBase() throws IOException {
		EncryptionKeyStore.reset();
		ConfigureDb.clear();
	}

	@Test
	public void testProtectedFieldsAreNotJournalized() throws Exception {
		final Account account = new Account();
		account.login = "bob";
		account.passwordHash = "hash-secret-value";
		account.encrypted = "clear-encrypted-value";
		ConfigureDb.da.insert(account);
		// Another collection captured by name, with an explicit exclusion.
		JournalTestTools.database().getCollection("journalRawTokens")
				.insertOne(new Document("token", "raw-token").append("label", "kept").append("updatedAt", new Date()));

		final ChangeJournalEngine engine = new ChangeJournalEngine();
		engine.addClass(Account.class);
		engine.addCollection("journalRawTokens");
		engine.excludeFields("journalRawTokens", "token");
		Assertions.assertEquals(Set.of("passwordHash"), engine.getExcludedFields(COLLECTION));
		engine.run();

		final List<Document> entries = JournalTestTools.journalEntries();
		Assertions.assertEquals(2, entries.size());
		for (final Document entry : entries) {
			final String data = entry.getString("data");
			Assertions.assertFalse(data.contains("hash-secret-value"), data);
			Assertions.assertFalse(data.contains("clear-encrypted-value"), data);
			Assertions.assertFalse(data.contains("raw-token"), data);
			if (COLLECTION.equals(entry.getString("collectionName"))) {
				final Document captured = Document.parse(data);
				Assertions.assertEquals("bob", captured.getString("login"));
				Assertions.assertFalse(captured.containsKey("passwordHash"));
				// Stored (encrypted) form, and the system fields are kept.
				Assertions.assertNotNull(captured.get("encrypted"));
				Assertions.assertTrue(captured.containsKey("updatedAt"));
				Assertions.assertTrue(captured.containsKey("deleted"));
			} else {
				Assertions.assertTrue(data.contains("kept"), data);
			}
		}
	}
}
