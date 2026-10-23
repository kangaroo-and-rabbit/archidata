package org.atriasoft.archidata.dataAccess.model;

import java.util.List;

import org.atriasoft.archidata.annotation.AnnotationTools.FieldName;
import org.atriasoft.archidata.bean.TypeInfo;
import org.atriasoft.archidata.dataAccess.QueryOptions;
import org.atriasoft.archidata.dataAccess.options.FilterOmit;
import org.atriasoft.archidata.dataAccess.options.FilterValue;
import org.atriasoft.archidata.dataAccess.options.OptionSpecifyType;
import org.atriasoft.archidata.dataAccess.options.ReadAllColumn;
import org.atriasoft.archidata.exception.DataAccessException;
import org.atriasoft.archidata.tools.ConfigBaseVariable;
import org.atriasoft.archidata.tools.TypeUtils;

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
 * <p><b>Embedded objects are projected down to their leaf fields</b>, by their declared type: a
 * sub-field the read does not return ({@code @DataNotRead} without {@link ReadAllColumn},
 * {@code @DataEncrypt} when the decryption is disabled), a relation of an embedded object and a key
 * the model does not know are not transferred. A {@code Map} is included whole (the protected sub-fields of its
 * values are transferred, without a log, then dropped by the Java read); a {@code List} /
 * {@code Set} of embedded objects is projected down to its leaves only when the read hides a sub-field
 * of its elements. A dotted {@link FilterValue} path is resolved the same way. The rules are those of
 * {@code LeafProjection}.
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
	/** The read entity class, to name a field in a log. May be {@code null}. */
	private final Class<?> entity;

	private ReadFieldSelector(final boolean readAllColumns, final List<String> include, final List<String> omit,
			final DbPropertyDescriptor primaryKey, final Class<?> entity) {
		this.readAllColumns = readAllColumns;
		this.include = include;
		this.omit = omit;
		this.primaryKey = primaryKey;
		this.entity = entity;
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
		final Class<?> entity = model == null ? null : model.getClassModel().getClassType();
		if (options == null) {
			return new ReadFieldSelector(readAllColumns, null, null, primaryKey, entity);
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
		return new ReadFieldSelector(readAllColumns, include, omit, primaryKey, entity);
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
	 * Append the projection paths of a read field: the field itself, or the paths of the leaf fields of
	 * an embedded object (a protected sub-field left out), or the dotted sub-paths when the query only
	 * asks for a part of an embedded sub-document. The paths of several fields may overlap
	 * ({@code FilterValue("a.b", "a.b.c")}): {@link DbClassModel#generateSelectFields} keeps the widest.
	 *
	 * <p>A field the read decodes with another class than the declared one ({@link #overriddenReadType}) is
	 * projected down to the leaf fields of that class, less every sub-field protected in it or in the declared class
	 * at the same key (and that this read does not return): the values were written by the declared class, whose
	 * protection holds whatever the class the read uses. A sub-field only the read class knows is projected by it.
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
		final boolean modeled = desc.getAddOn() == null && desc.getCodec() != null;
		final TypeInfo readType = modeled ? overriddenReadType(desc, options) : null;
		final boolean decryptEnabled = ConfigBaseVariable.getDataDecryptEnabled();
		if (this.include == null) {
			appendWhole(desc, fieldName.inTable(), modeled, readType, decryptEnabled, out);
			return;
		}
		final String struct = fieldName.inStruct();
		// The whole field is asked for: it wins over any sub-path of the same field.
		for (final String path : this.include) {
			if (path.equals(struct)) {
				appendWhole(desc, fieldName.inTable(), modeled, readType, decryptEnabled, out);
				return;
			}
		}
		boolean partial = false;
		for (final String path : this.include) {
			if (path.length() > struct.length() && path.charAt(struct.length()) == '.' && path.startsWith(struct)) {
				// Rebuilt on the table name: a renamed column keeps its sub-path.
				final String subPath = path.substring(struct.length() + 1);
				if (!modeled) {
					out.add(fieldName.inTable() + "." + subPath);
				} else if (readType == null) {
					LeafProjection.appendSubPath(site(desc), fieldName.inTable(), desc.getProperty().getTypeInfo(),
							subPath, this.readAllColumns, decryptEnabled, out);
				} else {
					LeafProjection.appendSubPathAs(site(desc), fieldName.inTable(), desc.getProperty().getTypeInfo(),
							readType, subPath, this.readAllColumns, decryptEnabled, out);
				}
				partial = true;
			}
		}
		if (!partial) {
			// Read without being listed: the primary key.
			appendWhole(desc, fieldName.inTable(), modeled, readType, decryptEnabled, out);
		}
	}

	private void appendWhole(
			final DbPropertyDescriptor desc,
			final String inTable,
			final boolean modeled,
			final TypeInfo readType,
			final boolean decryptEnabled,
			final List<String> out) {
		if (!modeled) {
			out.add(inTable);
		} else if (readType == null) {
			LeafProjection.appendWhole(site(desc), inTable, desc.getProperty().getTypeInfo(), this.readAllColumns,
					decryptEnabled, out);
		} else {
			LeafProjection.appendWholeAs(site(desc), inTable, desc.getProperty().getTypeInfo(), readType,
					this.readAllColumns, decryptEnabled, out);
		}
	}

	/** How a field is named in a log: {@code "Field com.app.Account.tree"}. */
	private String site(final DbPropertyDescriptor desc) {
		return "Field " + (this.entity == null ? "?" : this.entity.getName()) + "." + desc.getProperty().getName();
	}

	/**
	 * Gives the type a read decodes a field with when an {@link OptionSpecifyType} changes it: the read honours the
	 * option (the first one naming the field) only on a field declared {@code Object} (the primary key, the one
	 * such field the model accepts), read as the given class, and, with {@code isList}, on a {@code List} field,
	 * read as a {@code List} of the given class. Anywhere else the read ignores the option, and so does the
	 * projection.
	 *
	 * @param desc the field
	 * @param options the query options, may be {@code null}
	 * @return the type the read decodes the field with, or {@code null} when it is the declared one
	 */
	public static TypeInfo overriddenReadType(final DbPropertyDescriptor desc, final QueryOptions options) {
		if (options == null) {
			return null;
		}
		for (final OptionSpecifyType specify : options.get(OptionSpecifyType.class)) {
			if (specify.name.equals(desc.getProperty().getName())) {
				if (specify.isList) {
					return desc.getProperty().getTypeInfo().isList()
							? new TypeInfo(List.class, specify.clazz, null, TypeUtils.listOf(specify.clazz))
							: null;
				}
				return desc.getProperty().getType() == Object.class ? TypeInfo.ofRaw(specify.clazz) : null;
			}
		}
		return null;
	}

	/**
	 * Removes from the value of a field read from the database the sub-fields its declared type protects and this
	 * read does not return ({@code @DataNotRead} without {@link ReadAllColumn}, {@code @DataEncrypt} when the
	 * decryption is disabled), at any depth; the other keys are kept. Called before the value is decoded with the
	 * class of an {@link OptionSpecifyType}, which may declare such a sub-field without protection: what the
	 * projection had to transfer whole (a {@code Map}, a part cut by the depth or the path limit) is filtered by the
	 * declared type all the same.
	 *
	 * @param desc the field
	 * @param value the value read from the database (not modified), may be {@code null}
	 * @return the value restricted to what the declared type returns
	 */
	public Object restrictToDeclaredType(final DbPropertyDescriptor desc, final Object value) {
		return LeafProjection.restrict(value, desc.getProperty().getTypeInfo(), this.readAllColumns,
				ConfigBaseVariable.getDataDecryptEnabled());
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
