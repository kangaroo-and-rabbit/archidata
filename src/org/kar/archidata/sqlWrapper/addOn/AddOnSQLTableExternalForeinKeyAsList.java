package org.kar.archidata.sqlWrapper.addOn;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.kar.archidata.annotation.addOn.SQLTableExternalForeinKeyAsList;
import org.kar.archidata.sqlWrapper.QuerryOptions;
import org.kar.archidata.sqlWrapper.SqlWrapper;
import org.kar.archidata.sqlWrapper.SqlWrapperAddOn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.constraints.NotNull;

public class AddOnSQLTableExternalForeinKeyAsList implements SqlWrapperAddOn {
	static final Logger LOGGER = LoggerFactory.getLogger(AddOnManyToMany.class);
	static final String SEPARATOR = "-";
	
	/**
	 * Convert the list if external id in a string '-' separated
	 * @param ids List of value (null are removed)
	 * @return '-' string separated
	 */
	protected static String getStringOfIds(List<Long> ids) {
		List<Long> tmp = new ArrayList<>();
		for (Long elem : ids) {
			tmp.add(elem);
		}
		return tmp.stream().map(x -> String.valueOf(x)).collect(Collectors.joining(SEPARATOR));
	}
	
	@Override
	public Class<?> getAnnotationClass() {
		return SQLTableExternalForeinKeyAsList.class;
	}
	
	public String getSQLFieldType(Field elem) {
		try {
			return SqlWrapper.convertTypeInSQL(String.class);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	public boolean isCompatibleField(Field elem) {
		SQLTableExternalForeinKeyAsList decorators = elem.getDeclaredAnnotation(SQLTableExternalForeinKeyAsList.class);
		return decorators != null;
	}
	
	public int insertData(PreparedStatement ps, Object data, int iii) throws SQLException {
		if (data == null) {
			ps.setNull(iii++, Types.BIGINT);
		} else {
			@SuppressWarnings("unchecked")
			String dataTmp = getStringOfIds((List<Long>) data);
			ps.setString(iii++, dataTmp);
		}
		return iii++;
	}
	
	@Override
	public boolean isExternal() {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public int generateQuerry(@NotNull String tableName, @NotNull Field elem, @NotNull StringBuilder querry, @NotNull String name, @NotNull int elemCount, QuerryOptions options) {
		querry.append(" ");
		querry.append(tableName);
		querry.append(".");
		querry.append(name);
		return 1;
	}
	
	@Override
	public int fillFromQuerry(ResultSet rs, Field elem, Object data, int count, QuerryOptions options) throws SQLException, IllegalArgumentException, IllegalAccessException {
		List<Long> idList = SqlWrapper.getListOfIds(rs, count, SEPARATOR);
		elem.set(data, idList);
		return 1;
	}
	
	@Override
	public boolean canUpdate() {
		return true;
	}
	
	@Override
	public void createTables(String tableName, Field elem, StringBuilder mainTableBuilder, List<String> preActionList, List<String> postActionList, boolean createIfNotExist, boolean createDrop,
			int fieldId) throws Exception {
		// TODO Auto-generated method stub
		
		SqlWrapper.createTablesSpecificType(tableName, elem, mainTableBuilder, preActionList, postActionList, createIfNotExist, createDrop, fieldId, String.class);
	}
}
