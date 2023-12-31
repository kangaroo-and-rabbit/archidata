package org.kar.archidata.dataAccess.options;

import org.kar.archidata.dataAccess.QueryOption;

/** Internal option that permit to transmit the Key when updating the ManyToMany values (first step). */
public class TransmitKey extends QueryOption {
	private final Object key;

	public TransmitKey(final Object key) {
		this.key = key;
	}

	public Object getKey() {
		return this.key;
	}
}
