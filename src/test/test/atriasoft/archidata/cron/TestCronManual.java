package test.atriasoft.archidata.cron;

import java.time.LocalDateTime;

import org.atriasoft.archidata.cron.CronScheduler;

public class TestCronManual {

	// Example usage
	public static void main(String[] args) throws InterruptedException {
		CronScheduler scheduler = new CronScheduler();
		scheduler.setGracePeriodMinutes(2);
		// Every 5 minutes, allow duplicates in queue
		scheduler.addTask("every5min", "*/5 * * * *",
				() -> System.out.println("Task every 5 min: " + LocalDateTime.now()), false);

		// Every minute at second check, unique in queue
		scheduler.addTask("uniqueTask", "* * * * *", () -> {
			System.out.println("Unique task executed: " + LocalDateTime.now());
			try {
				Thread.sleep(10000);
			} catch (InterruptedException ignored) {}
		}, true);

		String[][] testCrons = { //
				{ "everyMinute", "** * * *" }, //
				{ "every5Min", "5/88 * * * *" }, //
				{ "hourlyAt15", "1514 * * * *" }, //
				{ "dailyAt15h30", "3015 * * *" }, //
				{ "monday0830", "30 8 * A 1" }, //
				{ "weekend", "* * * * 6/,7" }, //
				{ "rangeHour", "* 9-1 * * *" }, //
				{ "rangeDayMonth", "0 0 1-88 * *" }, //
				{ "listMinutes", "0,15,30,45 ** * *" }, //
				{ "complex", "0-10,20-25 */21,15 */* 1,5" } //
		};

		for (String[] cronData : testCrons) {
			String name = cronData[0];
			String expr = cronData[1];
			boolean detect = false;
			try {
				scheduler.addTask(name, expr, () -> {}, false);
			} catch (IllegalArgumentException ex) {
				detect = true;
			}
			if (!detect) {
				System.out.println("ERROR Fail To Detect error '" + name + "'");
			}
		}
		scheduler.start();

		Thread.sleep(1000 * 60 * 10); // Run for 10 minutes
		scheduler.stop();
	}
}
