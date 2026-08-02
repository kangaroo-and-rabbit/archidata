package org.atriasoft.archidata.index;

/**
 * One key of an index: the indexed field path and its sort order.
 *
 * @param path the structural field name, possibly a dotted path into an embedded sub-document
 * @param ascending {@code true} for an ascending key, {@code false} for a descending one
 */
public record IndexKey(
		String path,
		boolean ascending) {

	/**
	 * Validates the key.
	 *
	 * @throws IllegalArgumentException if the path is empty or malformed
	 */
	public IndexKey {
		if (path == null || path.isBlank()) {
			throw new IllegalArgumentException("An index key must have a field name");
		}
		if (path.startsWith(".") || path.endsWith(".") || path.contains("..")) {
			throw new IllegalArgumentException("Malformed index key path: '" + path + "'");
		}
	}

	/**
	 * Parses an annotation entry: a field name, optionally prefixed by {@code -} for a descending
	 * key ({@code "-createdAt"}).
	 *
	 * @param declaration the declared key
	 * @return the parsed key
	 */
	public static IndexKey parse(final String declaration) {
		if (declaration == null || declaration.isBlank()) {
			throw new IllegalArgumentException("An index key must have a field name");
		}
		final String trimmed = declaration.trim();
		if (trimmed.startsWith("-")) {
			return new IndexKey(trimmed.substring(1).trim(), false);
		}
		if (trimmed.startsWith("+")) {
			return new IndexKey(trimmed.substring(1).trim(), true);
		}
		return new IndexKey(trimmed, true);
	}

	/**
	 * MongoDB sort value of this key.
	 *
	 * @return {@code 1} for ascending, {@code -1} for descending
	 */
	public int order() {
		return this.ascending ? 1 : -1;
	}

	/**
	 * Top-level field name of the key: what stands before the first dot.
	 *
	 * @return the top-level field name
	 */
	public String topLevel() {
		final int dot = this.path.indexOf('.');
		return dot < 0 ? this.path : this.path.substring(0, dot);
	}

	@Override
	public String toString() {
		return this.path + ":" + order();
	}
}
