package test.atriasoft.archidata.tools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.atriasoft.archidata.tools.SecretReader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

class TestSecretReader {
	private static final String NAME = "MY_SECRET";
	private static final String VALUE = "value-that-must-not-leak";

	@TempDir
	Path folder;

	private final Map<String, String> environment = new HashMap<>();
	private final SecretReader reader = new SecretReader(this.environment::get);
	private final ListAppender<ILoggingEvent> logs = new ListAppender<>();
	private final Logger logger = (Logger) LoggerFactory.getLogger(SecretReader.class);

	@BeforeEach
	void captureLogs() {
		this.logs.start();
		this.logger.addAppender(this.logs);
	}

	@AfterEach
	void releaseLogs() {
		this.logger.detachAppender(this.logs);
	}

	private void useFile(final byte[] content) throws IOException {
		final Path file = this.folder.resolve("secret");
		Files.write(file, content);
		this.environment.put(NAME + SecretReader.FILE_SUFFIX, file.toString());
	}

	private void useFile(final String content) throws IOException {
		useFile(content.getBytes(StandardCharsets.UTF_8));
	}

	private long warnings() {
		return this.logs.list.stream().filter(event -> event.getLevel() == Level.WARN).count();
	}

	private void assertNothingLogged(final String value) {
		for (final ILoggingEvent event : this.logs.list) {
			Assertions.assertFalse(event.getFormattedMessage().contains(value));
		}
	}

	@Test
	void undefinedSecretIsNull() {
		Assertions.assertNull(this.reader.read(NAME));
		Assertions.assertNull(this.reader.readBytes(NAME));
		Assertions.assertEquals(0, warnings());
	}

	@Test
	void fileIsRead() throws IOException {
		useFile(VALUE);
		Assertions.assertEquals(VALUE, this.reader.read(NAME));
		Assertions.assertEquals(0, warnings());
	}

	@Test
	void lineBreaksAtTheEndOfTheFileAreRemoved() throws IOException {
		useFile(VALUE + "\n");
		Assertions.assertEquals(VALUE, this.reader.read(NAME));
		useFile(VALUE + "\r\n\n");
		Assertions.assertEquals(VALUE, this.reader.read(NAME));
	}

	@Test
	void otherWhitespaceIsPartOfTheSecret() throws IOException {
		useFile(" a\nb \n");
		Assertions.assertEquals(" a\nb ", this.reader.read(NAME));
	}

	@Test
	void bytesAreReturnedUnchanged() throws IOException {
		final byte[] key = { 1, 2, (byte) 0xFF, '\n' };
		useFile(key);
		Assertions.assertArrayEquals(key, this.reader.readBytes(NAME));
	}

	@Test
	void inlineValueIsUsedWithAWarningThatHidesIt() {
		this.environment.put(NAME, VALUE);
		Assertions.assertEquals(VALUE, this.reader.read(NAME));
		Assertions.assertEquals(1, warnings());
		Assertions.assertArrayEquals(VALUE.getBytes(StandardCharsets.UTF_8), this.reader.readBytes(NAME));
		Assertions.assertEquals(2, warnings());
		assertNothingLogged(VALUE);
	}

	@Test
	void emptyInlineValueIsKeptAsTextAndUnsetAsBytes() {
		this.environment.put(NAME, "");
		Assertions.assertEquals("", this.reader.read(NAME));
		Assertions.assertNull(this.reader.readBytes(NAME));
		Assertions.assertEquals(0, warnings());
	}

	@Test
	void fileWinsAndTheLeftoverInlineValueIsReported() throws IOException {
		useFile(VALUE);
		this.environment.put(NAME, "leftover");
		Assertions.assertEquals(VALUE, this.reader.read(NAME));
		Assertions.assertEquals(1, warnings());
		assertNothingLogged("leftover");
	}

	@Test
	void missingFileFailsInsteadOfFallingBack() {
		this.environment.put(NAME + SecretReader.FILE_SUFFIX, this.folder.resolve("missing").toString());
		this.environment.put(NAME, VALUE);
		final IllegalStateException error = Assertions.assertThrows(IllegalStateException.class,
				() -> this.reader.read(NAME));
		Assertions.assertTrue(error.getMessage().contains(NAME + SecretReader.FILE_SUFFIX));
		Assertions.assertFalse(error.getMessage().contains(VALUE));
		Assertions.assertThrows(IllegalStateException.class, () -> this.reader.readBytes(NAME));
	}

	@Test
	void passwordLeftInTheFileVariableStaysOutOfTheError() {
		this.environment.put(NAME + SecretReader.FILE_SUFFIX, VALUE);
		final IllegalStateException error = Assertions.assertThrows(IllegalStateException.class,
				() -> this.reader.read(NAME));
		Assertions.assertFalse(error.getMessage().contains(VALUE));
		Assertions.assertNull(error.getCause());
		this.environment.put(NAME + SecretReader.FILE_SUFFIX, "bad\0" + VALUE);
		final IllegalStateException invalid = Assertions.assertThrows(IllegalStateException.class,
				() -> this.reader.read(NAME));
		Assertions.assertFalse(invalid.getMessage().contains(VALUE));
		Assertions.assertNull(invalid.getCause());
	}

	@Test
	void folderInsteadOfAFileFails() {
		this.environment.put(NAME + SecretReader.FILE_SUFFIX, this.folder.toString());
		Assertions.assertThrows(IllegalStateException.class, () -> this.reader.read(NAME));
	}

	@Test
	void invalidPathFails() {
		this.environment.put(NAME + SecretReader.FILE_SUFFIX, "bad\0path");
		Assertions.assertThrows(IllegalStateException.class, () -> this.reader.read(NAME));
	}

	@Test
	void emptyFileFails() throws IOException {
		useFile("");
		Assertions.assertThrows(IllegalStateException.class, () -> this.reader.read(NAME));
		Assertions.assertThrows(IllegalStateException.class, () -> this.reader.readBytes(NAME));
	}

	@Test
	void fileWithOnlyLineBreaksFails() throws IOException {
		useFile("\n\r\n");
		Assertions.assertThrows(IllegalStateException.class, () -> this.reader.read(NAME));
		Assertions.assertThrows(IllegalStateException.class, () -> this.reader.readBytes(NAME));
	}

	@Test
	void fileOnlyRefusesAnInlineValue() {
		this.environment.put(SecretReader.FILE_ONLY_VARIABLE, "true");
		this.environment.put(NAME, VALUE);
		final IllegalStateException error = Assertions.assertThrows(IllegalStateException.class,
				() -> this.reader.read(NAME));
		Assertions.assertTrue(error.getMessage().contains(NAME));
		Assertions.assertFalse(error.getMessage().contains(VALUE));
		Assertions.assertThrows(IllegalStateException.class, () -> this.reader.readBytes(NAME));
	}

	@Test
	void fileOnlyRefusesAnInlineValueLeftNextToTheFile() throws IOException {
		useFile(VALUE);
		this.environment.put(SecretReader.FILE_ONLY_VARIABLE, "true");
		this.environment.put(NAME, "leftover");
		Assertions.assertThrows(IllegalStateException.class, () -> this.reader.read(NAME));
	}

	@Test
	void fileOnlyAcceptsAFileAndAnUnsetSecret() throws IOException {
		this.environment.put(SecretReader.FILE_ONLY_VARIABLE, "true");
		Assertions.assertNull(this.reader.read(NAME));
		useFile(VALUE);
		Assertions.assertEquals(VALUE, this.reader.read(NAME));
	}

	@Test
	void fileOnlyTreatsAnEmptyInlineValueAsUndefined() {
		this.environment.put(SecretReader.FILE_ONLY_VARIABLE, "true");
		this.environment.put(NAME, "");
		Assertions.assertNull(this.reader.read(NAME));
	}

	@Test
	void invalidFileOnlyValueFailsEvenWithoutASecret() {
		this.environment.put(SecretReader.FILE_ONLY_VARIABLE, "1");
		Assertions.assertThrows(IllegalStateException.class, () -> this.reader.read(NAME));
		this.environment.put(NAME, VALUE);
		Assertions.assertThrows(IllegalStateException.class, () -> this.reader.read(NAME));
		Assertions.assertThrows(IllegalStateException.class, () -> this.reader.readBytes(NAME));
	}

	@Test
	void fileOnlyIgnoresCase() {
		this.environment.put(SecretReader.FILE_ONLY_VARIABLE, "TRUE");
		this.environment.put(NAME, VALUE);
		Assertions.assertThrows(IllegalStateException.class, () -> this.reader.read(NAME));
		this.environment.put(SecretReader.FILE_ONLY_VARIABLE, "False");
		Assertions.assertEquals(VALUE, this.reader.read(NAME));
	}

	@Test
	void fileOnlySetToFalseAllowsAnInlineValue() {
		this.environment.put(SecretReader.FILE_ONLY_VARIABLE, "false");
		this.environment.put(NAME, VALUE);
		Assertions.assertEquals(VALUE, this.reader.read(NAME));
	}
}
