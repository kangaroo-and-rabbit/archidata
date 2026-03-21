package org.atriasoft.archidata.dataAccess.model;

import java.lang.reflect.Field;
import java.util.List;

import org.atriasoft.archidata.annotation.AnnotationTools;
import org.atriasoft.archidata.annotation.AnnotationTools.FieldName;
import org.atriasoft.archidata.annotation.CreationTimestamp;
import org.atriasoft.archidata.annotation.DataAsyncHardDeleted;
import org.atriasoft.archidata.annotation.DataDeleted;
import org.atriasoft.archidata.annotation.DataNotRead;
import org.atriasoft.archidata.annotation.UpdateTimestamp;
import org.atriasoft.archidata.annotation.apiGenerator.ApiReadOnly;
import org.atriasoft.archidata.bean.PropertyDescriptor;
import org.atriasoft.archidata.bean.exception.IntrospectionException;
import org.atriasoft.archidata.dataAccess.QueryOptions;
import org.atriasoft.archidata.dataAccess.addOn.DataAccessAddOn;
import org.atriasoft.archidata.dataAccess.model.codec.MongoCodecFactory;
import org.atriasoft.archidata.dataAccess.model.codec.MongoFieldCodec;
import org.atriasoft.archidata.dataAccess.options.OptionRenameColumn;

import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.ws.rs.DefaultValue;

/**
 * Wraps a generic {@link PropertyDescriptor} with archidata-specific database metadata.
 *
 * <p>All annotation lookups are done once at construction time and cached.
 * The converters (toDb/fromDb) are pre-built lambdas for zero-overhead type conversion.
 */
public final class DbPropertyDescriptor {

	private final PropertyDescriptor property;

	// Pre-computed DB metadata
	private final String dbFieldName;
	private DbFieldAction action;
	private final boolean nullable;
	private final boolean unique;
	private final boolean apiReadOnly;
	private final boolean notRead;
	private final String defaultValue;
	private final int columnLength;
	private final GenerationType generationStrategy;

	// AddOn support (resolved in 2nd pass via resolveAddOn)
	private DataAccessAddOn addOn;
	private boolean asyncInsert;
	private boolean asyncUpdate;
	private boolean previousDataNeeded;
	private boolean canInsert;
	private boolean canRetrieve;
	private boolean hasDeleteAction;
	private AddOnFieldContext addonContext;

	// Pre-compiled codec for zero-overhead MongoDB read/write
	private MongoFieldCodec codec;

	DbPropertyDescriptor(final PropertyDescriptor property) {
		this.property = property;

		final Field field = property.getField();

		// DB field name: @Column(name) or property name
		if (field != null) {
			this.dbFieldName = AnnotationTools.getFieldNameRaw(field);
			this.nullable = AnnotationTools.getNullable(field) || !AnnotationTools.getColumnNotNull(field);
			this.unique = AnnotationTools.isUnique(field);
			this.columnLength = AnnotationTools.getLimitSize(field);
			this.generationStrategy = AnnotationTools.getStrategy(field);
		} else {
			this.dbFieldName = property.getName();
			this.nullable = true;
			this.unique = false;
			this.columnLength = 255;
			this.generationStrategy = null;
		}

		// Determine action from annotations (without addOns - resolved later)
		this.action = determineActionWithoutAddOn(property);

		// API flags
		this.apiReadOnly = property.hasAnnotation(ApiReadOnly.class);
		this.notRead = property.hasAnnotation(DataNotRead.class);

		// Default value
		final DefaultValue dv = property.getAnnotation(DefaultValue.class);
		this.defaultValue = dv != null ? dv.value() : null;

		// AddOn defaults (resolved in 2nd pass)
		this.addOn = null;
		this.asyncInsert = false;
		this.asyncUpdate = false;
		this.previousDataNeeded = false;
		this.canInsert = false;
		this.canRetrieve = false;
		this.hasDeleteAction = false;
	}

	/**
	 * 2nd pass: resolve add-on after construction so that isCompatibleField
	 * can receive a fully built DbPropertyDescriptor (this).
	 */
	void resolveAddOn(final List<DataAccessAddOn> addOns) {
		if (this.action != DbFieldAction.NORMAL || this.property.getField() == null) {
			return;
		}
		for (final DataAccessAddOn candidate : addOns) {
			if (candidate.isCompatibleField(this)) {
				this.addOn = candidate;
				this.action = DbFieldAction.ADDON;
				try {
					this.asyncInsert = candidate.isInsertAsync(this);
					this.asyncUpdate = candidate.isUpdateAsync(this);
					this.previousDataNeeded = candidate.isPreviousDataNeeded(this);
					this.canInsert = candidate.canInsert(this);
					this.canRetrieve = candidate.canRetrieve(this);
					this.hasDeleteAction = candidate.asDeleteAction(this);
				} catch (final Exception e) {
					// Keep defaults
				}
				return;
			}
		}
	}

	/**
	 * Build the pre-compiled codec after add-on resolution.
	 * Called from DbClassModel after resolveAddOn().
	 */
	void buildCodec() {
		if (this.property.canRead()) {
			this.codec = MongoCodecFactory.buildFieldCodec(this.property.getRawGetter(),
					this.property.canWrite() ? this.property.getRawSetter() : null, this.property.getTypeInfo(),
					this.dbFieldName);
		}
	}

	/**
	 * Build the pre-computed AddOn context after all models are in the cache.
	 * Called from DbClassModel in a separate pass after resolveAddOn().
	 */
	void buildAddonContext() throws IntrospectionException {
		if (this.action == DbFieldAction.ADDON) {
			this.addonContext = AddOnFieldContext.buildFor(this);
		}
	}

	// ========== Getters ==========

	/**
	 * Returns the underlying property descriptor with type and accessor information.
	 *
	 * @return the property descriptor
	 */
	public PropertyDescriptor getProperty() {
		return this.property;
	}

	/**
	 * Returns the MongoDB field name derived from {@code @Column(name)} or the property name.
	 *
	 * @return the database field name
	 */
	public String getDbFieldName() {
		return this.dbFieldName;
	}

	/**
	 * Returns the categorized action for this field (e.g. PRIMARY_KEY, ADDON, NORMAL).
	 *
	 * @return the field action category
	 */
	public DbFieldAction getAction() {
		return this.action;
	}

	/**
	 * Checks whether this field allows null values.
	 *
	 * @return {@code true} if the field is nullable
	 */
	public boolean isNullable() {
		return this.nullable;
	}

	/**
	 * Checks whether this field has a unique constraint.
	 *
	 * @return {@code true} if the field must be unique
	 */
	public boolean isUnique() {
		return this.unique;
	}

	/**
	 * Checks whether this field is marked as API read-only via {@code @ApiReadOnly}.
	 *
	 * @return {@code true} if the field is read-only in the API
	 */
	public boolean isApiReadOnly() {
		return this.apiReadOnly;
	}

	/**
	 * Checks whether this field is excluded from default reads via {@code @DataNotRead}.
	 *
	 * @return {@code true} if the field is not read by default
	 */
	public boolean isNotRead() {
		return this.notRead;
	}

	/**
	 * Returns the default value string from {@code @DefaultValue}, or {@code null} if none.
	 *
	 * @return the default value, or {@code null}
	 */
	public String getDefaultValue() {
		return this.defaultValue;
	}

	/**
	 * Returns the maximum column length from {@code @Column(length)}, defaulting to 255.
	 *
	 * @return the column length limit
	 */
	public int getColumnLength() {
		return this.columnLength;
	}

	/**
	 * Returns the ID generation strategy from {@code @GeneratedValue}, or {@code null} if none.
	 *
	 * @return the generation strategy, or {@code null}
	 */
	public GenerationType getGenerationStrategy() {
		return this.generationStrategy;
	}

	/**
	 * Returns the resolved {@link DataAccessAddOn} for this field, or {@code null} for non-addon fields.
	 *
	 * @return the add-on handler, or {@code null}
	 */
	public DataAccessAddOn getAddOn() {
		return this.addOn;
	}

	/**
	 * Checks whether this field requires asynchronous processing on insert.
	 *
	 * @return {@code true} if insert is async
	 */
	public boolean isAsyncInsert() {
		return this.asyncInsert;
	}

	/**
	 * Checks whether this field requires asynchronous processing on update.
	 *
	 * @return {@code true} if update is async
	 */
	public boolean isAsyncUpdate() {
		return this.asyncUpdate;
	}

	/**
	 * Checks whether this add-on field requires previous data for its operations.
	 *
	 * @return {@code true} if previous data is needed
	 */
	public boolean isPreviousDataNeeded() {
		return this.previousDataNeeded;
	}

	/**
	 * Checks whether this add-on field supports insert operations.
	 *
	 * @return {@code true} if the field can be inserted
	 */
	public boolean canInsert() {
		return this.canInsert;
	}

	/**
	 * Checks whether this add-on field supports retrieve operations.
	 *
	 * @return {@code true} if the field can be retrieved
	 */
	public boolean canRetrieve() {
		return this.canRetrieve;
	}

	/**
	 * Checks whether this add-on field has a delete action configured.
	 *
	 * @return {@code true} if a delete action is defined
	 */
	public boolean hasDeleteAction() {
		return this.hasDeleteAction;
	}

	/**
	 * Returns the pre-computed metadata for add-on-managed fields (relation annotations).
	 *
	 * @return the add-on field context, or {@code null} for non-addon fields
	 */
	public AddOnFieldContext getAddonContext() {
		return this.addonContext;
	}

	/**
	 * Returns the pre-compiled codec for zero-overhead MongoDB read/write.
	 *
	 * @return the field codec, or {@code null} if the property is not readable
	 */
	public MongoFieldCodec getCodec() {
		return this.codec;
	}

	/**
	 * Resolve the FieldName (inStruct + inTable) considering QueryOptions rename overrides.
	 * If no OptionRenameColumn matches, inTable == inStruct == dbFieldName.
	 *
	 * @param options the query options containing potential column renames, may be {@code null}
	 * @return the resolved field name pair
	 */
	public FieldName getFieldName(final QueryOptions options) {
		String inTable = this.dbFieldName;
		if (options != null) {
			final List<OptionRenameColumn> renames = options.get(OptionRenameColumn.class);
			for (final OptionRenameColumn rename : renames) {
				if (rename.columnName.equals(this.dbFieldName)) {
					inTable = rename.ColumnNewName;
					break;
				}
			}
		}
		return new FieldName(this.dbFieldName, inTable);
	}

	/**
	 * Checks whether this is a "generic" field that should not be updated by users
	 * (primary key, timestamps, deleted markers).
	 *
	 * @return {@code true} if the field is a system-managed generic field
	 */
	public boolean isGenericField() {
		return this.action == DbFieldAction.PRIMARY_KEY || this.action == DbFieldAction.CREATION_TIMESTAMP
				|| this.action == DbFieldAction.UPDATE_TIMESTAMP || this.action == DbFieldAction.DELETED
				|| this.action == DbFieldAction.ASYNC_HARD_DELETED;
	}

	// ========== Private helpers ==========

	private static DbFieldAction determineActionWithoutAddOn(final PropertyDescriptor property) {
		if (property.hasAnnotation(Id.class)) {
			return DbFieldAction.PRIMARY_KEY;
		}
		if (property.hasAnnotation(CreationTimestamp.class)) {
			return DbFieldAction.CREATION_TIMESTAMP;
		}
		if (property.hasAnnotation(UpdateTimestamp.class)) {
			return DbFieldAction.UPDATE_TIMESTAMP;
		}
		if (property.hasAnnotation(DataDeleted.class)) {
			return DbFieldAction.DELETED;
		}
		if (property.hasAnnotation(DataAsyncHardDeleted.class)) {
			return DbFieldAction.ASYNC_HARD_DELETED;
		}
		if (property.hasAnnotation(DataNotRead.class)) {
			return DbFieldAction.NOT_READ;
		}
		return DbFieldAction.NORMAL;
	}
}
