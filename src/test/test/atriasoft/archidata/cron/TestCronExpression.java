package test.atriasoft.archidata.cron;

import java.time.LocalDateTime;

import org.atriasoft.archidata.cron.CronScheduler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests of the cron expression syntax, which follows Unix (Vixie) cron. Pure matching tests: no thread is
 * started, nothing depends on timing.
 *
 * <p>Reference dates: 2026-10-01 is a Thursday, 2026-10-04 a Sunday, 2026-10-05 a Monday.
 */
public class TestCronExpression {

	private static LocalDateTime at(final int month, final int day, final int hour, final int minute) {
		return LocalDateTime.of(2026, month, day, hour, minute);
	}

	private static void assertMatch(final String expr, final LocalDateTime time) {
		Assertions.assertTrue(CronScheduler.matches(expr, time), "'" + expr + "' should match " + time);
	}

	private static void assertNoMatch(final String expr, final LocalDateTime time) {
		Assertions.assertFalse(CronScheduler.matches(expr, time), "'" + expr + "' should not match " + time);
	}

	@Test
	public void stepOnMinuteStartsAtZero() {
		assertMatch("*/15 * * * *", at(10, 1, 3, 0));
		assertMatch("*/15 * * * *", at(10, 1, 3, 45));
		assertNoMatch("*/15 * * * *", at(10, 1, 3, 5));
		assertMatch("*/10 * * * *", at(10, 1, 3, 50));
	}

	@Test
	public void stepOnDayOfMonthStartsAtOne() {
		assertMatch("0 0 */2 * *", at(10, 1, 0, 0));
		assertMatch("0 0 */2 * *", at(10, 3, 0, 0));
		assertNoMatch("0 0 */2 * *", at(10, 2, 0, 0));
		assertMatch("0 0 */5 * *", at(10, 6, 0, 0));
		assertNoMatch("0 0 */5 * *", at(10, 5, 0, 0));
	}

	@Test
	public void stepOnMonthStartsAtJanuary() {
		assertMatch("0 0 1 */2 *", at(1, 1, 0, 0));
		assertMatch("0 0 1 */2 *", at(3, 1, 0, 0));
		assertNoMatch("0 0 1 */2 *", at(2, 1, 0, 0));
	}

	@Test
	public void stepOnRange() {
		assertMatch("0 0 1-10/3 * *", at(10, 1, 0, 0));
		assertMatch("0 0 1-10/3 * *", at(10, 4, 0, 0));
		assertMatch("0 0 1-10/3 * *", at(10, 10, 0, 0));
		assertNoMatch("0 0 1-10/3 * *", at(10, 2, 0, 0));
		assertNoMatch("0 0 1-10/3 * *", at(10, 13, 0, 0));
		assertMatch("0 9-17/4 * * *", at(10, 1, 13, 0));
		assertNoMatch("0 9-17/4 * * *", at(10, 1, 11, 0));
	}

	@Test
	public void sundayIsZeroOrSeven() {
		assertMatch("0 3 * * 0", at(10, 4, 3, 0));
		assertMatch("0 3 * * 7", at(10, 4, 3, 0));
		assertNoMatch("0 3 * * 0", at(10, 5, 3, 0));
		assertMatch("0 3 * * 5-7", at(10, 4, 3, 0));
		assertMatch("0 3 * * 0-1", at(10, 5, 3, 0));
		assertNoMatch("0 3 * * 1-5", at(10, 4, 3, 0));
		// */2 on the day of week covers 0,2,4,6: Sunday, Tuesday, Thursday, Saturday
		assertMatch("0 3 * * */2", at(10, 4, 3, 0));
		assertMatch("0 3 * * */2", at(10, 1, 3, 0));
		assertNoMatch("0 3 * * */2", at(10, 5, 3, 0));
	}

	@Test
	public void dayOfMonthAndDayOfWeekAreOredWhenBothRestricted() {
		// The 1st (a Thursday) or any Monday
		assertMatch("0 0 1 * 1", at(10, 1, 0, 0));
		assertMatch("0 0 1 * 1", at(10, 5, 0, 0));
		assertNoMatch("0 0 1 * 1", at(10, 6, 0, 0));
	}

	@Test
	public void dayOfMonthAndDayOfWeekAreAndedWhenOneStartsWithStar() {
		// Field starting with '*' counts as unrestricted (Vixie): odd days that are Mondays
		assertMatch("0 0 */2 * 1", at(10, 5, 0, 0));
		assertNoMatch("0 0 */2 * 1", at(10, 12, 0, 0));
		assertNoMatch("0 0 */2 * 1", at(10, 7, 0, 0));
		assertMatch("0 0 * * 1", at(10, 12, 0, 0));
		assertNoMatch("0 0 * * 1", at(10, 7, 0, 0));
	}

	@Test
	public void listsCombineAllForms() {
		final String expr = "0,30 1-3,22 * * *";
		assertMatch(expr, at(10, 1, 2, 30));
		assertMatch(expr, at(10, 1, 22, 0));
		assertNoMatch(expr, at(10, 1, 4, 0));
		assertNoMatch(expr, at(10, 1, 2, 15));
	}

	@Test
	public void invalidExpressionsAreRejected() {
		final CronScheduler scheduler = new CronScheduler();
		final String[] invalid = { "* * * *", "5/10 * * * *", "0 0 * * 8", "0 0 0 * *", "0 0 10-5 * *",
				"0 0 1-10/0 * *", "0 0 */0 * *", "60 * * * *", "* 24 * * *", "* * * 13 *", "a * * * *", "1- * * * *",
				"*/ * * * *", "0,,1 * * * *", "1-2-3 * * * *" };
		for (final String expr : invalid) {
			Assertions.assertThrows(IllegalArgumentException.class, () -> scheduler.addTask("bad", expr, () -> {}),
					"'" + expr + "' should be rejected");
		}
		scheduler.addTask("sunday0", "0 3 * * 0", () -> {});
		scheduler.addTask("rangeStep", "0 0 1-31/2 1-12/3 1-5", () -> {});
	}

	@Test
	public void stepLargerThanTheFieldIsRejected() {
		// a huge step used to overflow 'value += step' and throw ArrayIndexOutOfBoundsException
		for (final String expr : new String[] { "*/2147483647 * * * *", "0 0 1-31/2147483647 * *", "*/61 * * * *",
				"0 0 * * */9" }) {
			Assertions.assertThrows(IllegalArgumentException.class, () -> CronScheduler.matches(expr, at(10, 1, 0, 0)),
					"'" + expr + "' should be rejected");
		}
		assertMatch("*/60 * * * *", at(10, 1, 3, 0));
		assertNoMatch("*/60 * * * *", at(10, 1, 3, 1));
	}

	@Test
	public void removeTaskAlsoRemovesOneTimeTask() {
		final CronScheduler scheduler = new CronScheduler();
		scheduler.addTask("later", LocalDateTime.now().plusHours(1), () -> {});
		Assertions.assertTrue(scheduler.hasTask("later"));
		scheduler.removeTask("later");
		Assertions.assertFalse(scheduler.hasTask("later"));
		scheduler.addTask("cron", "0 0 * * *", () -> {});
		Assertions.assertTrue(scheduler.hasTask("cron"));
		scheduler.removeTask("cron");
		Assertions.assertFalse(scheduler.hasTask("cron"));
	}
}
