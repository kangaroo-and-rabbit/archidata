package org.atriasoft.archidata.dataAccess.addOn;

import java.util.List;

import org.atriasoft.archidata.dataAccess.DBAccessMongo;
import org.atriasoft.archidata.dataAccess.LazyGetter;
import org.atriasoft.archidata.dataAccess.LazyGetterCollector;
import org.atriasoft.archidata.dataAccess.QueryOptions;
import org.atriasoft.archidata.dataAccess.model.DbPropertyDescriptor;
import org.bson.Document;

/**
 * Interface for add-ons that extend the database access layer with support for
 * complex field types such as ManyToMany, OneToMany, and ManyToOne relationships.
 *
 * <p>Implementations handle insertion, retrieval, update, and deletion of
 * relationship-linked fields that cannot be managed by standard field codecs.
 */
public interface DataAccessAddOn {
	/** Get the Class of the declaration annotation.
	 * @return The annotation class */
	Class<?> getAnnotationClass();

	/** Check if the field is managed by the local add-on.
	 * @param desc Property descriptor to inspect.
	 * @return True if the field is managed by the current add-on. */
	boolean isCompatibleField(DbPropertyDescriptor desc);

	/**
	 * Inserts add-on field data into the BSON document being built for insertion.
	 *
	 * @param ioDb       The database access instance
	 * @param desc       The property descriptor for the add-on field
	 * @param rootObject The entity being inserted
	 * @param options    Query options for the current operation
	 * @param docSet     The BSON document accumulating {@code $set} fields
	 * @param docUnSet   The BSON document accumulating {@code $unset} fields
	 * @throws Exception if data insertion fails
	 */
	void insertData(
			final DBAccessMongo ioDb,
			final DbPropertyDescriptor desc,
			final Object rootObject,
			final QueryOptions options,
			final Document docSet,
			final Document docUnSet) throws Exception;

	/** Check if the add-on field can be included in the main insert request.
	 * @param desc The property descriptor for the add-on field
	 * @return true if the field can be inserted in the main request */
	default boolean canInsert(final DbPropertyDescriptor desc) {
		return false;
	}

	/** Check if the add-on field can be retrieved from the database.
	 * @param desc The property descriptor for the add-on field
	 * @return true if the field can be retrieved */
	default boolean canRetrieve(final DbPropertyDescriptor desc) {
		return false;
	}

	/**
	 * Populates an add-on field from a MongoDB document during entity retrieval.
	 *
	 * @param ioDb           The database access instance
	 * @param doc            The MongoDB document being read
	 * @param desc           The property descriptor for the add-on field
	 * @param data           The target entity object being populated
	 * @param options        Query options for the current operation
	 * @param lazyCall       List to collect deferred entity-reference loading actions
	 * @param batchCollector Collector for batching entity-reference loads across rows
	 * @throws Exception if field population fails
	 */
	void fillFromDoc(
			final DBAccessMongo ioDb,
			Document doc,
			DbPropertyDescriptor desc,
			Object data,
			QueryOptions options,
			final List<LazyGetter> lazyCall,
			final LazyGetterCollector batchCollector) throws Exception;

	/** Check if insertion of this field requires asynchronous processing.
	 * @param desc The property descriptor for the add-on field
	 * @return true if the field requires async insertion
	 * @throws Exception if the check fails */
	default boolean isInsertAsync(final DbPropertyDescriptor desc) throws Exception {
		return false;
	}

	/** Performs asynchronous insert operations for add-on fields after the main insert.
	 * @param ioDb    The database access instance
	 * @param clazz   The entity class being inserted
	 * @param localId Local ID (primary key) of the inserted entity
	 * @param desc    Property descriptor that is updated
	 * @param data    Data that might be inserted
	 * @param actions List to collect asynchronous actions to execute after the main request
	 * @param options Query options for the current operation
	 * @throws Exception if the async insert fails */
	default void asyncInsert(
			final DBAccessMongo ioDb,
			final Class<?> clazz,
			final Object localId,
			final DbPropertyDescriptor desc,
			final Object data,
			final List<LazyGetter> actions,
			final QueryOptions options) throws Exception {

	}

	/** Check if update of this field requires asynchronous processing.
	 * @param desc The property descriptor for the add-on field
	 * @return true if the field requires async update
	 * @throws Exception if the check fails */
	default boolean isUpdateAsync(final DbPropertyDescriptor desc) throws Exception {
		return false;
	}

	/** Performs asynchronous update operations for add-on fields after the main update.
	 * @param ioDb         The database access instance
	 * @param previousData The entity data before the update
	 * @param localId      Local ID (primary key) of the entity being updated
	 * @param desc         Property descriptor that is updated
	 * @param data         The new data being applied
	 * @param actions      List to collect asynchronous actions to execute after the main request
	 * @param options      Query options for the current operation
	 * @throws Exception if the async update fails */
	default void asyncUpdate(
			final DBAccessMongo ioDb,
			final Object previousData,
			final Object localId,
			final DbPropertyDescriptor desc,
			final Object data,
			final List<LazyGetter> actions,
			final QueryOptions options) throws Exception {

	}

	/** Check if the previous data must be loaded before performing an update.
	 * @param desc The property descriptor for the add-on field
	 * @return true if the previous entity data is needed for the update */
	default boolean isPreviousDataNeeded(final DbPropertyDescriptor desc) {
		return false;
	}

	/**
	 * Check if the add-on field has delete actions (e.g., cascade delete or link cleanup).
	 *
	 * @param desc The property descriptor for the add-on field
	 * @return true if a delete action is required for this field
	 */
	default boolean asDeleteAction(final DbPropertyDescriptor desc) {
		return false;
	}

	/**
	 * Executes delete-time actions for this add-on field (e.g., cascade deletes, link cleanup).
	 *
	 * @param ioDb         The database access instance
	 * @param clazz        The entity class being deleted
	 * @param desc         The property descriptor for the add-on field
	 * @param previousData The list of entities being deleted (with their current values)
	 * @param actions      List to collect asynchronous actions to execute after the delete
	 * @throws Exception if the delete action fails
	 */
	default void onDelete(
			final DBAccessMongo ioDb,
			final Class<?> clazz,
			final DbPropertyDescriptor desc,
			final List<Object> previousData,
			final List<LazyGetter> actions) throws Exception {

	}

	/**
	 * Drops any auxiliary data structures associated with this add-on field.
	 *
	 * @param ioDb      The database access instance
	 * @param tableName The collection name being dropped
	 * @param desc      The property descriptor for the add-on field
	 * @param options   Query options for the current operation
	 * @throws Exception if the drop action fails
	 */
	default void drop(
			final DBAccessMongo ioDb,
			final String tableName,
			final DbPropertyDescriptor desc,
			final QueryOptions options) throws Exception {

	}

	/**
	 * Cleans all auxiliary data for this add-on field without dropping the structure.
	 *
	 * @param ioDb      The database access instance
	 * @param tableName The collection name being cleaned
	 * @param desc      The property descriptor for the add-on field
	 * @param options   Query options for the current operation
	 * @throws Exception if the clean action fails
	 */
	default void cleanAll(
			final DBAccessMongo ioDb,
			final String tableName,
			final DbPropertyDescriptor desc,
			final QueryOptions options) throws Exception {

	}

}
