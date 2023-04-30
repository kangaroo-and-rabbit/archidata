package org.kar.archidata.exception;

import java.util.UUID;

public class RESTErrorResponseExeption extends Exception {
	public UUID uuid;
	public String time; 
	public String error;
	public String message;
	public int status;
	public String statusMessage;

	public RESTErrorResponseExeption() {
		super();
		this.uuid = null;
		this.time = null;
		this.error = null;
		this.message = null;
		this.status = 0;
		this.statusMessage = null;
	}
	public RESTErrorResponseExeption(UUID uuid, String time, String error, String message, int status,
			String statusMessage) {
		super();
		this.uuid = uuid;
		this.time = time;
		this.error = error;
		this.message = message;
		this.status = status;
		this.statusMessage = statusMessage;
	}

	@Override
	public String toString() {
		return "RESTErrorResponseExeption [uuid=" + uuid + ", time=" + time + ", error=" + error + ", message="
				+ message + ", status=" + status + ", statusMessage=" + statusMessage + "]";
	}
	
	

}
