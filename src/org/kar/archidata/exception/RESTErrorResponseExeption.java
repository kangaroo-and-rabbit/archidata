package org.kar.archidata.exception;

import java.util.UUID;

public class RESTErrorResponseExeption extends Exception {
	public UUID uuid;
	public String time;
	public String name;
	public String message;
	public int status;
	public String statusMessage;

	public RESTErrorResponseExeption() {
		this.uuid = null;
		this.time = null;
		this.name = null;
		this.message = null;
		this.status = 0;
		this.statusMessage = null;
	}

	public RESTErrorResponseExeption(final UUID uuid, final String time, final String name, final String message,
			final int status, final String statusMessage) {
		this.uuid = uuid;
		this.time = time;
		this.name = name;
		this.message = message;
		this.status = status;
		this.statusMessage = statusMessage;
	}

	@Override
	public String toString() {
		return "RESTErrorResponseExeption [uuid=" + this.uuid + ", time=" + this.time + ", name=" + this.name
				+ ", message=" + this.message + ", status=" + this.status + ", statusMessage=" + this.statusMessage
				+ "]";
	}

}
