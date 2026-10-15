package test.atriasoft.archidata.cron;

import java.util.List;

import org.atriasoft.archidata.cron.CronScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

/**
 * {@code addTask} warns when an expression changed meaning with the move to the Unix (Vixie) cron syntax.
 */
class TestCronLegacyWarning {
	private final Logger schedulerLogger = (Logger) LoggerFactory.getLogger(CronScheduler.class);
	private final ListAppender<ILoggingEvent> appender = new ListAppender<>();

	@BeforeEach
	void setUp() {
		this.appender.start();
		this.schedulerLogger.addAppender(this.appender);
	}

	@AfterEach
	void tearDown() {
		this.schedulerLogger.detachAppender(this.appender);
		this.appender.stop();
	}

	private List<String> warningsFor(final String expression) {
		this.appender.list.clear();
		new CronScheduler().addTask("task", expression, () -> {});
		return this.appender.list.stream().filter(event -> event.getLevel() == Level.WARN)
				.map(ILoggingEvent::getFormattedMessage).toList();
	}

	private void assertWarns(final String expression, final String... fragments) {
		final List<String> warnings = warningsFor(expression);
		Assertions.assertEquals(1, warnings.size(), "'" + expression + "' must give one warning: " + warnings);
		for (final String fragment : fragments) {
			Assertions.assertTrue(warnings.get(0).contains(fragment), warnings.get(0) + " must contain " + fragment);
		}
	}

	private void assertSilent(final String expression) {
		Assertions.assertEquals(List.of(), warningsFor(expression), "'" + expression + "' kept its meaning");
	}

	@Test
	void testUnchangedExpressionsAreSilent() {
		assertSilent("0 0 * * *");
		assertSilent("*/10 * * * *");
		assertSilent("30 3 * * *");
		assertSilent("0 */6 * * *");
		assertSilent("0 0 1 * *");
		assertSilent("0 0 * * 1-5");
		assertSilent("0 0 * * */1");
		assertSilent("0 0 * * */7");
	}

	@Test
	void testStepOnDayOfMonthWarns() {
		assertWarns("0 0 */2 * *", "day of month '*/2' was {2,4,", "now {1,3,");
	}

	@Test
	void testStepOnMonthWarns() {
		assertWarns("0 0 1 */3 *", "month '*/3' was {3,6,9,12}, now {1,4,7,10}");
	}

	@Test
	void testStepOnDayOfWeekWarns() {
		assertWarns("0 0 * * */2", "day of week '*/2' was {Tue,Thu,Sat}, now {Tue,Thu,Sat,Sun}");
	}

	@Test
	void testSundayAsZeroWarns() {
		assertWarns("0 0 * * 0", "day of week '0' was never, now {Sun}");
	}

	@Test
	void testBothDaysRestrictedWarns() {
		assertWarns("0 0 13 * 5", "(AND)", "(OR)");
	}
}
