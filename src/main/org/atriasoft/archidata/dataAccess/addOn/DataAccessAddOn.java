package org.atriasoft.archidata.dataAccess.addOn;

import java.util.List;

import org.atriasoft.archidata.dataAccess.DBAccessMongo;
import org.atriasoft.archidata.dataAccess.LazyGetter;
import org.atriasoft.archidata.dataAccess.QueryOptions;
import org.atriasoft.archidata.dataAccess.model.DbPropertyDescriptor;
import org.bson.Document;

public interface DataAccessAddOn {
	/** Get the Class of the declaration annotation
	 * @return The annotation class */
	Class<?> getAnnotationClass();

	/** Check if the field is manage by the local add-on
	 * @param desc Property descriptor to inspect.
	 * @return True of the field is manage by the current Add-on. */
	boolean isCompatibleField(DbPropertyDescriptor desc);

	void insertData(
			final DBAccessMongo ioDb,
			final DbPropertyDescriptor desc,
			final Object rootObject,
			final QueryOptions options,
			final Document docSet,
			final Document docUnSet) throws Exception;

	/** Element can insert in the single request
	 * @param desc
	 * @return */
	default boolean canInsert(final DbPropertyDescriptor desc) {
		return false;
	}

	/** Element can be retrieve with the specific mode
	 * @param desc
	 * @return */
	default boolean canRetrieve(final DbPropertyDescriptor desc) {
		return false;
	}

	// Return the number of Column read
	void fillFromDoc(
			final DBAccessMongo ioDb,
			Document doc,
			DbPropertyDescriptor desc,
			Object data,
			QueryOptions options,
			final List<LazyGetter> lazyCall) throws Exception;

	/** Some action must be done asynchronously for update or remove element
	 * @param desc
	 * @return */
	default boolean isInsertAsync(final DbPropertyDescriptor desc) throws Exception {
		return false;
	}

	/** When insert is mark async, this function permit to create or update the data
	 * @param localId Local ID of the current table
	 * @param desc Property descriptor that is updated.
	 * @param data Data that might be inserted.
	 * @param actions Asynchronous action to do after main request. */
	default void asyncInsert(
			final DBAccessMongo ioDb,
			final Class<?> clazz,
			final Object localId,
			final DbPropertyDescriptor desc,
			final Object data,
			final List<LazyGetter> actions,
			final QueryOptions options) throws Exception {

	}

	/** Some action must be done asynchronously for update or remove element
	 * @param desc
	 * @return */
	default boolean isUpdateAsync(final DbPropertyDescriptor desc) throws Exception {
		return false;
	}

	/** When insert is mark async, this function permit to create or update the data
	 * @param localId Local ID of the current table
	 * @param desc Property descriptor that is updated.
	 * @param data Data that might be inserted.
	 * @param actions Asynchronous action to do after main request. */
	default void asyncUpdate(
			final DBAccessMongo ioDb,
			final Object previousData,
			final Object localId,
			final DbPropertyDescriptor desc,
			final Object data,
			final List<LazyGetter> actions,
			final QueryOptions options) throws Exception {

	}

	/** Some annotation need to collect data before updating the current values
	 * @param desc
	 * @return */
	default boolean isPreviousDataNeeded(final DbPropertyDescriptor desc) {
		return false;
	}

	default boolean asDeleteAction(final DbPropertyDescriptor desc) {
		return false;
	}

	default void onDelete(
			final DBAccessMongo ioDb,
			final Class<?> clazz,
			final DbPropertyDescriptor desc,
			final List<Object> previousData,
			final List<LazyGetter> actions) throws Exception {

	}

	default void drop(final DBAccessMongo ioDb, final String tableName, final DbPropertyDescriptor desc,
			final QueryOptions options)
			throws Exception {

	}

	default void cleanAll(
			final DBAccessMongo ioDb,
			final String tableName,
			final DbPropertyDescriptor desc,
			final QueryOptions options) throws Exception {

	}

}
