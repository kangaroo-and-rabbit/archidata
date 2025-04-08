package org.atriasoft.archidata.dataAccess.options;

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
