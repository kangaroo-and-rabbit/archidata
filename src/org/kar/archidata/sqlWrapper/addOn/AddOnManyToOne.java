package org.kar.archidata.sqlWrapper.addOn;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.kar.archidata.sqlWrapper.QuerryOptions;
import org.kar.archidata.sqlWrapper.SqlWrapper;
import org.kar.archidata.sqlWrapper.SqlWrapperAddOn;
import org.kar.archidata.sqlWrapper.StateLoad;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.ManyToOne;

public class AddOnManyToOne implements SqlWrapperAddOn {
	static final Logger LOGGER = LoggerFactory.getLogger(AddOnManyToMany.class);
	
	/**
	 * Convert the list if external id in a string '-' separated
	 * @param ids List of value (null are removed)
	 * @return '-' string separated
	 */
	protected static String getStringOfIds(final List<Long> ids) {
		final List<Long> tmp = new ArrayList<>(ids);
		return tmp.stream().map(String::valueOf).collect(Collectors.joining("-"));
	}
	
	/**
	 * extract a list of "-" separated element from a SQL input data.
	 * @param rs Result Set of the BDD
	 * @param iii Id in the result set
	 * @return The list  of Long value
	 * @throws SQLException if an error is generated in the sql request.
	 */
	protected static List<Long> getListOfIds(final ResultSet rs, final int iii) throws SQLException {
		final String trackString = rs.getString(iii);
		if (rs.wasNull()) {
			return null;
		}
		final List<Long> out = new ArrayList<>();
		final String[] elements = trackString.split("-");
		for (final String elem : elements) {
			final Long tmp = Long.parseLong(elem);
			out.add(tmp);
		}
		return out;
	}
	
	@Override
	public Class<?> getAnnotationClass() {
		return ManyToOne.class;
	}
	
	@Override
	public String getSQLFieldType(final Field elem) {
		try {
			return SqlWrapper.convertTypeInSQL(Long.class);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	@Override
	public boolean isCompatibleField(final Field elem) {
		final ManyToOne decorators = elem.getDeclaredAnnotation(ManyToOne.class);
		return decorators != null;
	}
	
	@Override
	public int insertData(final PreparedStatement ps, final Object data, int iii) throws SQLException {
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
	public int generateQuerry(final String tableName, final Field elem, final StringBuilder querry, final String name, final List<StateLoad> autoClasify, QuerryOptions options) {
		autoClasify.add(StateLoad.NORMAL);
		querry.append(" ");
		querry.append(tableName);
		querry.append(".");
		querry.append(name);
		return 1;
	}
	
	@Override
	public int fillFromQuerry(final ResultSet rs, final Field elem, final Object data, final int count, QuerryOptions options) throws SQLException, IllegalArgumentException, IllegalAccessException {
		Long foreignKey = rs.getLong(count);
		if (rs.wasNull()) {
			return 0;
		}
		elem.set(data, foreignKey);
		return 1;
	}
	
	@Override
	public boolean canUpdate() {
		return true;
	}
	
	// TODO : refacto this table to manage a generic table with dynamic name to be serializable with the default system
	@Override
	public void createTables(final String tableName, final Field elem, final StringBuilder mainTableBuilder, final List<String> ListOtherTables, final boolean createIfNotExist,
			final boolean createDrop, final int fieldId) throws Exception {
		SqlWrapper.createTablesSpecificType(tableName, elem, mainTableBuilder, ListOtherTables, createIfNotExist, createDrop, fieldId, Long.class);
	}
}
