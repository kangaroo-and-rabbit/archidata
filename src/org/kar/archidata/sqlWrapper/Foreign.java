package org.kar.archidata.sqlWrapper;

// Mark as deprecated while the concept is not ready ...
@Deprecated
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
