package org.atriasoft.archidata.dataAccess.options;

public class Limit extends QueryOption {
	protected final long limit;

	public Limit(final long limit) {
		this.limit = limit;
	}

	public long getValue() {
		return this.limit;
	}
}
