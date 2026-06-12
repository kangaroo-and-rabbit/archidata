package test.atriasoft.archidata.dataAccess;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.atriasoft.archidata.dataAccess.model.DbClassModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import test.atriasoft.archidata.dataAccess.model.TypeManyToManyDocLongRemote;
import test.atriasoft.archidata.dataAccess.model.TypeManyToManyDocLongRoot;

/**
 * Regression test: two threads building the {@link DbClassModel} of the two
 * ends of a bidirectional {@code @ManyToManyDoc} cycle
 * ({@code TypeManyToManyDocLongRoot ↔ TypeManyToManyDocLongRemote}) at the
 * same time.
 *
 * <p>
 * The addon-context build phase resolves the target entity of each addon field
 * through {@code DbClassModel.of(target)}. With one monitor per model, the two
 * threads enter the cycle from opposite ends, acquire the two monitors in
 * opposite order and deadlock forever (ABBA); every later {@code of()} call on
 * either class then blocks un-interruptibly. The build phase must therefore be
 * serialized on a single global lock.
 * </p>
 */
public class TestDbClassModelConcurrentCycle {

	/**
	 * Number of build races attempted. The deadlock window is the duration of one
	 * addon-context build, so a single attempt can miss it; repeating with a
	 * barrier-aligned start makes the race overwhelmingly probable on a buggy
	 * implementation while staying fast (< 1 s) on a correct one.
	 */
	private static final int RACE_ITERATIONS = 200;

	@BeforeAll
	public static void registerDefaultAddons() throws ClassNotFoundException {
		// The default add-ons (ManyToManyDoc, ...) are registered into DbClassModel
		// by the static initializer of DBAccessMongo: force-load it without a DB.
		Class.forName("org.atriasoft.archidata.dataAccess.DBAccessMongo");
	}

	@Test
	@Timeout(60)
	public void concurrentBuildOfBidirectionalCycleDoesNotDeadlock() throws Exception {
		final ExecutorService pool = Executors.newFixedThreadPool(2);
		try {
			for (int iteration = 0; iteration < RACE_ITERATIONS; iteration++) {
				DbClassModel.clearCache();
				final CyclicBarrier startBarrier = new CyclicBarrier(2);
				final Future<DbClassModel> rootSide = pool.submit(() -> {
					startBarrier.await();
					return DbClassModel.of(TypeManyToManyDocLongRoot.class);
				});
				final Future<DbClassModel> remoteSide = pool.submit(() -> {
					startBarrier.await();
					return DbClassModel.of(TypeManyToManyDocLongRemote.class);
				});
				// On a deadlock both futures hang: fail fast with a clear message
				// instead of letting the @Timeout reap the whole test.
				Assertions.assertNotNull(rootSide.get(20, TimeUnit.SECONDS));
				Assertions.assertNotNull(remoteSide.get(20, TimeUnit.SECONDS));
			}
		} finally {
			pool.shutdownNow();
		}
	}
}
