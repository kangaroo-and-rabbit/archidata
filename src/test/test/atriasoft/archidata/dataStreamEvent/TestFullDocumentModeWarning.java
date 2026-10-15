package test.atriasoft.archidata.dataStreamEvent;

import java.io.IOException;
import java.util.List;

import org.atriasoft.archidata.dataStreamEvent.ChangeNotificationManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import com.mongodb.client.model.changestream.FullDocument;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import test.atriasoft.archidata.ConfigureDb;

/**
 * A per-listener or per-watch FullDocument mode is always ignored: a warning is logged as soon as one is
 * passed, before or after {@code start()}.
 */
class TestFullDocumentModeWarning {
	private final Logger managerLogger = (Logger) LoggerFactory.getLogger(ChangeNotificationManager.class);
	private final ListAppender<ILoggingEvent> appender = new ListAppender<>();
	private ChangeNotificationManager manager;

	@BeforeEach
	void setUp() throws Exception {
		ConfigureDb.configure();
		this.manager = ChangeNotificationManager.getInstance();
		if (this.manager.isRunning()) {
			this.manager.stop();
		}
		this.manager.clearAllListeners();
		this.appender.start();
		this.managerLogger.addAppender(this.appender);
	}

	@AfterEach
	void tearDown() throws IOException {
		this.managerLogger.detachAppender(this.appender);
		this.appender.stop();
		if (this.manager.isRunning()) {
			this.manager.stop();
		}
		this.manager.clearAllListeners();
		ConfigureDb.clear();
	}

	private List<ILoggingEvent> ignoredModeWarnings() {
		return this.appender.list.stream()
				.filter(event -> event.getLevel() == Level.WARN && event.getFormattedMessage().contains("IGNORED"))
				.toList();
	}

	@SuppressWarnings("deprecation")
	@Test
	void testModeRegisteredBeforeStartIsWarnedAtOnce() {
		this.manager.registerListener(event -> {}, "modeWarningCollection", FullDocument.UPDATE_LOOKUP);

		final List<ILoggingEvent> warnings = ignoredModeWarnings();
		Assertions.assertEquals(1, warnings.size(), "a mode passed to a listener is always ignored");
		Assertions.assertTrue(warnings.get(0).getFormattedMessage().contains("UPDATE_LOOKUP"));
		Assertions.assertTrue(warnings.get(0).getFormattedMessage().contains("start()"));

		this.manager.start(ConfigureDb.da.getInterface().getDatabase(), FullDocument.DEFAULT);
		Assertions.assertEquals(1, ignoredModeWarnings().size(), "start() does not warn again");
	}

	@SuppressWarnings("deprecation")
	@Test
	void testSameModeAsStartIsWarnedToo() {
		this.manager.start(ConfigureDb.da.getInterface().getDatabase(), FullDocument.DEFAULT);

		this.manager.registerListener(event -> {}, "modeWarningCollection", FullDocument.DEFAULT);

		Assertions.assertEquals(1, ignoredModeWarnings().size(), "the per-listener mode is ignored even when equal");
	}

	@SuppressWarnings("deprecation")
	@Test
	void testWatchModeIsWarned() {
		this.manager.watch("modeWarningCollection").fullDocument(FullDocument.UPDATE_LOOKUP).start();

		Assertions.assertEquals(1, ignoredModeWarnings().size());
	}

	@Test
	void testNoModeIsNotWarned() {
		this.manager.registerListener(event -> {}, "modeWarningCollection");
		this.manager.start(ConfigureDb.da.getInterface().getDatabase(), FullDocument.DEFAULT);

		Assertions.assertTrue(ignoredModeWarnings().isEmpty());
	}
}
