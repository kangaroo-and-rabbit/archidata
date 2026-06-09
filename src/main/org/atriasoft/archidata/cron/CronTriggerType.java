package org.atriasoft.archidata.cron;

/**
 * What caused a cron task to run, reported to the {@link CronExecutionListener}.
 */
public enum CronTriggerType {
	/** Fired by the cron timetable (or a one-time scheduled task reaching its time). */
	SCHEDULED,
	/** Woken on demand by an external event via {@link CronScheduler#triggerWakeup(String)}. */
	WAKEUP,
	/** Re-run requested because a wake-up arrived while the task was already running (coalesced rerun). */
	RERUN,
}
