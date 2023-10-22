package org.kar.archidata.sqlWrapper;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import jakarta.validation.constraints.NotNull;

public interface SqlWrapperAddOn {
	/**
	 * Get the Class of the declaration annotation
	 * @return The annotation class
	 */
	Class<?> getAnnotationClass();
	
	/**
	 * Get the SQL type that is needed to declare for the specific Field Type.
	 * @param elem Field to declare.
	 * @return SQL type to create.
	 */
	String getSQLFieldType(Field elem);
	
	/**
	 * Check if the field is manage by the local add-on
	 * @param elem Field to inspect.
	 * @return True of the field is manage by the current Add-on.
	 */
	boolean isCompatibleField(Field elem);
	
	/**
	 * Insert data in the specific field (the field must be in the current db, otherwiise it does not work at all.
	 * @param ps DB statement interface.
	 * @param data The date to inject.
	 * @param iii The index of injection
	 * @return the new index of injection in case of multiple value management
	 * @throws SQLException
	 */
	int insertData(PreparedStatement ps, Object data, int iii) throws SQLException;
	
	// External mean that the type of the object is absolutely not obvious...
	boolean isExternal();
	
	int generateQuerry(@NotNull String tableName, @NotNull Field elem, @NotNull StringBuilder querry, @NotNull String name, @NotNull int elemCount, QuerryOptions options);
	
	// Return the number of colomn read
	int fillFromQuerry(ResultSet rs, Field elem, Object data, int count, QuerryOptions options) throws SQLException, IllegalArgumentException, IllegalAccessException;
	
	boolean canUpdate();
	
	/**
	 * Create associated table of the specific element.
	 * @param tableName
	 * @param elem
	 * @param mainTableBuilder
	 * @param ListOtherTables
	 * @param createIfNotExist
	 * @param createDrop
	 * @param fieldId
	 * @throws Exception
	 */
	void createTables(String tableName, Field elem, StringBuilder mainTableBuilder, List<String> preActionList, List<String> postActionList, boolean createIfNotExist, boolean createDrop, int fieldId)
			throws Exception;
	
}
