package org.kar.archidata.dataAccess.addOn;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.kar.archidata.annotation.AnnotationTools;
import org.kar.archidata.annotation.addOn.SQLTableExternalForeinKeyAsList;
import org.kar.archidata.dataAccess.DataAccess;
import org.kar.archidata.dataAccess.DataAccessAddOn;
import org.kar.archidata.dataAccess.DataFactory;
import org.kar.archidata.dataAccess.QueryOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.constraints.NotNull;

public class AddOnSQLTableExternalForeinKeyAsList implements DataAccessAddOn {
	static final Logger LOGGER = LoggerFactory.getLogger(AddOnManyToMany.class);
	static final String SEPARATOR = "-";

	/**
	 * Convert the list if external id in a string '-' separated
	 * @param ids List of value (null are removed)
	 * @return '-' string separated
	 */
	protected static String getStringOfIds(final List<Long> ids) {
		final List<Long> tmp = new ArrayList<>(ids);
		return tmp.stream().map(String::valueOf).collect(Collectors.joining(SEPARATOR));
	}

	@Override
	public Class<?> getAnnotationClass() {
		return SQLTableExternalForeinKeyAsList.class;
	}

	@Override
	public String getSQLFieldType(final Field elem) throws Exception {
		final String fieldName = AnnotationTools.getFieldName(elem);
		try {
			return DataFactory.convertTypeInSQL(String.class, fieldName);
		} catch (final Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public boolean isCompatibleField(final Field elem) {
		final SQLTableExternalForeinKeyAsList decorators = elem.getDeclaredAnnotation(SQLTableExternalForeinKeyAsList.class);
		return decorators != null;
	}

	@Override
	public int insertData(final PreparedStatement ps, final Object data, int iii) throws SQLException {
		if (data == null) {
			ps.setNull(iii++, Types.BIGINT);
		} else {
			@SuppressWarnings("unchecked")
			final String dataTmp = getStringOfIds((List<Long>) data);
			ps.setString(iii++, dataTmp);
		}
		return iii++;
	}

	@Override
	public boolean canInsert() {
		return false;
	}

	@Override
	public boolean canRetrieve(final Field field) {
		return false;
	}

	@Override
	public int generateQuerry(@NotNull final String tableName, @NotNull final Field elem, @NotNull final StringBuilder querry, @NotNull final String name, @NotNull final int elemCount,
			final QueryOptions options) {
		querry.append(" ");
		querry.append(tableName);
		querry.append(".");
		querry.append(name);
		return 1;
	}

	@Override
	public int fillFromQuerry(final ResultSet rs, final Field elem, final Object data, final int count, final QueryOptions options)
			throws SQLException, IllegalArgumentException, IllegalAccessException {
		final List<Long> idList = DataAccess.getListOfIds(rs, count, SEPARATOR);
		elem.set(data, idList);
		return 1;
	}

	@Override
	public boolean canUpdate() {
		return true;
	}

	@Override
	public void createTables(final String tableName, final Field elem, final StringBuilder mainTableBuilder, final List<String> preActionList, final List<String> postActionList,
			final boolean createIfNotExist, final boolean createDrop, final int fieldId) throws Exception {
		// TODO Auto-generated method stub

		DataFactory.createTablesSpecificType(tableName, elem, mainTableBuilder, preActionList, postActionList, createIfNotExist, createDrop, fieldId, String.class);
	}
}
