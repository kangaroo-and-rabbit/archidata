package org.atriasoft.archidata.dataAccess.model;

import java.lang.reflect.Field;
import java.util.List;

import org.atriasoft.archidata.annotation.AnnotationTools;
import org.atriasoft.archidata.annotation.AnnotationTools.FieldName;
import org.atriasoft.archidata.annotation.CreationTimestamp;
import org.atriasoft.archidata.annotation.DataDeleted;
import org.atriasoft.archidata.annotation.DataJson;
import org.atriasoft.archidata.annotation.DataNotRead;
import org.atriasoft.archidata.annotation.UpdateTimestamp;
import org.atriasoft.archidata.annotation.apiGenerator.ApiReadOnly;
import org.atriasoft.archidata.bean.PropertyDescriptor;
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

	// Pre-compiled codec for zero-overhead MongoDB read/write
	private MongoFieldCodec codec;

	DbPropertyDescriptor(
			final PropertyDescriptor property) {
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
			this.codec = MongoCodecFactory.buildFieldCodec(
					this.property.getRawGetter(),
					this.property.canWrite() ? this.property.getRawSetter() : null,
					this.property.getTypeInfo(),
					this.dbFieldName);
		}
	}

	// ========== Getters ==========

	public PropertyDescriptor getProperty() {
		return this.property;
	}

	public String getDbFieldName() {
		return this.dbFieldName;
	}

	public DbFieldAction getAction() {
		return this.action;
	}

	public boolean isNullable() {
		return this.nullable;
	}

	public boolean isUnique() {
		return this.unique;
	}

	public boolean isApiReadOnly() {
		return this.apiReadOnly;
	}

	public boolean isNotRead() {
		return this.notRead;
	}

	public String getDefaultValue() {
		return this.defaultValue;
	}

	public int getColumnLength() {
		return this.columnLength;
	}

	public GenerationType getGenerationStrategy() {
		return this.generationStrategy;
	}

	public DataAccessAddOn getAddOn() {
		return this.addOn;
	}

	public boolean isAsyncInsert() {
		return this.asyncInsert;
	}

	public boolean isAsyncUpdate() {
		return this.asyncUpdate;
	}

	public boolean isPreviousDataNeeded() {
		return this.previousDataNeeded;
	}

	/** Pre-compiled codec for zero-overhead MongoDB read/write. May be null if property is not readable. */
	public MongoFieldCodec getCodec() {
		return this.codec;
	}

	/**
	 * Resolve the FieldName (inStruct + inTable) considering QueryOptions rename overrides.
	 * If no OptionRenameColumn matches, inTable == inStruct == dbFieldName.
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

	/** Quick check: is this a "generic" field that shouldn't be updated by users? */
	public boolean isGenericField() {
		return this.action == DbFieldAction.PRIMARY_KEY
				|| this.action == DbFieldAction.CREATION_TIMESTAMP
				|| this.action == DbFieldAction.UPDATE_TIMESTAMP
				|| this.action == DbFieldAction.DELETED;
	}

	// ========== Private helpers ==========

	private static DbFieldAction determineActionWithoutAddOn(
			final PropertyDescriptor property) {
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
		if (property.hasAnnotation(DataJson.class)) {
			return DbFieldAction.JSON;
		}
		if (property.hasAnnotation(DataNotRead.class)) {
			return DbFieldAction.NOT_READ;
		}
		return DbFieldAction.NORMAL;
	}
}
