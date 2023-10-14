package org.kar.archidata.sqlWrapper;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

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
	
	int insertData(PreparedStatement ps, Object data, int iii) throws SQLException;
	
	// External mean that the type of the object is absolutely not obvious...
	boolean isExternal();
	
	int generateQuerry(String tableName, Field elem, StringBuilder querry, String name, List<StateLoad> autoClasify);
	
	int fillFromQuerry(ResultSet rs, Field elem, Object data, int count) throws SQLException, IllegalArgumentException, IllegalAccessException;
	
	boolean canUpdate();
	
	void createTables(String tableName, Field elem, StringBuilder mainTableBuilder, List<String> ListOtherTables, boolean createIfNotExist, boolean createDrop, int fieldId) throws Exception;
	
}
