package org.atriasoft.archidata;

import org.atriasoft.archidata.tools.ConfigBaseVariable;
import org.atriasoft.archidata.tools.JWTWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Daemon thread that periodically refreshes the JWT public key from a remote SSO server.
 *
 * <p>If no SSO address is configured, the thread exits immediately.</p>
 *
 * <p>Behavior:</p>
 * <ul>
 *   <li>Retries every {@value #RETRY_INTERVAL_MS_FAILURE} ms while the key has never been fetched
 *       successfully (typical case: this server started before the SSO).</li>
 *   <li>Once a key is loaded, polls every {@value #RETRY_INTERVAL_MS_SUCCESS} ms as a safety net.</li>
 *   <li>Registers itself with {@link JWTWrapper} so a token validation that fails on signature
 *       can trigger an immediate refresh (out-of-band, no need to wait for the polling tick).</li>
 * </ul>
 */
public class UpdateJwtPublicKey extends Thread {
	private static final Logger LOGGER = LoggerFactory.getLogger(UpdateJwtPublicKey.class);
	private static final long RETRY_INTERVAL_MS_FAILURE = 30_000L;
	private static final long RETRY_INTERVAL_MS_SUCCESS = 5L * 60_000L;
	private volatile boolean kill = false;

	/** Creates a new daemon thread for JWT public key updates. */
	public UpdateJwtPublicKey() {
		setDaemon(true);
	}

	/** {@inheritDoc} */
	@Override
	public void run() {
		if (ConfigBaseVariable.getSSOAddress() == null) {
			LOGGER.warn("SSO INTERFACE is not provided ==> work alone.");
			// No SSO provided, kill the thread.
			return;
		}
		// Allow JWTWrapper.validateToken to trigger an immediate refresh on signature mismatch.
		JWTWrapper.setRefreshTrigger(this::requestImmediateRefresh);
		while (!this.kill) {
			final boolean success = tryFetchPublicKey();
			final long sleepMs = success ? RETRY_INTERVAL_MS_SUCCESS : RETRY_INTERVAL_MS_FAILURE;
			if (!success) {
				LOGGER.warn("JWT public key not loaded, retrying in {} ms", sleepMs);
			}
			sleepInterruptibly(sleepMs);
		}
	}

	private static boolean tryFetchPublicKey() {
		try {
			JWTWrapper.initLocalTokenRemote(ConfigBaseVariable.getSSOAddress(), "archidata");
			return JWTWrapper.getPublicKeyJson() != null;
		} catch (final Exception e) {
			LOGGER.error("Cannot retrieve the JWT public key: {}", e.getMessage(), e);
			return false;
		}
	}

	private void sleepInterruptibly(final long sleepMs) {
		try {
			Thread.sleep(sleepMs);
		} catch (final InterruptedException e) {
			if (this.kill) {
				LOGGER.debug("UpdateJwtPublicKey killed, stopping.");
				Thread.currentThread().interrupt();
			} else {
				// Interrupt was an explicit refresh request — loop again immediately.
				LOGGER.debug("UpdateJwtPublicKey received refresh request, fetching now.");
			}
		}
	}

	/**
	 * Triggers an immediate refresh of the JWT public key by interrupting this thread's sleep.
	 * Safe to call from any thread; idempotent if the worker is already awake.
	 */
	public void requestImmediateRefresh() {
		this.interrupt();
	}

	/** Signals this thread to stop and interrupts it. */
	public void kill() {
		this.kill = true;
		this.interrupt();
	}
}
