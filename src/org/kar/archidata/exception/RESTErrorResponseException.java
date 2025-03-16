package org.kar.archidata.exception;

import java.util.List;

import org.bson.types.ObjectId;
import org.kar.archidata.catcher.RestInputError;

public class RESTErrorResponseException extends Exception {
	private static final long serialVersionUID = 1L;
	public ObjectId oid;
	public String time;
	public String name;
	public String message;
	public int status;
	public String statusMessage;
	public List<RestInputError> inputError;

	public RESTErrorResponseException() {
		this.oid = new ObjectId();
		this.time = null;
		this.name = null;
		this.message = null;
		this.status = 0;
		this.statusMessage = null;
		this.inputError = null;
	}

	public RESTErrorResponseException(final ObjectId oid, final String time, final String name, final String message,
			final int status, final String statusMessage, final List<RestInputError> inputError) {
		this.oid = oid;
		this.time = time;
		this.name = name;
		this.message = message;
		this.status = status;
		this.statusMessage = statusMessage;
		this.inputError = inputError;
	}

	@Override
	public String toString() {
		return "RESTErrorResponseExeption [oid=" + this.oid + ", time=" + this.time + ", name=" + this.name
				+ ", message=" + this.message + ", status=" + this.status + ", statusMessage=" + this.statusMessage
				+ "]";
	}

}
