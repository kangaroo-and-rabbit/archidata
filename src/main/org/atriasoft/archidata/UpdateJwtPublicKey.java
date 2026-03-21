package org.atriasoft.archidata;

import org.atriasoft.archidata.tools.ConfigBaseVariable;
import org.atriasoft.archidata.tools.JWTWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Daemon thread that periodically refreshes the JWT public key from a remote SSO server.
 *
 * <p>If no SSO address is configured, the thread exits immediately.</p>
 */
public class UpdateJwtPublicKey extends Thread {
	private static final Logger LOGGER = LoggerFactory.getLogger(UpdateJwtPublicKey.class);
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
			// No SO provided, kill the thread.
			return;
		}
		while (!this.kill) {
			// need to upgrade when server call us...
			try {
				JWTWrapper.initLocalTokenRemote(ConfigBaseVariable.getSSOAddress(), "archidata");
			} catch (final Exception e1) {
				LOGGER.error("Can not retreive the basic tocken: {}", e1.getMessage(), e1);
				return;
			}
			try {
				// update every 5 minutes the master token
				Thread.sleep(1000 * 60 * 5, 0);
			} catch (final InterruptedException e) {
				LOGGER.debug("UpdateJwtPublicKey interrupted, stopping.");
				Thread.currentThread().interrupt();
				break;
			}
		}
	}

	/** Signals this thread to stop and interrupts it. */
	public void kill() {
		this.kill = true;
		this.interrupt();
	}
}
