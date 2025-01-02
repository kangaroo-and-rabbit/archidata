package org.kar.archidata.dataAccess.addOnSQL;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.kar.archidata.dataAccess.CountInOut;
import org.kar.archidata.dataAccess.DBAccessSQL;
import org.kar.archidata.dataAccess.LazyGetter;
import org.kar.archidata.dataAccess.QueryOptions;

import jakarta.validation.constraints.NotNull;

public interface DataAccessAddOn {
	/** Get the Class of the declaration annotation
	 * @return The annotation class */
	Class<?> getAnnotationClass();

	/** Get the SQL type that is needed to declare for the specific Field Type.
	 * @param elem Field to declare.
	 * @return SQL type to create. */
	default String getSQLFieldType(final Field elem, final QueryOptions options) throws Exception {
		return null;
	}

	/** Check if the field is manage by the local add-on
	 * @param elem Field to inspect.
	 * @return True of the field is manage by the current Add-on. */
	boolean isCompatibleField(Field elem);

	/** Insert data in the specific field (the field must be in the current db, otherwiise it does not work at all.
	 * @param ps DB statement interface.
	 * @param data The date to inject.
	 * @param iii The index of injection
	 * @return the new index of injection in case of multiple value management
	 * @throws SQLException */
	void insertData(final DBAccessSQL ioDb, PreparedStatement ps, final Field field, Object data, CountInOut iii)
			throws Exception, SQLException, IllegalArgumentException, IllegalAccessException;

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

	void generateQuery(
			@NotNull String tableName,
			@NotNull final String primaryKey,
			@NotNull Field field,
			@NotNull final StringBuilder querySelect,
			@NotNull final StringBuilder query,
			@NotNull String name,
			@NotNull CountInOut count,
			QueryOptions options) throws Exception;

	// Return the number of colomn read
	void fillFromQuery(
			final DBAccessSQL ioDb,
			ResultSet rs,
			Field field,
			Object data,
			CountInOut count,
			QueryOptions options,
			final List<LazyGetter> lazyCall)
			throws Exception, SQLException, IllegalArgumentException, IllegalAccessException;

	/** Create associated table of the specific element.
	 * @param tableName
	 * @param elem
	 * @param mainTableBuilder
	 * @param ListOtherTables
	 * @param createIfNotExist
	 * @param createDrop
	 * @param fieldId
	 * @throws Exception */
	default void createTables(
			final String tableName,
			final Field primaryField,
			final Field field,
			final StringBuilder mainTableBuilder,
			final List<String> preActionList,
			final List<String> postActionList,
			final boolean createIfNotExist,
			final boolean createDrop,
			final int fieldId,
			final QueryOptions options) throws Exception {

	}

	/** Some action must be done asynchronously for update or remove element
	 * @param field
	 * @return */
	default boolean isInsertAsync(final Field field) throws Exception {
		return false;
	}

	/** When insert is mark async, this function permit to create or update the data
	 * @param tableName Name of the Table.
	 * @param localId Local ID of the current table
	 * @param field Field that is updated.
	 * @param data Data that might be inserted.
	 * @param actions Asynchronous action to do after main request. */
	default void asyncInsert(
			final DBAccessSQL ioDb,
			final String tableName,
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
	 * @param tableName Name of the Table.
	 * @param localId Local ID of the current table
	 * @param field Field that is updated.
	 * @param data Data that might be inserted.
	 * @param actions Asynchronous action to do after main request. */
	default void asyncUpdate(
			final DBAccessSQL ioDb,
			final String tableName,
			final Object localId,
			final Field field,
			final Object data,
			final List<LazyGetter> actions,
			final QueryOptions options) throws Exception {

	}

	default void drop(final DBAccessSQL ioDb, final String tableName, final Field field, final QueryOptions options)
			throws Exception {

	}

	default void cleanAll(final DBAccessSQL ioDb, final String tableName, final Field field, final QueryOptions options)
			throws Exception {

	}

}
