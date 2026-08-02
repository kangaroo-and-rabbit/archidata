package org.atriasoft.archidata.dataAccess.model;

import java.util.List;

import org.atriasoft.archidata.dataAccess.QueryOptions;
import org.atriasoft.archidata.dataAccess.options.FilterOmit;
import org.atriasoft.archidata.dataAccess.options.FilterValue;
import org.atriasoft.archidata.dataAccess.options.ReadAllColumn;
import org.atriasoft.archidata.exception.DataAccessException;

/**
 * Resolves which top-level fields of an entity are read from the database.
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
 * Field names are matched on the structural name — the same one {@code FilterValue}/{@code FilterOmit}
 * use for updates, so {@code new FilterValue(User::getName)} designates the same field on a read and
 * on a write.
 */
public final class ReadFieldSelector {

	private final boolean readAllColumns;
	/** Whitelisted structural field names, or {@code null} when no {@link FilterValue} is given. */
	private final List<String> include;
	/** Blacklisted structural field names, or {@code null} when no {@link FilterOmit} is given. */
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
	 * @throws DataAccessException if the options carry more than one {@link FilterValue} or more
	 *         than one {@link FilterOmit} (the intent would be ambiguous)
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
	 * @throws DataAccessException if the options carry more than one {@link FilterValue} or more
	 *         than one {@link FilterOmit}
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
		return new ReadFieldSelector(readAllColumns, //
				includes.isEmpty() ? null : includes.get(0).getValues(), //
				omits.isEmpty() ? null : omits.get(0).getValues(), //
				primaryKey);
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
		if (this.include != null && !this.include.contains(name)) {
			return false;
		}
		return this.omit == null || !this.omit.contains(name);
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
