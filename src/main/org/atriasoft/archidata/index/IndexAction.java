package org.atriasoft.archidata.index;

/**
 * One elementary operation of an index synchronization.
 *
 * @param collectionName the collection the action applies to
 * @param kind what must be done
 * @param indexName the name of the index
 * @param spec the wanted specification, {@code null} for a {@link Kind#DROP}
 * @param detail a human readable reason, quoted in the logs and the plan
 */
public record IndexAction(
		String collectionName,
		IndexAction.Kind kind,
		String indexName,
		IndexSpec spec,
		String detail) {

	/** What an action does to an index. */
	public enum Kind {
		/** The index is missing and must be created. */
		CREATE,
		/** The index exists with a different definition: dropped, then created again. */
		REPLACE,
		/** The index is not declared any more and must be removed. */
		DROP,
		/** The index already matches its declaration: nothing to do. */
		KEEP,
		/** The index is not managed by the code and is deliberately left alone. */
		PRESERVE,
	}

	/**
	 * Tells whether the action modifies the database.
	 *
	 * @return {@code true} for {@link Kind#CREATE}, {@link Kind#REPLACE} and {@link Kind#DROP}
	 */
	public boolean isChange() {
		return this.kind == Kind.CREATE || this.kind == Kind.REPLACE || this.kind == Kind.DROP;
	}

	@Override
	public String toString() {
		return this.kind + " " + this.collectionName + "." + this.indexName
				+ (this.detail == null || this.detail.isEmpty() ? "" : " (" + this.detail + ")");
	}
}
