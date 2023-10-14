package org.kar.archidata.sqlWrapper;

public class Foreign<T> {
	public final Long id;
	public final T data;
	
	public Foreign(final Long id) {
		this.id = id;
		this.data = null;
	}
	
	public Foreign(final T data) {
		this.id = null;
		this.data = data;
	}
}
