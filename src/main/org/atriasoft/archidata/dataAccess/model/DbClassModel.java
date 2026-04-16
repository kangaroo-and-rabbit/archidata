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
	private final List<DbPropertyDescriptor> deleteActionFields;
	private final boolean needsPreviousDataForDelete;
	private final boolean needsPreviousDataForUpdate;
	private final String deletedFieldName;
	private final DbPropertyDescriptor asyncHardDeletedField;
	private final String asyncHardDeletedFieldName;

	/** Flag to track if addon contexts have been built (lazy, after model is in cache). */
	private volatile boolean addonContextsBuilt;

	/** Track classes currently building their addon contexts to break circular references. */
	private static final ThreadLocal<java.util.Set<Class<?>>> BUILDING_CONTEXTS = ThreadLocal
			.withInitial(java.util.HashSet::new);

	// ========== Static API ==========

	/**
	 * Register add-ons that will be used when building DbClassModels.
	 * Must be called before any {@link #of} calls, typically at application startup.
	 *
	 * @param addOns the list of data access add-ons to register
	 */
	public static void setAddOns(final List<DataAccessAddOn> addOns) {
		registeredAddOns = new ArrayList<>(addOns);
		CACHE.clear(); // Force rebuild with new add-ons
	}

	/**
	 * Get or create the DbClassModel for a given class (thread-safe, cached).
	 *
	 * <p>AddOn contexts are built lazily after the model is in the cache to avoid
	 * circular reference issues (e.g. class A references class B which references A).
	 *
	 * @param clazz the entity class to model
	 * @return the cached or newly created DbClassModel for the class
	 * @throws IntrospectionException if the class cannot be introspected
	 */
	public static DbClassModel of(final Class<?> clazz) throws IntrospectionException {
		final DbClassModel existing = CACHE.get(clazz);
		if (existing != null) {
			existing.ensureAddonContextsBuilt();
			return existing;
		}
		final DbClassModel model;
		try {
			model = CACHE.computeIfAbsent(clazz, cls -> {
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
		model.ensureAddonContextsBuilt();
		return model;
	}

	/**
	 * Clear the global cache. Useful for testing.
	 */
	public static void clearCache() {
		CACHE.clear();
	}

	// ========== Public API ==========

	/**
	 * Returns the underlying {@link ClassModel} with introspected property descriptors.
	 *
	 * @return the class model
	 */
	public ClassModel getClassModel() {
		return this.classModel;
	}

	/**
	 * Returns the MongoDB collection name for this entity.
	 *
	 * @return the table (collection) name
	 */
	public String getTableName() {
		return this.tableName;
	}

	/**
	 * Get table name considering QueryOptions overrides.
	 *
	 * @param options the query options that may override the table name
	 * @return the resolved table (collection) name
	 * @throws DataAccessException if the table name cannot be resolved
	 */
	public String getTableName(final QueryOptions options) throws DataAccessException {
		return AnnotationTools.getTableName(this.classModel.getClassType(), options);
	}

	/**
	 * Returns the descriptor for the primary key field, or {@code null} if none exists.
	 *
	 * @return the primary key descriptor, or {@code null}
	 */
	public DbPropertyDescriptor getPrimaryKey() {
		return this.primaryKey;
	}

	/**
	 * Returns the descriptor for the creation timestamp field, or {@code null} if none exists.
	 *
	 * @return the creation timestamp descriptor, or {@code null}
	 */
	public DbPropertyDescriptor getCreationTimestamp() {
		return this.creationTimestamp;
	}

	/**
	 * Returns the descriptor for the update timestamp field, or {@code null} if none exists.
	 *
	 * @return the update timestamp descriptor, or {@code null}
	 */
	public DbPropertyDescriptor getUpdateTimestamp() {
		return this.updateTimestamp;
	}

	/**
	 * Returns the descriptor for the soft-delete marker field, or {@code null} if none exists.
	 *
	 * @return the deleted field descriptor, or {@code null}
	 */
	public DbPropertyDescriptor getDeletedField() {
		return this.deletedField;
	}

	/**
	 * Returns the DB column name of the soft-delete marker field, or {@code null} if none exists.
	 *
	 * @return the deleted field column name, or {@code null}
	 */
	public String getDeletedFieldName() {
		return this.deletedFieldName;
	}

	/**
	 * Returns the descriptor for the async hard-delete marker field, or {@code null} if none exists.
	 *
	 * @return the async hard-deleted field descriptor, or {@code null}
	 */
	public DbPropertyDescriptor getAsyncHardDeletedField() {
		return this.asyncHardDeletedField;
	}

	/**
	 * Returns the DB column name of the async hard-delete marker field, or {@code null} if none exists.
	 *
	 * @return the async hard-deleted field column name, or {@code null}
	 */
	public String getAsyncHardDeletedFieldName() {
		return this.asyncHardDeletedFieldName;
	}

	/**
	 * Returns all fields (including special ones such as primary key, timestamps, and add-ons).
	 *
	 * @return an unmodifiable list of all field descriptors
	 */
	public List<DbPropertyDescriptor> getAllFields() {
		return this.allFields;
	}

	/**
	 * Returns regular fields (not primary key, not timestamps, not deleted, not add-ons).
	 *
	 * @return an unmodifiable list of regular field descriptors
	 */
	public List<DbPropertyDescriptor> getRegularFields() {
		return this.regularFields;
	}

	/**
	 * Returns fields managed by {@link DataAccessAddOn} implementations.
	 *
	 * @return an unmodifiable list of add-on field descriptors
	 */
	public List<DbPropertyDescriptor> getAddonFields() {
		return this.addonFields;
	}

	/**
	 * Returns fields that require asynchronous processing on insert.
	 *
	 * @return an unmodifiable list of async insert field descriptors
	 */
	public List<DbPropertyDescriptor> getAsyncInsertFields() {
		return this.asyncInsertFields;
	}

	/**
	 * Returns fields that require asynchronous processing on update.
	 *
	 * @return an unmodifiable list of async update field descriptors
	 */
	public List<DbPropertyDescriptor> getAsyncUpdateFields() {
		return this.asyncUpdateFields;
	}

	/**
	 * Returns add-on fields that have delete actions configured.
	 *
	 * @return an unmodifiable list of delete action field descriptors
	 */
	public List<DbPropertyDescriptor> getDeleteActionFields() {
		return this.deleteActionFields;
	}

	/**
	 * Checks whether any add-on field with a delete action needs previous data.
	 *
	 * @return {@code true} if previous data is needed for delete operations
	 */
	public boolean needsPreviousDataForDelete() {
		return this.needsPreviousDataForDelete;
	}

	/**
	 * Find a {@link DbPropertyDescriptor} by its DB field name.
	 *
	 * @param dbFieldName the database column name to search for
	 * @return the matching descriptor, or {@code null} if not found
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
	 * Find a {@link DbPropertyDescriptor} by its Java property name.
	 *
	 * @param propertyName the Java property name to search for
	 * @return the matching descriptor, or {@code null} if not found
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
	 * Checks whether any add-on field needs previous data for update operations.
	 *
	 * @return {@code true} if previous data is needed for update operations
	 */
	public boolean needsPreviousDataForUpdate() {
		return this.needsPreviousDataForUpdate;
	}

	/**
	 * Generate the list of field names for SELECT/projection.
	 *
	 * @param readAllColumns if {@code true}, include all columns regardless of {@code @DataNotRead}
	 * @param options the query options for column renaming
	 * @return a list of resolved field names for the projection
	 */
	public List<String> generateSelectFields(final boolean readAllColumns, final QueryOptions options) {
		final List<String> fields = new ArrayList<>();
		for (final DbPropertyDescriptor desc : this.allFields) {
			if (!readAllColumns && desc.isNotRead()) {
				continue;
			}
			if (desc.getAction() == DbFieldAction.ADDON && !desc.canRetrieve()) {
				continue;
			}
			fields.add(desc.getFieldName(options).inTable());
		}
		return fields;
	}

	// ========== Lazy addon context initialization ==========

	/**
	 * Build AddOn contexts if not yet built. Called after the model is in the cache
	 * to avoid circular reference deadlocks in ConcurrentHashMap.computeIfAbsent().
	 *
	 * <p>Uses a ThreadLocal set to detect circular references (A refs B refs A).
	 * When re-entrance is detected, the method returns immediately — the contexts
	 * will be fully built when the outermost call completes.
	 */
	private void ensureAddonContextsBuilt() throws IntrospectionException {
		if (this.addonContextsBuilt) {
			return;
		}
		final Class<?> clazz = this.classModel.getClassType();
		final java.util.Set<Class<?>> building = BUILDING_CONTEXTS.get();
		if (!building.add(clazz)) {
			return;
		}
		try {
			synchronized (this) {
				if (this.addonContextsBuilt) {
					return;
				}
				for (final DbPropertyDescriptor dbProp : this.allFields) {
					dbProp.buildAddonContext();
				}
				this.addonContextsBuilt = true;
			}
		} finally {
			building.remove(clazz);
		}
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
		final List<DbPropertyDescriptor> deleteAction = new ArrayList<>();
		boolean needsPrevDataForDelete = false;
		boolean needsPrevDataForUpdate = false;
		DbPropertyDescriptor pk = null;
		DbPropertyDescriptor createTs = null;
		DbPropertyDescriptor updateTs = null;
		DbPropertyDescriptor deleted = null;
		DbPropertyDescriptor asyncHardDeleted = null;
		String deletedName = null;
		String asyncHardDeletedName = null;

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
				case ASYNC_HARD_DELETED:
					asyncHardDeleted = dbProp;
					asyncHardDeletedName = dbProp.getDbFieldName();
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
				if (dbProp.isPreviousDataNeeded()) {
					needsPrevDataForUpdate = true;
				}
			}
			if (dbProp.hasDeleteAction()) {
				deleteAction.add(dbProp);
				if (dbProp.isPreviousDataNeeded()) {
					needsPrevDataForDelete = true;
				}
			}
		}

		this.primaryKey = pk;
		this.creationTimestamp = createTs;
		this.updateTimestamp = updateTs;
		this.deletedField = deleted;
		this.deletedFieldName = deletedName;
		this.asyncHardDeletedField = asyncHardDeleted;
		this.asyncHardDeletedFieldName = asyncHardDeletedName;
		this.allFields = Collections.unmodifiableList(all);
		this.regularFields = Collections.unmodifiableList(regular);
		this.addonFields = Collections.unmodifiableList(addon);
		this.asyncInsertFields = Collections.unmodifiableList(asyncInsert);
		this.asyncUpdateFields = Collections.unmodifiableList(asyncUpdate);
		this.deleteActionFields = Collections.unmodifiableList(deleteAction);
		this.needsPreviousDataForDelete = needsPrevDataForDelete;
		this.needsPreviousDataForUpdate = needsPrevDataForUpdate;

		LOGGER.trace("DbClassModel '{}': table='{}', pk={}, fields={}, addons={}", clazz.getSimpleName(),
				this.tableName, pk != null ? pk.getDbFieldName() : "none", regular.size(), addon.size());
	}

	@Override
	public String toString() {
		return "DbClassModel{" + this.classModel.getSimpleName() + ", table='" + this.tableName + "'" + ", fields="
				+ this.allFields.size() + "}";
	}
}
