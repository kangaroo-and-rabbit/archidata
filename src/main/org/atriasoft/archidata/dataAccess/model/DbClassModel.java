package org.atriasoft.archidata.dataAccess.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.atriasoft.archidata.annotation.AnnotationTools;
import org.atriasoft.archidata.bean.ClassModel;
import org.atriasoft.archidata.bean.PropertyDescriptor;
import org.atriasoft.archidata.bean.exception.IntrospectionException;
import org.atriasoft.archidata.dataAccess.QueryOptions;
import org.atriasoft.archidata.dataAccess.addOn.DataAccessAddOn;
import org.atriasoft.archidata.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Database-aware class model that wraps {@link ClassModel} with archidata-specific
 * metadata for MongoDB operations.
 *
 * <p>All metadata is pre-computed once and cached globally per class. This eliminates
 * the need for repeated reflection and annotation lookups during CRUD operations.
 *
 * <p>Usage: {@code DbClassModel model = DbClassModel.of(MyEntity.class, addOns);}
 */
public final class DbClassModel {
	private static final Logger LOGGER = LoggerFactory.getLogger(DbClassModel.class);

	/** Global thread-safe cache. */
	private static final ConcurrentHashMap<Class<?>, DbClassModel> CACHE = new ConcurrentHashMap<>();

	/** AddOns registered for field handling — shared across all models. */
	private static volatile List<DataAccessAddOn> registeredAddOns = new ArrayList<>();

	private final ClassModel classModel;
	private final String tableName;

	// Pre-categorized fields for fast iteration during CRUD operations
	private final DbPropertyDescriptor primaryKey;
	private final DbPropertyDescriptor creationTimestamp;
	private final DbPropertyDescriptor updateTimestamp;
	private final DbPropertyDescriptor deletedField;
	private final List<DbPropertyDescriptor> allFields;
	private final List<DbPropertyDescriptor> regularFields;
	private final List<DbPropertyDescriptor> addonFields;
	private final List<DbPropertyDescriptor> asyncInsertFields;
	private final List<DbPropertyDescriptor> asyncUpdateFields;
	private final String deletedFieldName;

	// ========== Static API ==========

	/**
	 * Register add-ons that will be used when building DbClassModels.
	 * Must be called before any {@link #of} calls, typically at application startup.
	 */
	public static void setAddOns(final List<DataAccessAddOn> addOns) {
		registeredAddOns = new ArrayList<>(addOns);
		CACHE.clear(); // Force rebuild with new add-ons
	}

	/**
	 * Get or create the DbClassModel for a given class (thread-safe, cached).
	 */
	public static DbClassModel of(final Class<?> clazz) throws IntrospectionException {
		final DbClassModel existing = CACHE.get(clazz);
		if (existing != null) {
			return existing;
		}
		try {
			return CACHE.computeIfAbsent(clazz, cls -> {
				try {
					return new DbClassModel(cls, registeredAddOns);
				} catch (final IntrospectionException | DataAccessException e) {
					throw new RuntimeException(e);
				}
			});
		} catch (final RuntimeException e) {
			if (e.getCause() instanceof final IntrospectionException ie) {
				throw ie;
			}
			if (e.getCause() instanceof final DataAccessException dae) {
				throw new IntrospectionException("DB model creation failed for " + clazz.getSimpleName(), dae);
			}
			throw e;
		}
	}

	/**
	 * Clear the global cache. Useful for testing.
	 */
	public static void clearCache() {
		CACHE.clear();
	}

	// ========== Public API ==========

	public ClassModel getClassModel() {
		return this.classModel;
	}

	public String getTableName() {
		return this.tableName;
	}

	/**
	 * Get table name considering QueryOptions overrides.
	 */
	public String getTableName(final QueryOptions options) throws DataAccessException {
		return AnnotationTools.getTableName(this.classModel.getClassType(), options);
	}

	public DbPropertyDescriptor getPrimaryKey() {
		return this.primaryKey;
	}

	public DbPropertyDescriptor getCreationTimestamp() {
		return this.creationTimestamp;
	}

	public DbPropertyDescriptor getUpdateTimestamp() {
		return this.updateTimestamp;
	}

	public DbPropertyDescriptor getDeletedField() {
		return this.deletedField;
	}

	public String getDeletedFieldName() {
		return this.deletedFieldName;
	}

	/** All fields (including special ones). */
	public List<DbPropertyDescriptor> getAllFields() {
		return this.allFields;
	}

	/** Regular fields (not primary key, not timestamps, not deleted, not addons). */
	public List<DbPropertyDescriptor> getRegularFields() {
		return this.regularFields;
	}

	/** Fields managed by DataAccessAddOns. */
	public List<DbPropertyDescriptor> getAddonFields() {
		return this.addonFields;
	}

	/** Fields that require async processing on insert. */
	public List<DbPropertyDescriptor> getAsyncInsertFields() {
		return this.asyncInsertFields;
	}

	/** Fields that require async processing on update. */
	public List<DbPropertyDescriptor> getAsyncUpdateFields() {
		return this.asyncUpdateFields;
	}

	/**
	 * Find a DbPropertyDescriptor by its DB field name.
	 */
	public DbPropertyDescriptor findByDbFieldName(final String dbFieldName) {
		for (final DbPropertyDescriptor desc : this.allFields) {
			if (desc.getDbFieldName().equals(dbFieldName)) {
				return desc;
			}
		}
		return null;
	}

	/**
	 * Find a DbPropertyDescriptor by its property name.
	 */
	public DbPropertyDescriptor findByPropertyName(final String propertyName) {
		for (final DbPropertyDescriptor desc : this.allFields) {
			if (desc.getProperty().getName().equals(propertyName)) {
				return desc;
			}
		}
		return null;
	}

	/**
	 * Check if any addon field needs previous data for update operations.
	 */
	public boolean needsPreviousDataForUpdate() {
		for (final DbPropertyDescriptor desc : this.addonFields) {
			if (desc.isPreviousDataNeeded()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Generate the list of field names for SELECT/projection.
	 */
	public List<String> generateSelectFields(final boolean readAllColumns, final QueryOptions options) {
		final List<String> fields = new ArrayList<>();
		for (final DbPropertyDescriptor desc : this.allFields) {
			if (!readAllColumns && desc.isNotRead()) {
				continue;
			}
			if (desc.getAddOn() != null && !desc.getAddOn().canRetrieve(desc)) {
				continue;
			}
			fields.add(desc.getFieldName(options).inTable());
		}
		return fields;
	}

	// ========== Private constructor ==========

	private DbClassModel(final Class<?> clazz, final List<DataAccessAddOn> addOns)
			throws IntrospectionException, DataAccessException {
		this.classModel = ClassModel.of(clazz);
		this.tableName = AnnotationTools.getTableName(clazz);

		final List<DbPropertyDescriptor> all = new ArrayList<>();
		final List<DbPropertyDescriptor> regular = new ArrayList<>();
		final List<DbPropertyDescriptor> addon = new ArrayList<>();
		final List<DbPropertyDescriptor> asyncInsert = new ArrayList<>();
		final List<DbPropertyDescriptor> asyncUpdate = new ArrayList<>();
		DbPropertyDescriptor pk = null;
		DbPropertyDescriptor createTs = null;
		DbPropertyDescriptor updateTs = null;
		DbPropertyDescriptor deleted = null;
		String deletedName = null;

		// 1st pass: build descriptors (without add-on resolution)
		for (final PropertyDescriptor prop : this.classModel.getProperties()) {
			final DbPropertyDescriptor dbProp = new DbPropertyDescriptor(prop);
			all.add(dbProp);
		}

		// 2nd pass: resolve add-ons (needs fully built DbPropertyDescriptor)
		for (final DbPropertyDescriptor dbProp : all) {
			dbProp.resolveAddOn(addOns);
		}

		// 3rd pass: build pre-compiled codecs (after add-on resolution)
		for (final DbPropertyDescriptor dbProp : all) {
			dbProp.buildCodec();
		}

		// 4th pass: categorize
		for (final DbPropertyDescriptor dbProp : all) {
			switch (dbProp.getAction()) {
				case PRIMARY_KEY:
					pk = dbProp;
					break;
				case CREATION_TIMESTAMP:
					createTs = dbProp;
					break;
				case UPDATE_TIMESTAMP:
					updateTs = dbProp;
					break;
				case DELETED:
					deleted = dbProp;
					deletedName = dbProp.getDbFieldName();
					break;
				case ADDON:
					addon.add(dbProp);
					break;
				default:
					regular.add(dbProp);
					break;
			}

			if (dbProp.isAsyncInsert()) {
				asyncInsert.add(dbProp);
			}
			if (dbProp.isAsyncUpdate()) {
				asyncUpdate.add(dbProp);
			}
		}

		this.primaryKey = pk;
		this.creationTimestamp = createTs;
		this.updateTimestamp = updateTs;
		this.deletedField = deleted;
		this.deletedFieldName = deletedName;
		this.allFields = Collections.unmodifiableList(all);
		this.regularFields = Collections.unmodifiableList(regular);
		this.addonFields = Collections.unmodifiableList(addon);
		this.asyncInsertFields = Collections.unmodifiableList(asyncInsert);
		this.asyncUpdateFields = Collections.unmodifiableList(asyncUpdate);

		LOGGER.trace("DbClassModel '{}': table='{}', pk={}, fields={}, addons={}",
				clazz.getSimpleName(), this.tableName,
				pk != null ? pk.getDbFieldName() : "none",
				regular.size(), addon.size());
	}

	@Override
	public String toString() {
		return "DbClassModel{" + this.classModel.getSimpleName()
				+ ", table='" + this.tableName + "'"
				+ ", fields=" + this.allFields.size()
				+ "}";
	}
}
