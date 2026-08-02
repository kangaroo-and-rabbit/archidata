package org.atriasoft.archidata.index;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.atriasoft.archidata.dataAccess.FieldRef;
import org.atriasoft.archidata.dataAccess.MethodReferenceResolver;
import org.atriasoft.archidata.dataAccess.SerializableFunction;
import org.atriasoft.archidata.exception.DataAccessException;
import org.bson.Document;
import org.bson.json.JsonParseException;

/**
 * An index declaration, whatever it was declared with: {@link org.atriasoft.archidata.annotation.Index},
 * {@link org.atriasoft.archidata.annotation.Indexed}, or {@link IndexRegistry#declare}.
 *
 * <p>Instances are immutable: every builder method returns a new specification.
 *
 * <pre>{@code
 * IndexSpec.asc(User::getCompanyId).thenDesc(User::getCreatedAt)
 * IndexSpec.asc("email").unique()
 * IndexSpec.asc("sessionExpireAt").expireAfterSeconds(3600)
 * }</pre>
 *
 * <p>The static {@code asc}/{@code desc} open a specification, the instance {@code thenAsc}/
 * {@code thenDesc} append the following keys — Java forbids a static and an instance method with
 * the same signature, and the distinct names also make the key order obvious at the call site.
 */
public final class IndexSpec {

	/** Prefix of every code-managed index name: what tells the engine it may drop it. */
	public static final String MANAGED_PREFIX = "kar_";
	/** Maximum length of a generated name before it is truncated and hashed. */
	private static final int NAME_MAX_LENGTH = 110;
	/** Length kept from the readable part when a name is truncated. */
	private static final int NAME_TRUNCATE_AT = 60;

	private final List<IndexKey> keys;
	private final String explicitName;
	private final boolean unique;
	private final boolean sparse;
	private final long expireAfterSeconds;
	private final String partialFilter;

	private IndexSpec(final List<IndexKey> keys, final String explicitName, final boolean unique, final boolean sparse,
			final long expireAfterSeconds, final String partialFilter) {
		this.keys = List.copyOf(keys);
		this.explicitName = explicitName;
		this.unique = unique;
		this.sparse = sparse;
		this.expireAfterSeconds = expireAfterSeconds;
		this.partialFilter = partialFilter == null ? "" : partialFilter;
	}

	// ========== Opening a specification ==========

	/**
	 * Start a specification with an ascending key.
	 * @param path the structural field name, or a dotted path into an embedded sub-document
	 * @return the new specification
	 */
	public static IndexSpec asc(final String path) {
		return new IndexSpec(List.of(new IndexKey(path, true)), null, false, false, -1, "");
	}

	/**
	 * Start a specification with a descending key.
	 * @param path the structural field name, or a dotted path into an embedded sub-document
	 * @return the new specification
	 */
	public static IndexSpec desc(final String path) {
		return new IndexSpec(List.of(new IndexKey(path, false)), null, false, false, -1, "");
	}

	/**
	 * Start a specification with an ascending key, from a getter reference.
	 * @param <T> the entity type
	 * @param <R> the property type
	 * @param getter the getter of the indexed property
	 * @return the new specification
	 */
	public static <T, R> IndexSpec asc(final SerializableFunction<T, R> getter) {
		return asc(MethodReferenceResolver.resolveFieldName(getter));
	}

	/**
	 * Start a specification with a descending key, from a getter reference.
	 * @param <T> the entity type
	 * @param <R> the property type
	 * @param getter the getter of the indexed property
	 * @return the new specification
	 */
	public static <T, R> IndexSpec desc(final SerializableFunction<T, R> getter) {
		return desc(MethodReferenceResolver.resolveFieldName(getter));
	}

	/**
	 * Start a specification with an ascending key, from a field reference (getter or setter).
	 * @param <T> the entity type
	 * @param ref the reference of the indexed property
	 * @return the new specification
	 */
	public static <T> IndexSpec asc(final FieldRef<T> ref) {
		return asc(ref.getFieldName());
	}

	/**
	 * Start a specification with a descending key, from a field reference (getter or setter).
	 * @param <T> the entity type
	 * @param ref the reference of the indexed property
	 * @return the new specification
	 */
	public static <T> IndexSpec desc(final FieldRef<T> ref) {
		return desc(ref.getFieldName());
	}

	// ========== Appending keys ==========

	/**
	 * Append an ascending key.
	 * @param path the structural field name, or a dotted path
	 * @return a new specification with the extra key
	 */
	public IndexSpec thenAsc(final String path) {
		return withKey(new IndexKey(path, true));
	}

	/**
	 * Append a descending key.
	 * @param path the structural field name, or a dotted path
	 * @return a new specification with the extra key
	 */
	public IndexSpec thenDesc(final String path) {
		return withKey(new IndexKey(path, false));
	}

	/**
	 * Append an ascending key, from a getter reference.
	 * @param <T> the entity type
	 * @param <R> the property type
	 * @param getter the getter of the indexed property
	 * @return a new specification with the extra key
	 */
	public <T, R> IndexSpec thenAsc(final SerializableFunction<T, R> getter) {
		return thenAsc(MethodReferenceResolver.resolveFieldName(getter));
	}

	/**
	 * Append a descending key, from a getter reference.
	 * @param <T> the entity type
	 * @param <R> the property type
	 * @param getter the getter of the indexed property
	 * @return a new specification with the extra key
	 */
	public <T, R> IndexSpec thenDesc(final SerializableFunction<T, R> getter) {
		return thenDesc(MethodReferenceResolver.resolveFieldName(getter));
	}

	/**
	 * Append an ascending key, from a field reference.
	 * @param <T> the entity type
	 * @param ref the reference of the indexed property
	 * @return a new specification with the extra key
	 */
	public <T> IndexSpec thenAsc(final FieldRef<T> ref) {
		return thenAsc(ref.getFieldName());
	}

	/**
	 * Append a descending key, from a field reference.
	 * @param <T> the entity type
	 * @param ref the reference of the indexed property
	 * @return a new specification with the extra key
	 */
	public <T> IndexSpec thenDesc(final FieldRef<T> ref) {
		return thenDesc(ref.getFieldName());
	}

	private IndexSpec withKey(final IndexKey key) {
		final List<IndexKey> merged = new ArrayList<>(this.keys);
		merged.add(key);
		return new IndexSpec(merged, this.explicitName, this.unique, this.sparse, this.expireAfterSeconds,
				this.partialFilter);
	}

	// ========== Options ==========

	/**
	 * Make the index reject duplicated values.
	 * @return a new unique specification
	 */
	public IndexSpec unique() {
		return new IndexSpec(this.keys, this.explicitName, true, this.sparse, this.expireAfterSeconds,
				this.partialFilter);
	}

	/**
	 * Skip the documents that do not carry the indexed fields.
	 * @return a new sparse specification
	 */
	public IndexSpec sparse() {
		return new IndexSpec(this.keys, this.explicitName, this.unique, true, this.expireAfterSeconds,
				this.partialFilter);
	}

	/**
	 * Turn the index into a TTL index: MongoDB removes a document this many seconds after the
	 * indexed date. Requires a single date key.
	 * @param seconds the expiration delay in seconds
	 * @return a new specification with the TTL
	 */
	public IndexSpec expireAfterSeconds(final long seconds) {
		return new IndexSpec(this.keys, this.explicitName, this.unique, this.sparse, seconds, this.partialFilter);
	}

	/**
	 * Restrict the index to the documents matching a filter, given as a JSON document.
	 * @param filter the partial filter expression
	 * @return a new partial specification
	 */
	public IndexSpec partialFilter(final String filter) {
		return new IndexSpec(this.keys, this.explicitName, this.unique, this.sparse, this.expireAfterSeconds, filter);
	}

	/**
	 * Force the index name. It is always prefixed by {@value #MANAGED_PREFIX}: an index without
	 * that prefix is considered foreign and would be dropped by the synchronization.
	 * @param name the wanted name
	 * @return a new named specification
	 */
	public IndexSpec name(final String name) {
		return new IndexSpec(this.keys, name, this.unique, this.sparse, this.expireAfterSeconds, this.partialFilter);
	}

	// ========== Accessors ==========

	/**
	 * Ordered keys of the index.
	 * @return the index keys
	 */
	public List<IndexKey> getKeys() {
		return this.keys;
	}

	/**
	 * Tells whether the index rejects duplicated values.
	 * @return {@code true} for a unique index
	 */
	public boolean isUnique() {
		return this.unique;
	}

	/**
	 * Tells whether the index skips the documents without the indexed fields.
	 * @return {@code true} for a sparse index
	 */
	public boolean isSparse() {
		return this.sparse;
	}

	/**
	 * TTL of the index, in seconds.
	 * @return the expiration delay, or {@code -1} when the index has no TTL
	 */
	public long getExpireAfterSeconds() {
		return this.expireAfterSeconds;
	}

	/**
	 * Partial filter of the index.
	 * @return the filter as a JSON document, empty when the index covers the whole collection
	 */
	public String getPartialFilter() {
		return this.partialFilter;
	}

	/**
	 * Tells whether the index carries an option beyond its keys.
	 * @return {@code true} if any option differs from its default
	 */
	public boolean hasOptions() {
		return this.unique || this.sparse || this.expireAfterSeconds >= 0 || !this.partialFilter.isEmpty();
	}

	/**
	 * MongoDB key document of the index, ready for {@code createIndex}.
	 * @return the keys as {@code {field: 1, other: -1}}
	 */
	public Document toKeysDocument() {
		final Document out = new Document();
		for (final IndexKey key : this.keys) {
			out.append(key.path(), key.order());
		}
		return out;
	}

	/**
	 * Name of the index: the explicit one, or a canonical name built from the keys. Always
	 * prefixed by {@value #MANAGED_PREFIX}.
	 *
	 * <p>The name embeds a short hash of the whole definition as soon as it cannot be represented
	 * literally (options set, dotted path, name too long). Two specifications that differ
	 * therefore never share a name, and a modified declaration produces a new name — which is what
	 * makes the synchronization replace the index instead of leaving a stale one behind.
	 *
	 * @return the index name
	 */
	public String getName() {
		if (this.explicitName != null && !this.explicitName.isBlank()) {
			final String trimmed = this.explicitName.trim();
			return trimmed.startsWith(MANAGED_PREFIX) ? trimmed : MANAGED_PREFIX + trimmed;
		}
		final StringBuilder readable = new StringBuilder();
		boolean dotted = false;
		for (final IndexKey key : this.keys) {
			if (readable.length() != 0) {
				readable.append('_');
			}
			if (key.path().indexOf('.') >= 0) {
				dotted = true;
			}
			readable.append(key.path().replace('.', '_')).append('_').append(key.order());
		}
		final String base = MANAGED_PREFIX + readable;
		if (!dotted && !hasOptions() && base.length() <= NAME_MAX_LENGTH) {
			return base;
		}
		// A dotted path is flattened in the name, so two distinct paths could collide: the hash of
		// the real definition is what keeps the names unique.
		final String prefix = base.length() > NAME_TRUNCATE_AT ? base.substring(0, NAME_TRUNCATE_AT) : base;
		return prefix + "_" + shortHash(definitionSignature());
	}

	/**
	 * Canonical text of the whole definition, used for hashing and comparing.
	 * @return the signature of the specification
	 */
	public String definitionSignature() {
		final StringBuilder out = new StringBuilder();
		for (final IndexKey key : this.keys) {
			out.append(key.path()).append(':').append(key.order()).append('|');
		}
		out.append("unique=").append(this.unique)//
				.append("|sparse=").append(this.sparse)//
				.append("|ttl=").append(this.expireAfterSeconds)//
				.append("|partial=").append(this.partialFilter);
		return out.toString();
	}

	/**
	 * Identity of the indexed data: the keys and the partial filter. Two specifications sharing it
	 * describe the same index and must not disagree on the other options.
	 * @return the target signature of the specification
	 */
	public String targetSignature() {
		final StringBuilder out = new StringBuilder();
		for (final IndexKey key : this.keys) {
			out.append(key.path()).append(':').append(key.order()).append('|');
		}
		return out.append("partial=").append(this.partialFilter).toString();
	}

	private static String shortHash(final String value) {
		try {
			final MessageDigest digest = MessageDigest.getInstance("SHA-256");
			final byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
			final StringBuilder out = new StringBuilder(8);
			for (int iii = 0; iii < 4; iii++) {
				out.append(String.format("%02x", hash[iii]));
			}
			return out.toString();
		} catch (final NoSuchAlgorithmException ex) {
			// SHA-256 is mandated by the platform: unreachable.
			throw new IllegalStateException("SHA-256 is not available", ex);
		}
	}

	/**
	 * Check what can be checked without the entity model: keys present and distinct, TTL on a
	 * single key, parsable partial filter.
	 *
	 * @param origin human readable origin of the declaration, quoted in the error messages
	 * @throws DataAccessException if the specification cannot describe a MongoDB index
	 */
	public void validate(final String origin) throws DataAccessException {
		if (this.keys.isEmpty()) {
			throw new DataAccessException(origin + ": an index must have at least one key");
		}
		final Set<String> seen = new HashSet<>();
		for (final IndexKey key : this.keys) {
			if (!seen.add(key.path())) {
				throw new DataAccessException(
						origin + ": the field '" + key.path() + "' appears twice in the same index");
			}
		}
		if (this.expireAfterSeconds >= 0 && this.keys.size() != 1) {
			throw new DataAccessException(origin + ": a TTL index requires a single date key, got " + this.keys.size());
		}
		if (this.expireAfterSeconds < -1) {
			throw new DataAccessException(origin + ": expireAfterSeconds must be >= 0, or -1 to disable the TTL");
		}
		if (!this.partialFilter.isEmpty()) {
			try {
				Document.parse(this.partialFilter);
			} catch (final JsonParseException ex) {
				throw new DataAccessException(
						origin + ": the partial filter is not a valid JSON document: " + ex.getMessage());
			}
		}
	}

	@Override
	public boolean equals(final Object other) {
		if (this == other) {
			return true;
		}
		if (!(other instanceof final IndexSpec casted)) {
			return false;
		}
		return this.unique == casted.unique //
				&& this.sparse == casted.sparse //
				&& this.expireAfterSeconds == casted.expireAfterSeconds //
				&& this.keys.equals(casted.keys) //
				&& this.partialFilter.equals(casted.partialFilter) //
				&& Objects.equals(getName(), casted.getName());
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.keys, this.unique, this.sparse, this.expireAfterSeconds, this.partialFilter,
				getName());
	}

	@Override
	public String toString() {
		return getName() + " " + toKeysDocument().toJson() + (hasOptions() ? " [" + definitionSignature() + "]" : "");
	}
}
