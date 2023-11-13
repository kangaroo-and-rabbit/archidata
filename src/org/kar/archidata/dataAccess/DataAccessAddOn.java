package org.kar.archidata.dataAccess;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import jakarta.validation.constraints.NotNull;

public interface DataAccessAddOn {
	/** Get the Class of the declaration annotation
	 * @return The annotation class */
	Class<?> getAnnotationClass();

	/** Get the SQL type that is needed to declare for the specific Field Type.
	 * @param elem Field to declare.
	 * @return SQL type to create. */
	String getSQLFieldType(Field elem) throws Exception;

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
	void insertData(PreparedStatement ps, final Field field, Object data, CountInOut iii) throws Exception, SQLException, IllegalArgumentException, IllegalAccessException;

	// Element can insert in the single request
	boolean canInsert(final Field field);

	// Element can be retrieve with the specific mode
	boolean canRetrieve(final Field field);

	void generateQuerry(@NotNull String tableName, @NotNull Field field, @NotNull final StringBuilder querrySelect, @NotNull final StringBuilder querry, @NotNull String name,
			@NotNull CountInOut count, QueryOptions options) throws Exception;

	// Return the number of colomn read
	void fillFromQuerry(ResultSet rs, Field field, Object data, CountInOut count, QueryOptions options, final List<LazyGetter> lazyCall)
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
	void createTables(String tableName, Field field, StringBuilder mainTableBuilder, List<String> preActionList, List<String> postActionList, boolean createIfNotExist, boolean createDrop, int fieldId)
			throws Exception;

}
