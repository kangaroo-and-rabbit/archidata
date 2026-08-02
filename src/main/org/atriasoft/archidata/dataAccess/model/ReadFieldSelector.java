package org.atriasoft.archidata.dataAccess.model;

import java.util.List;

import org.atriasoft.archidata.annotation.AnnotationTools.FieldName;
import org.atriasoft.archidata.dataAccess.QueryOptions;
import org.atriasoft.archidata.dataAccess.options.FilterOmit;
import org.atriasoft.archidata.dataAccess.options.FilterValue;
import org.atriasoft.archidata.dataAccess.options.ReadAllColumn;
import org.atriasoft.archidata.exception.DataAccessException;

/**
 * Resolves which fields of an entity are read from the database.
 *
 * <p>The selection is applied twice for a single query, and both times through this class so the
 * two stay in sync:
 * <ul>
 *   <li>to build the MongoDB projection — an unselected field is never transferred over the wire;</li>
 *   <li>to build the Java object — an unselected field is left at its default value, and its AddOn
 *       is not resolved, which also drops the extra queries a relation field would have triggered.</li>
 * </ul>
 *
 * <p>Selection rules, in order:
 * <ol>
 *   <li>an AddOn field that cannot be retrieved is never read;</li>
 *   <li>a {@code @DataNotRead} field is only read with the {@link ReadAllColumn} option;</li>
 *   <li>the primary key is always read — an entity without its identifier can neither be updated,
 *       deleted, nor linked afterwards;</li>
 *   <li>with a {@link FilterValue}, only the listed fields are read (whitelist);</li>
 *   <li>with a {@link FilterOmit}, the listed fields are not read (blacklist).</li>
 * </ol>
 *
 * <p><b>Dotted paths.</b> A {@link FilterValue} entry may address an embedded sub-document
 * ({@code "address.city"}, {@code "address.geo.lat"}): the path is pushed to MongoDB as-is, so only
 * that part of the sub-document is transferred, and the top-level field is rebuilt partially — the
 * fields left out keep their default value. Naming both a field and one of its sub-paths reads the
 * whole field: the widest wins.
 *
 * <p>Dotted paths are rejected in a {@link FilterOmit}: the projection archidata builds is an
 * inclusion, and MongoDB does not allow mixing inclusions and exclusions in one projection. List
 * what to keep with a {@link FilterValue} instead.
 *
 * <p>Field names are matched on the structural name — the same one {@code FilterValue}/{@code
 * FilterOmit} use for updates, so {@code new FilterValue(User::getName)} designates the same field
 * on a read and on a write. An unknown name is rejected: silently reading nothing would be a much
 * harder failure to diagnose than an exception.
 */
public final class ReadFieldSelector {

	private final boolean readAllColumns;
	/** Whitelisted structural field names or dotted paths, or {@code null} without {@link FilterValue}. */
	private final List<String> include;
	/** Blacklisted structural field names, or {@code null} without {@link FilterOmit}. */
	private final List<String> omit;
	/** Primary key descriptor, always read whatever the filters say. May be {@code null}. */
	private final DbPropertyDescriptor primaryKey;

	private ReadFieldSelector(final boolean readAllColumns, final List<String> include, final List<String> omit,
			final DbPropertyDescriptor primaryKey) {
		this.readAllColumns = readAllColumns;
		this.include = include;
		this.omit = omit;
		this.primaryKey = primaryKey;
	}

	/**
	 * Resolve the selector of a query.
	 *
	 * @param model the model of the read entity
	 * @param options the query options, may be {@code null}
	 * @return the selector to apply on every field of the entity
	 * @throws DataAccessException if the restriction options are ambiguous, name an unknown field,
	 *         or omit a dotted path
	 */
	public static ReadFieldSelector of(final DbClassModel model, final QueryOptions options)
			throws DataAccessException {
		return of(model, QueryOptions.readAllColumn(options), options);
	}

	/**
	 * Resolve the selector of a query with an already known {@link ReadAllColumn} state.
	 *
	 * @param model the model of the read entity
	 * @param readAllColumns {@code true} to also read the {@code @DataNotRead} fields
	 * @param options the query options, may be {@code null}
	 * @return the selector to apply on every field of the entity
	 * @throws DataAccessException if the restriction options are ambiguous, name an unknown field,
	 *         or omit a dotted path
	 */
	public static ReadFieldSelector of(
			final DbClassModel model,
			final boolean readAllColumns,
			final QueryOptions options) throws DataAccessException {
		final DbPropertyDescriptor primaryKey = model == null ? null : model.getPrimaryKey();
		if (options == null) {
			return new ReadFieldSelector(readAllColumns, null, null, primaryKey);
		}
		final List<FilterValue> includes = options.get(FilterValue.class);
		if (includes.size() > 1) {
			throw new DataAccessException("Request a read with more than 1 FilterValue of values");
		}
		final List<FilterOmit> omits = options.get(FilterOmit.class);
		if (omits.size() > 1) {
			throw new DataAccessException("Request a read with more than 1 FilterOmit of values");
		}
		final List<String> include = includes.isEmpty() ? null : includes.get(0).getValues();
		final List<String> omit = omits.isEmpty() ? null : omits.get(0).getValues();
		checkFieldsExist(model, include, "FilterValue");
		checkFieldsExist(model, omit, "FilterOmit");
		if (omit != null) {
			for (final String path : omit) {
				if (path.indexOf('.') >= 0) {
					throw new DataAccessException("FilterOmit does not support the dotted path '" + path
							+ "': a projection cannot mix inclusions and exclusions, list what to keep with a FilterValue");
				}
			}
		}
		return new ReadFieldSelector(readAllColumns, include, omit, primaryKey);
	}

	/** Reject a restriction naming a field the entity does not have — a typo must not read nothing. */
	private static void checkFieldsExist(final DbClassModel model, final List<String> paths, final String origin)
			throws DataAccessException {
		if (model == null || paths == null) {
			return;
		}
		for (final String path : paths) {
			if (model.findByFieldPath(path) == null) {
				throw new DataAccessException(
						origin + " references the unknown field '" + topLevelOf(path) + "' on " + model.getTableName());
			}
		}
	}

	/** Top-level field name of a path: what stands before the first dot. */
	private static String topLevelOf(final String path) {
		final int dot = path.indexOf('.');
		return dot < 0 ? path : path.substring(0, dot);
	}

	/** Tells whether a path list addresses the given top-level field, whole or through a sub-path. */
	private static boolean addresses(final List<String> paths, final String field) {
		for (final String path : paths) {
			if (path.length() == field.length()) {
				if (path.equals(field)) {
					return true;
				}
			} else if (path.length() > field.length() && path.charAt(field.length()) == '.' && path.startsWith(field)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Tells whether a field must be read from the database.
	 *
	 * @param desc the field to test
	 * @param options the query options (used to resolve a renamed column), may be {@code null}
	 * @return {@code true} if the field must be transferred and mapped
	 */
	public boolean isRead(final DbPropertyDescriptor desc, final QueryOptions options) {
		if (desc.getAction() == DbFieldAction.ADDON && !desc.canRetrieve()) {
			return false;
		}
		if (!this.readAllColumns && desc.isNotRead()) {
			return false;
		}
		if (this.include == null && this.omit == null) {
			return true;
		}
		if (desc == this.primaryKey) {
			return true;
		}
		// Matched on the structural name: the name a rename option maps *from*, and the one
		// FilterValue/FilterOmit already use for the updates.
		final String name = desc.getFieldName(options).inStruct();
		if (this.include != null && !addresses(this.include, name)) {
			return false;
		}
		return this.omit == null || !this.omit.contains(name);
	}

	/**
	 * Append the projection paths of a read field: the field itself, or the dotted sub-paths when
	 * the query only asks for a part of an embedded sub-document.
	 *
	 * @param desc the field to project (must be {@link #isRead} first)
	 * @param options the query options (used to resolve a renamed column), may be {@code null}
	 * @param out the projection paths accumulated so far
	 */
	public void collectProjectionPaths(
			final DbPropertyDescriptor desc,
			final QueryOptions options,
			final List<String> out) {
		final FieldName fieldName = desc.getFieldName(options);
		if (this.include == null) {
			out.add(fieldName.inTable());
			return;
		}
		final String struct = fieldName.inStruct();
		// The whole field is asked for: it wins over any sub-path of the same field.
		for (final String path : this.include) {
			if (path.equals(struct)) {
				out.add(fieldName.inTable());
				return;
			}
		}
		boolean partial = false;
		for (final String path : this.include) {
			if (path.length() > struct.length() && path.charAt(struct.length()) == '.' && path.startsWith(struct)) {
				// Rebuilt on the table name: a renamed column keeps its sub-path.
				out.add(fieldName.inTable() + path.substring(struct.length()));
				partial = true;
			}
		}
		if (!partial) {
			// Read without being listed: the primary key.
			out.add(fieldName.inTable());
		}
	}

	/**
	 * Tells whether the query restricts the read fields beyond the model defaults.
	 *
	 * @return {@code true} if a {@link FilterValue} or a {@link FilterOmit} is applied
	 */
	public boolean hasFieldRestriction() {
		return this.include != null || this.omit != null;
	}
}
