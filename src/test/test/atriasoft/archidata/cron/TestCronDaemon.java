package test.atriasoft.archidata.cron;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.atriasoft.archidata.cron.CronScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/**
 * Tests the daemon flag of the {@link CronScheduler} threads. Each test waits on the result of an immediate
 * task (no fixed sleep).
 */
public class TestCronDaemon {

	private CronScheduler scheduler;

	@AfterEach
	public void tearDown() {
		if (this.scheduler != null) {
			this.scheduler.stop();
			this.scheduler = null;
		}
	}

	private boolean runsOnDaemonThread(final CronScheduler local) throws Exception {
		final CompletableFuture<Boolean> daemon = new CompletableFuture<>();
		local.addTask(() -> daemon.complete(Thread.currentThread().isDaemon()));
		return daemon.get(30, TimeUnit.SECONDS);
	}

	@Test
	@Timeout(60)
	public void threadsAreNotDaemonByDefault() throws Exception {
		this.scheduler = new CronScheduler();
		Assertions.assertFalse(this.scheduler.isDaemonThreads());
		this.scheduler.start();
		Assertions.assertFalse(runsOnDaemonThread(this.scheduler));
	}

	@Test
	@Timeout(60)
	public void daemonThreadsWhenRequested() throws Exception {
		this.scheduler = new CronScheduler();
		this.scheduler.setDaemonThreads(true);
		Assertions.assertTrue(this.scheduler.isDaemonThreads());
		this.scheduler.start();
		Assertions.assertTrue(runsOnDaemonThread(this.scheduler));
	}
}
