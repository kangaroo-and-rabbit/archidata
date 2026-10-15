Cron scheduler
==============

`CronScheduler` (`org.atriasoft.archidata.cron`) runs background tasks inside
the server JVM: recurring tasks described by a five-field cron expression,
one-time tasks at a given date, tasks to run "now", and tasks woken on demand
by an event. It has no dependency (no Quartz): one producer thread decides what
is due, one consumer thread runs it.

This page covers the API, the supported cron syntax (smaller than Unix cron),
the threading model, the wake-up debounce and the listener hook.


When to use it
--------------

Use it for housekeeping and periodic jobs of one server process: nightly
backups (see [backup.md](backup.md)), cleanup of soft-deleted rows
(`CleanupTools`, see [tools.md](tools.md)), the periodic flush of the
[query statistics](statistics.md), polling jobs.

Do not use it when a task must run exactly once across several server
instances: every instance runs its own scheduler and nothing coordinates them.
It does not persist anything: the one-time tasks are lost at restart and a task
missed while the server was down is not caught up.


Minimal example
---------------

```java
final CronScheduler scheduler = new CronScheduler();
scheduler.setGracePeriodMinutes(1);                       // let the server finish starting
scheduler.addTask("backup", "0 0 * * *", new BackupJob());   // every day at 00:00
scheduler.addTask("reclaim", "*/10 * * * *", reclaimJob);    // every 10 minutes
scheduler.start();
// at shutdown
scheduler.stop();
```

This is what karideo and karusic do in their `WebLauncher`.


Cron expressions
----------------

Exactly five fields separated by **one space**: `minute hour dayOfMonth month dayOfWeek`.

| Field | Range |
|---|---|
| minute | 0-59 |
| hour | 0-23 |
| day of month | 1-31 |
| month | 1-12 |
| day of week | 1-7, **1 = Monday, 7 = Sunday** |

Each field accepts `*`, a number, `*/n`, a range `a-b`, or a comma-separated
list of those (`1-5,10,20-22`). Not supported: names (`MON`, `JAN`), `?`, `L`,
`@daily`, steps on ranges (`1-10/2`), a seconds field, `0` or `7` for Sunday
as in Unix cron (here `0` is rejected).

`addTask` validates the expression and throws `IllegalArgumentException` (the
`NumberFormatException` of a bad number is one too) with the offending field.

Matching rules that differ from Unix cron:

- `*/n` matches when `value % n == 0`. For minutes and hours that is what you
  expect (`*/15` is 0, 15, 30, 45). For day of month and month, which start at
  1, `*/2` matches 2, 4, 6... and `*/5` on the day matches 5, 10, 15... not
  1, 6, 11.
- The day of month **and** the day of week must both match. In Unix cron they
  are ORed when both are restricted. `0 0 1 * 1` is "midnight when the 1st is a
  Monday" here.
- Times are local (`LocalDateTime.now()`, JVM time zone), evaluated once per
  minute.


Adding and removing tasks
-------------------------

| Method | Effect |
|---|---|
| `addTask(String name, String cron, Runnable action, boolean uniqueInQueue)` | recurring task |
| `addTask(String name, String cron, Runnable action)` | same with `uniqueInQueue = true` |
| `addTask(String name, LocalDateTime executeAt, Runnable action)` | one-time task. `executeAt` may be at most one minute in the past, otherwise `IllegalArgumentException` |
| `addTask(Runnable action)` | one-time task, runs as soon as possible |
| `removeTask(String name)` | removes a **recurring** task only |

A null or empty `name` is replaced by a generated `ObjectId` string. Adding a
task with an existing name replaces it. Tasks can be added while the scheduler
is running. One-time tasks are removed from the schedule when they fire, and
adding the same name twice before it fires keeps only the last.

`uniqueInQueue = true` means: if the previous occurrence of this task is still
waiting in the queue (not running, waiting), the new one is dropped instead of
being queued behind it. Use it for periodic jobs that must not pile up.
`false` queues every occurrence.


Threading model
---------------

- `start()` creates the threads (idempotent while running; after `stop()` it
  creates new ones). `stop()` interrupts both threads, clears the queue and the
  pending wake-ups.
- The **producer** wakes every second (immediately when a one-time task is
  added), enqueues the one-time tasks that are due, and once per minute
  enqueues the recurring tasks whose expression matches.
- The **consumer** is a single thread that takes the queue and runs the actions
  one after the other. Tasks never run concurrently with each other, and a long
  task delays all the others. A task running longer than 2 minutes is logged at
  `ERROR` when it ends.
- A task that throws (even an `Error`) is logged, reported to the listener as
  a failure, and the consumer keeps going.
- Both executors use non-daemon threads: call `stop()` at shutdown, otherwise
  the JVM does not exit by itself.
- `setGracePeriodMinutes(Integer)`: the producer sleeps that long after
  `start()` before scheduling anything, one-time tasks included. `null`, 0 or a
  negative value disables it (`disableGracePeriodMinutes()` does the same).


Waking a task on demand
-----------------------

`triggerWakeup(String taskName)` runs a **recurring** task now, outside its
schedule, for example when a request just created work for it. It never runs the
action in the caller thread, and an unknown name is ignored.

Calls are debounced by a grace window (`setEventGraceMillis(long)`, default
15 000 ms, must be > 0; `getEventGraceMillis()`):

- first call outside the window and task not running: enqueued at once;
- more calls inside the window, or while the task runs: collapsed into a single
  trailing run fired when the window has elapsed (and the task has finished).

So 200 events per second cause at most one extra run per window. The
`uniqueInQueue` flag of the task is honoured.


Observing executions
--------------------

```java
scheduler.setExecutionListener(new CronExecutionListener() {
    public Object onStart(String taskName, CronTriggerType trigger) { return journal.open(taskName); }
    public void onEnd(Object handle, boolean success) { journal.close(handle, success); }
    @Override
    public void onEnd(Object handle, boolean success, Throwable failure) { journal.close(handle, failure); }
});
```

`onStart` returns an opaque handle given back to `onEnd`. The three-argument
`onEnd` is a default method that delegates to the two-argument one; override it
to record the failure. Listener exceptions are caught and logged. The trigger is
a `CronTriggerType`: `SCHEDULED` (timetable or one-time task), `WAKEUP`
(`triggerWakeup`), `RERUN` (the coalesced trailing run).


Reference
---------

| Class | Visibility | Role |
|---|---|---|
| `CronScheduler` | public | the scheduler: `start`, `stop`, `addTask` (4 overloads), `removeTask`, `triggerWakeup`, `setGracePeriodMinutes`, `disableGracePeriodMinutes`, `setEventGraceMillis`, `getEventGraceMillis`, `setExecutionListener` |
| `CronExecutionListener` | public interface | `onStart`, `onEnd` (2 and 3 arguments) |
| `CronTriggerType` | public enum | `SCHEDULED`, `WAKEUP`, `RERUN` |
| `ScheduledTask` | public record | `(String name, LocalDateTime executeAt, Runnable action)`, equality by name |
| `CronTask`, `Task` | package-private | internal representations |


Pitfalls
--------

- Day of week numbering: Sunday is `7`. `0 3 * * 0` throws.
- The scheduler is per-process. With two server instances behind a load
  balancer every job runs twice.
- A single consumer thread: a blocking task (network call without timeout)
  freezes every other job. Give long jobs their own executor and let the cron
  action only submit work.
- The cron clock uses the JVM time zone and is evaluated on minute changes:
  a clock jump or a suspended JVM skips the minutes in between, with no
  catch-up.
- `removeTask` does not cancel a one-time task, nor a task already queued.
- `triggerWakeup` ignores one-time tasks and unknown names without any error.
- `QueryStatistics` creates its own `CronScheduler` when
  `QUERY_STATISTICS_FILE` is set, whose threads you cannot stop (only
  `QueryStatistics.reset()`, meant for tests, does); it flushes at JVM shutdown
  through a shutdown hook.
