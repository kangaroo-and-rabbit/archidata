package test.atriasoft.archidata.dataAccess;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.atriasoft.archidata.annotation.Collection;
import org.atriasoft.archidata.annotation.DataEncrypt;
import org.atriasoft.archidata.annotation.DataNotRead;
import org.atriasoft.archidata.annotation.Id;
import org.atriasoft.archidata.annotation.ManyToManyDoc;
import org.atriasoft.archidata.crypto.AesGcmCipherProvider;
import org.atriasoft.archidata.crypto.EncryptionKeyGenerator;
import org.atriasoft.archidata.crypto.EncryptionKeyStore;
import org.atriasoft.archidata.dataAccess.commonTools.ManyToManyTools;
import org.atriasoft.archidata.dataAccess.options.FilterValue;
import org.atriasoft.archidata.dataAccess.options.ReadAllColumn;
import org.atriasoft.archidata.tools.ConfigBaseVariable;
import org.bson.types.ObjectId;
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

/**
 * A full update ({@code updateById} without {@code FilterValue}) never writes the {@code @DataNotRead} and
 * {@code @DataEncrypt} fields: a standard read leaves them {@code null}, and a read-then-update must not
 * erase them. Naming them in a {@code FilterValue} still writes them.
 */
@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestProtectedFieldUpdate {

	/** Entity mixing a clear field and the two kinds of protected fields. */
	@Collection("protectedFieldEntity")
	public static class ProtectedEntity {
		@Id
		public ObjectId _id = null;
		public String visible;
		@DataNotRead
		public String secret;
		@DataEncrypt
		public String encrypted;
		@ManyToManyDoc(targetEntity = ProtectedRemote.class, remoteField = "parents")
		public List<ObjectId> remotes;
	}

	/** Remote side of the many-to-many link. */
	@Collection("protectedFieldRemote")
	public static class ProtectedRemote {
		@Id
		public ObjectId _id = null;
		public String name;
		@ManyToManyDoc(targetEntity = ProtectedEntity.class, remoteField = "remotes")
		public List<ObjectId> parents;
	}

	private static ObjectId entityId = null;

	@BeforeAll
	public static void configureWebServer() throws Exception {
		entityId = null;
		ConfigureDb.configure();
		final byte[] aesKey = Base64.getDecoder().decode(EncryptionKeyGenerator.generateAesKeyBase64());
		EncryptionKeyStore.register(EncryptionKeyStore.DEFAULT_KEY_NAME, new AesGcmCipherProvider(aesKey));
		ConfigBaseVariable.setDataDecryptEnable("false");
	}

	@AfterAll
	public static void removeDataBase() throws IOException {
		ConfigBaseVariable.setDataDecryptEnable("false");
		EncryptionKeyStore.reset();
		ConfigureDb.clear();
	}

	private static ProtectedEntity readAll() throws Exception {
		ConfigBaseVariable.setDataDecryptEnable("true");
		try {
			return ConfigureDb.da.getById(ProtectedEntity.class, entityId, new ReadAllColumn());
		} finally {
			ConfigBaseVariable.setDataDecryptEnable("false");
		}
	}

	@Order(1)
	@Test
	public void testInsertWritesProtectedFields() throws Exception {
		final ProtectedEntity entity = new ProtectedEntity();
		entity.visible = "v1";
		entity.secret = "secret-1";
		entity.encrypted = "encrypted-1";
		entityId = ConfigureDb.da.insert(entity)._id;
		final ProtectedEntity stored = readAll();
		Assertions.assertEquals("secret-1", stored.secret);
		Assertions.assertEquals("encrypted-1", stored.encrypted);
	}

	@Order(2)
	@Test
	public void testReadThenFullUpdateKeepsProtectedFields() throws Exception {
		// Standard read: @DataNotRead not projected, @DataEncrypt not decrypted -> both null.
		final ProtectedEntity read = ConfigureDb.da.getById(ProtectedEntity.class, entityId);
		Assertions.assertNull(read.secret);
		Assertions.assertNull(read.encrypted);
		read.visible = "v2";
		ConfigureDb.da.updateById(read, entityId);
		final ProtectedEntity stored = readAll();
		Assertions.assertEquals("v2", stored.visible);
		Assertions.assertEquals("secret-1", stored.secret);
		Assertions.assertEquals("encrypted-1", stored.encrypted);
	}

	@Order(3)
	@Test
	public void testFullUpdateDoesNotOverwriteProtectedFields() throws Exception {
		final ProtectedEntity update = new ProtectedEntity();
		update.visible = "v3";
		update.secret = "client-secret";
		update.encrypted = "client-encrypted";
		ConfigureDb.da.updateById(update, entityId);
		final ProtectedEntity stored = readAll();
		Assertions.assertEquals("v3", stored.visible);
		Assertions.assertEquals("secret-1", stored.secret);
		Assertions.assertEquals("encrypted-1", stored.encrypted);
	}

	@Order(4)
	@Test
	public void testExplicitFilterValueWritesProtectedFields() throws Exception {
		final ProtectedEntity update = new ProtectedEntity();
		update.secret = "secret-2";
		update.encrypted = "encrypted-2";
		ConfigureDb.da.updateById(update, entityId, new FilterValue("secret", "encrypted"));
		final ProtectedEntity stored = readAll();
		Assertions.assertEquals("v3", stored.visible);
		Assertions.assertEquals("secret-2", stored.secret);
		Assertions.assertEquals("encrypted-2", stored.encrypted);
		// Named explicitly with a null value: the field is removed.
		ConfigureDb.da.updateById(new ProtectedEntity(), entityId, new FilterValue("secret"));
		Assertions.assertNull(readAll().secret);
		Assertions.assertEquals("encrypted-2", readAll().encrypted);
	}

	@Order(5)
	@Test
	public void testEditableFieldsExcludeProtectedFields() throws Exception {
		final List<String> editable = FilterValue.getEditableFieldsNames(ProtectedEntity.class).getValues();
		Assertions.assertTrue(editable.contains("visible"));
		Assertions.assertTrue(editable.contains("remotes"));
		Assertions.assertFalse(editable.contains("secret"));
		Assertions.assertFalse(editable.contains("encrypted"));
		Assertions.assertFalse(editable.contains("_id"));
		Assertions.assertTrue(FilterValue.getAllFieldsNames(ProtectedEntity.class).getValues().contains("secret"));
	}

	@Order(6)
	@Test
	public void testUpdateRemoteLinksKeepsProtectedFields() throws Exception {
		ConfigureDb.da.updateById(buildSecret("secret-3"), entityId, new FilterValue("secret"));
		final ProtectedRemote remote = new ProtectedRemote();
		remote.name = "r1";
		final ObjectId remoteId = ConfigureDb.da.insert(remote)._id;
		ManyToManyTools.addLink(ConfigureDb.da, ProtectedEntity.class, entityId, "remotes", remoteId);
		ManyToManyTools.updateRemoteLinks(ProtectedEntity.class, "remotes", false);
		final ProtectedEntity stored = readAll();
		Assertions.assertEquals("v3", stored.visible);
		Assertions.assertEquals("secret-3", stored.secret);
		Assertions.assertEquals("encrypted-2", stored.encrypted);
		Assertions.assertEquals(new ArrayList<>(List.of(remoteId)), new ArrayList<>(stored.remotes));
	}

	private static ProtectedEntity buildSecret(final String secret) {
		final ProtectedEntity out = new ProtectedEntity();
		out.secret = secret;
		return out;
	}
}
