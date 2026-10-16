package test.atriasoft.archidata.dataAccess;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import org.atriasoft.archidata.annotation.Collection;
import org.atriasoft.archidata.annotation.DataEncrypt;
import org.atriasoft.archidata.annotation.DataNotRead;
import org.atriasoft.archidata.annotation.Id;
import org.atriasoft.archidata.crypto.AesGcmCipherProvider;
import org.atriasoft.archidata.crypto.EncryptionKeyGenerator;
import org.atriasoft.archidata.crypto.EncryptionKeyStore;
import org.atriasoft.archidata.dataAccess.DBAccessMongo;
import org.atriasoft.archidata.dataAccess.Filters;
import org.atriasoft.archidata.dataAccess.options.Condition;
import org.atriasoft.archidata.dataAccess.options.ReadAllColumn;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import test.atriasoft.archidata.ConfigureDb;

/** D-09: the TRACE logs of the reads mask the values of the protected fields. */
public class TestTraceMasking {

	/** Entity with a clear, a hidden and an encrypted field. */
	@Collection("traceMasking")
	public static class Traced {
		@Id
		public ObjectId _id = null;
		public String name;
		@DataNotRead
		public String hiddenToken;
		@DataEncrypt
		public String encrypted;
	}

	@BeforeAll
	public static void configureWebServer() throws Exception {
		ConfigureDb.configure();
		final byte[] aesKey = Base64.getDecoder().decode(EncryptionKeyGenerator.generateAesKeyBase64());
		EncryptionKeyStore.register(EncryptionKeyStore.DEFAULT_KEY_NAME, new AesGcmCipherProvider(aesKey));
	}

	@AfterAll
	public static void removeDataBase() throws IOException {
		EncryptionKeyStore.reset();
		ConfigureDb.clear();
	}

	@Test
	public void testTraceMasksProtectedValues() throws Exception {
		final Traced entity = new Traced();
		entity.name = "visible-name";
		entity.hiddenToken = "hidden-token-value";
		entity.encrypted = "clear-secret";
		ConfigureDb.da.insert(entity);

		final Logger logger = (Logger) LoggerFactory.getLogger(DBAccessMongo.class);
		final Level previous = logger.getLevel();
		final ListAppender<ILoggingEvent> appender = new ListAppender<>();
		appender.start();
		logger.addAppender(appender);
		logger.setLevel(Level.TRACE);
		try {
			ConfigureDb.da.gets(Traced.class, new ReadAllColumn(), new Condition(
					Filters.and(Filters.eq("name", "visible-name"), Filters.eq("hiddenToken", "hidden-token-value"))));
		} finally {
			logger.setLevel(previous);
			logger.detachAppender(appender);
		}
		final String logs = appender.list.stream().map(ILoggingEvent::getFormattedMessage)
				.collect(Collectors.joining("\n"));
		Assertions.assertTrue(logs.contains("filter ="), logs);
		Assertions.assertTrue(logs.contains("receive data from DB"), logs);
		Assertions.assertTrue(logs.contains("visible-name"), logs);
		Assertions.assertTrue(logs.contains("***"), logs);
		Assertions.assertFalse(logs.contains("hidden-token-value"), logs);
		Assertions.assertFalse(logs.contains("clear-secret"), logs);
		final List<ILoggingEvent> traces = appender.list;
		Assertions.assertFalse(traces.isEmpty());
	}
}
