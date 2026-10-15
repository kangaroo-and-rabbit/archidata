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
 * A per-listener FullDocument mode is ignored: a warning must be logged even when the listener is
 * registered before {@code start()} (the effective mode is only known at start).
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
	void testModeRegisteredBeforeStartIsWarnedAtStart() {
		this.manager.registerListener(event -> {}, "modeWarningCollection", FullDocument.UPDATE_LOOKUP);
		Assertions.assertTrue(ignoredModeWarnings().isEmpty(), "the effective mode is not known before start()");

		this.manager.start(ConfigureDb.da.getInterface().getDatabase(), FullDocument.DEFAULT);

		final List<ILoggingEvent> warnings = ignoredModeWarnings();
		Assertions.assertEquals(1, warnings.size(), "start() must warn about the mode requested before it");
		Assertions.assertTrue(warnings.get(0).getFormattedMessage().contains("UPDATE_LOOKUP"));
		Assertions.assertTrue(warnings.get(0).getFormattedMessage().contains("DEFAULT"));
	}

	@SuppressWarnings("deprecation")
	@Test
	void testSameModeRegisteredBeforeStartIsNotWarned() {
		this.manager.registerListener(event -> {}, "modeWarningCollection", FullDocument.DEFAULT);

		this.manager.start(ConfigureDb.da.getInterface().getDatabase(), FullDocument.DEFAULT);

		Assertions.assertTrue(ignoredModeWarnings().isEmpty(), "the requested mode is the effective one");
	}

	@SuppressWarnings("deprecation")
	@Test
	void testModeRegisteredAfterStartIsWarnedAtOnce() {
		this.manager.start(ConfigureDb.da.getInterface().getDatabase(), FullDocument.DEFAULT);

		this.manager.registerListener(event -> {}, "modeWarningCollection", FullDocument.UPDATE_LOOKUP);

		Assertions.assertEquals(1, ignoredModeWarnings().size());
	}
}
