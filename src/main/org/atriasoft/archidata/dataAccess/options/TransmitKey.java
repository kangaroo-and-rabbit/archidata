package org.atriasoft.archidata.dataAccess.options;

/** Internal option set by {@code updateById}: it carries the primary key of the updated document, so that
 * {@code update} can read its stored state before applying the relation add-ons (ManyToOne, OneToMany,
 * ManyToMany fields). */
public class TransmitKey extends QueryOption {
	private final Object key;

	/**
	 * Constructs a TransmitKey with the specified key value.
	 *
	 * @param key the key to transmit during ManyToMany update operations
	 */
	public TransmitKey(final Object key) {
		this.key = key;
	}

	/**
	 * Returns the transmitted key.
	 *
	 * @return the key object
	 */
	public Object getKey() {
		return this.key;
	}
}
