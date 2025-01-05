package org.kar.archidata;

import org.kar.archidata.tools.ConfigBaseVariable;
import org.kar.archidata.tools.JWTWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UpdateJwtPublicKey extends Thread {
	private static final Logger LOGGER = LoggerFactory.getLogger(UpdateJwtPublicKey.class);
	boolean kill = false;

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
				e1.printStackTrace();
				LOGGER.error("Can not retreive the basic tocken");
				return;
			}
			try {
				// update every 5 minutes the master token
				Thread.sleep(1000 * 60 * 5, 0);
			} catch (final InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void kill() {
		this.kill = true;
	}
}
