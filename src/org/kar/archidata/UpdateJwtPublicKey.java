package org.kar.archidata;

import org.kar.archidata.tools.ConfigBaseVariable;
import org.kar.archidata.tools.JWTWrapper;

public class UpdateJwtPublicKey extends Thread {
	boolean kill = false;

	@Override
	public void run() {
		if (ConfigBaseVariable.getSSOAddress() == null) {
			System.out.println("SSO INTERFACE is not provided ==> work alone.");
			// No SO provided, kill the thread.
			return;
		}
		while (!this.kill) {
			// need to upgrade when server call us...
			try {
				JWTWrapper.initLocalTokenRemote(ConfigBaseVariable.getSSOAddress(), "archidata");
			} catch (final Exception e1) {
				e1.printStackTrace();
				System.out.println("Can not retreive the basic tocken");
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
