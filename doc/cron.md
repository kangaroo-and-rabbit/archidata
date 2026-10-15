Cron scheduler
==============

`CronScheduler` (`org.atriasoft.archidata.cron`) runs background tasks inside
the server JVM: recurring tasks described by a five-field cron expression,
one-time tasks at a given date, tasks to run "now", and tasks woken on demand
by an event. It has no dependency (no Quartz): one producer thread decides what
is due, one consumer thread runs it.

This page covers the API, the cron syntax (Unix cron, without names nor
macros), the threading model, the wake-up debounce and the listener hook.


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

Five fields separated by whitespace: `minute hour dayOfMonth month dayOfWeek`.
The syntax and the matching rules are those of Unix (Vixie) cron.

| Field | Range |
|---|---|
| minute | 0-59 |
| hour | 0-23 |
| day of month | 1-31 |
| month | 1-12 |
| day of week | 0-7, **0 and 7 = Sunday**, 1 = Monday |

Each field is a comma-separated list (`1-5,10,20-22`) of:

| Form | Meaning |
|---|---|
| `*` | every value of the field |
| `n` | that value |
| `a-b` | from `a` to `b` included (`a <= b`) |
| `*/n` | every `n` values **from the minimum of the field**: `*/15` on the minute is 0, 15, 30, 45; `*/2` on the day of month is 1, 3, 5...; `*/2` on the month is January, March...; `*/2` on the day of week is Sunday, Tuesday, Thursday, Saturday |
| `a-b/n` | every `n` values from `a` to `b`: `1-10/3` is 1, 4, 7, 10 |

Not supported: names (`MON`, `JAN`), `?`, `L`, `W`, `#`, macros (`@daily`),
a seconds field, `a/n` without a range (rejected, as in Vixie cron).

Day of month and day of week, as in Unix cron: when **both** are restricted
(neither starts with `*`), a day is due if **either** matches. `0 0 1 * 1` is
"midnight on the 1st of the month and on every Monday". When one of them
starts with `*` (`*` or `*/n`), both must match: `0 0 */2 * 1` is "midnight on
the Mondays that fall on an odd day".

`addTask` validates the expression and throws `IllegalArgumentException` with
the offending field. `CronScheduler.matches(String expression, LocalDateTime
time)` tells whether an expression is due at a given minute, with the same
rules (useful to check an expression in a test).

Before this syntax, archidata read `*/n` as "the value is a multiple of `n`",
numbered the days of week Monday = 1 .. Sunday = 7 (0 never matched) and always
combined the two day fields with AND. `MIGRATION.md` lists the expressions whose
meaning changed.

Times are local (`LocalDateTime.now()`, JVM time zone), evaluated once per
minute.


Adding and removing tasks
-------------------------

| Method | Effect |
|---|---|
| `addTask(String name, String cron, Runnable action, boolean uniqueInQueue)` | recurring task |
| `addTask(String name, String cron, Runnable action)` | same with `uniqueInQueue = true` |
| `addTask(String name, LocalDateTime executeAt, Runnable action)` | one-time task. `executeAt` may be at most one minute in the past, otherwise `IllegalArgumentException` |
| `addTask(Runnable action)` | one-time task, runs as soon as possible |
| `removeTask(String name)` | removes the recurring task and the pending one-time task of that name |
| `hasTask(String name)` | `true` if a recurring task or a not-yet-fired one-time task has this name |

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
- By default the threads are not daemon threads: call `stop()` at shutdown,
  otherwise the JVM does not exit by itself. `setDaemonThreads(true)` before
  `start()` creates daemon threads instead (`isDaemonThreads()` reads the flag),
  for a scheduler nobody stops explicitly.
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
| `CronScheduler` | public | the scheduler: `start`, `stop`, `addTask` (4 overloads), `removeTask`, `hasTask`, static `matches`, `setDaemonThreads`, `isDaemonThreads`, `triggerWakeup`, `setGracePeriodMinutes`, `disableGracePeriodMinutes`, `setEventGraceMillis`, `getEventGraceMillis`, `setExecutionListener` |
| `CronExecutionListener` | public interface | `onStart`, `onEnd` (2 and 3 arguments) |
| `CronTriggerType` | public enum | `SCHEDULED`, `WAKEUP`, `RERUN` |
| `ScheduledTask` | public record | `(String name, LocalDateTime executeAt, Runnable action)`, equality by name |
| `CronTask`, `Task` | package-private | internal representations |


Pitfalls
--------

- The scheduler is per-process. With two server instances behind a load
  balancer every job runs twice.
- A single consumer thread: a blocking task (network call without timeout)
  freezes every other job. Give long jobs their own executor and let the cron
  action only submit work.
- The cron clock uses the JVM time zone and is evaluated on minute changes:
  a clock jump or a suspended JVM skips the minutes in between, with no
  catch-up.
- `removeTask` does not cancel an occurrence already waiting in the queue.
- `triggerWakeup` ignores one-time tasks and unknown names without any error.
- `QueryStatistics` creates its own `CronScheduler` when
  `QUERY_STATISTICS_FILE` is set. Its threads are daemon threads, so it does not
  prevent the JVM from exiting; it flushes at JVM shutdown through a shutdown
  hook.
