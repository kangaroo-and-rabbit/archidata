package org.kar.archidata;

import org.kar.archidata.util.ConfigBaseVariable;
import org.kar.archidata.util.JWTWrapper;

public class UpdateJwtPublicKey extends Thread {
	boolean kill = false;
	public void run() {
		try {
			Thread.sleep(1000*20, 0);
		} catch (InterruptedException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
    	while (this.kill == false) {
    		// need to uppgrade when server call us...
			try {
				JWTWrapper.initLocalTokenRemote(ConfigBaseVariable.getSSOAddress(), "archidata");
			} catch (Exception e1) {
				e1.printStackTrace();
				System.out.println("Can not retreive the basic tocken");
				return;
			}
			try {
				Thread.sleep(1000*60*5, 0);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
    	}
	}
	public void kill() {
		this.kill = true;
	}
}