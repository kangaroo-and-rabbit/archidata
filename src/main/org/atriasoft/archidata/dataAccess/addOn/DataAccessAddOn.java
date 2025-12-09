package org.atriasoft.archidata.dataAccess.addOn;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.List;

import org.atriasoft.archidata.dataAccess.DBAccessMongo;
import org.atriasoft.archidata.dataAccess.LazyGetter;
import org.atriasoft.archidata.dataAccess.QueryOptions;
import org.bson.Document;

public interface DataAccessAddOn {
	/** Get the Class of the declaration annotation
	 * @return The annotation class */
	Class<?> getAnnotationClass();

	/** Check if the field is manage by the local add-on
	 * @param elem Field to inspect.
	 * @return True of the field is manage by the current Add-on. */
	boolean isCompatibleField(Field elem);

	void insertData(
			final DBAccessMongo ioDb,
			final Field field,
			final Object rootObject,
			final QueryOptions options,
			final Document docSet,
			final Document docUnSet) throws Exception;

	/** Element can insert in the single request
	 * @param field
	 * @return */
	default boolean canInsert(final Field field) {
		return false;
	}

	/** Element can be retrieve with the specific mode
	 * @param field
	 * @return */
	default boolean canRetrieve(final Field field) {
		return false;
	}

	// Return the number of colomn read
	void fillFromDoc(
			final DBAccessMongo ioDb,
			Document doc,
			Field field,
			Object data,
			QueryOptions options,
			final List<LazyGetter> lazyCall)
			throws Exception, SQLException, IllegalArgumentException, IllegalAccessException;

	/** Some action must be done asynchronously for update or remove element
	 * @param field
	 * @return */
	default boolean isInsertAsync(final Field field) throws Exception {
		return false;
	}

	/** When insert is mark async, this function permit to create or update the data
	 * @param localId Local ID of the current table
	 * @param field Field that is updated.
	 * @param data Data that might be inserted.
	 * @param actions Asynchronous action to do after main request. */
	default void asyncInsert(
			final DBAccessMongo ioDb,
			final Class<?> clazz,
			final Object localId,
			final Field field,
			final Object data,
			final List<LazyGetter> actions,
			final QueryOptions options) throws Exception {

	}

	/** Some action must be done asynchronously for update or remove element
	 * @param field
	 * @return */
	default boolean isUpdateAsync(final Field field) throws Exception {
		return false;
	}

	/** When insert is mark async, this function permit to create or update the data
	 * @param localId Local ID of the current table
	 * @param field Field that is updated.
	 * @param data Data that might be inserted.
	 * @param actions Asynchronous action to do after main request. */
	default void asyncUpdate(
			final DBAccessMongo ioDb,
			final Object previousData,
			final Object localId,
			final Field field,
			final Object data,
			final List<LazyGetter> actions,
			final QueryOptions options) throws Exception {

	}

	/** Some annotation need to collect data before updating the current values
	 * @param field
	 * @return */
	default boolean isPreviousDataNeeded(final Field field) {
		return false;
	}

	default boolean asDeleteAction(final Field field) {
		return false;
	}

	default void onDelete(
			final DBAccessMongo ioDb,
			final Class<?> clazz,
			final Field field,
			final List<Object> previousData,
			final List<LazyGetter> actions) throws Exception {

	}

	default void drop(final DBAccessMongo ioDb, final String tableName, final Field field, final QueryOptions options)
			throws Exception {

	}

	default void cleanAll(
			final DBAccessMongo ioDb,
			final String tableName,
			final Field field,
			final QueryOptions options) throws Exception {

	}

}
