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
import org.kar.archidata.sqlWrapper.StateLoad;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddOnSQLTableExternalForeinKeyAsList implements SqlWrapperAddOn {
	static final Logger LOGGER = LoggerFactory.getLogger(AddOnManyToMany.class);
	
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
		return tmp.stream().map(x -> String.valueOf(x)).collect(Collectors.joining("-"));
	}
	
	/**
	 * extract a list of "-" separated element from a SQL input data.
	 * @param rs Result Set of the BDD
	 * @param iii Id in the result set
	 * @return The list  of Long value
	 * @throws SQLException if an error is generated in the sql request.
	 */
	protected static List<Long> getListOfIds(ResultSet rs, int iii) throws SQLException {
		String trackString = rs.getString(iii);
		if (rs.wasNull()) {
			return null;
		}
		List<Long> out = new ArrayList<>();
		String[] elements = trackString.split("-");
		for (String elem : elements) {
			Long tmp = Long.parseLong(elem);
			out.add(tmp);
		}
		return out;
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
	public int generateQuerry(String tableName, Field elem, StringBuilder querry, String name, List<StateLoad> autoClasify, QuerryOptions options) {
		autoClasify.add(StateLoad.ARRAY);
		querry.append(" ");
		querry.append(tableName);
		querry.append(".");
		querry.append(name);
		return 1;
	}
	
	@Override
	public int fillFromQuerry(ResultSet rs, Field elem, Object data, int count, QuerryOptions options) throws SQLException, IllegalArgumentException, IllegalAccessException {
		List<Long> idList = getListOfIds(rs, count);
		elem.set(data, idList);
		return 1;
	}
	
	@Override
	public boolean canUpdate() {
		return true;
	}
	
	@Override
	public void createTables(String tableName, Field elem, StringBuilder mainTableBuilder, List<String> ListOtherTables, boolean createIfNotExist, boolean createDrop, int fieldId) throws Exception {
		// TODO Auto-generated method stub
		
		SqlWrapper.createTablesSpecificType(tableName, elem, mainTableBuilder, ListOtherTables, createIfNotExist, createDrop, fieldId, String.class);
	}
}
