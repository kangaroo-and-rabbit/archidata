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
import org.kar.archidata.dataAccess.CountInOut;
import org.kar.archidata.dataAccess.DataAccess;
import org.kar.archidata.dataAccess.DataAccessAddOn;
import org.kar.archidata.dataAccess.DataFactory;
import org.kar.archidata.dataAccess.LazyGetter;
import org.kar.archidata.dataAccess.QueryOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.constraints.NotNull;

// TODO: maybe deprecated ==> use DataJson instead...
public class AddOnSQLTableExternalForeinKeyAsList implements DataAccessAddOn {
	static final Logger LOGGER = LoggerFactory.getLogger(AddOnManyToMany.class);
	static final String SEPARATOR = "-";

	/** Convert the list if external id in a string '-' separated
	 * @param ids List of value (null are removed)
	 * @return '-' string separated */
	protected static String getStringOfIds(final List<Long> ids) {
		final List<Long> tmp = new ArrayList<>(ids);
		return tmp.stream().map(String::valueOf).collect(Collectors.joining(SEPARATOR));
	}

	@Override
	public Class<?> getAnnotationClass() {
		return SQLTableExternalForeinKeyAsList.class;
	}

	@Override
	public String getSQLFieldType(final Field field) throws Exception {
		final String fieldName = AnnotationTools.getFieldName(field);
		try {
			return DataFactory.convertTypeInSQL(String.class, fieldName);
		} catch (final Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public boolean isCompatibleField(final Field field) {
		final SQLTableExternalForeinKeyAsList decorators = field.getDeclaredAnnotation(SQLTableExternalForeinKeyAsList.class);
		return decorators != null;
	}

	@Override
	public void insertData(final PreparedStatement ps, final Field field, final Object rootObject, final CountInOut iii) throws SQLException, IllegalArgumentException, IllegalAccessException {
		final Object data = field.get(rootObject);
		iii.inc();
		if (data == null) {
			ps.setNull(iii.value, Types.BIGINT);
		} else {
			@SuppressWarnings("unchecked")
			final String dataTmp = getStringOfIds((List<Long>) data);
			ps.setString(iii.value, dataTmp);
		}
	}

	@Override
	public boolean canInsert(final Field field) {
		return true;
	}

	@Override
	public boolean isInsertAsync(final Field field) throws Exception {
		return false;
	}

	@Override
	public boolean canRetrieve(final Field field) {
		return true;
	}

	@Override
	public void generateQuery(//
			@NotNull final String tableName, //
			@NotNull final String primaryKey, //
			@NotNull final Field field, //
			@NotNull final StringBuilder querySelect, //
			@NotNull final StringBuilder query, //
			@NotNull final String name, //
			@NotNull final CountInOut count, //
			final QueryOptions options//
	) {
		count.inc();
		querySelect.append(" ");
		querySelect.append(tableName);
		querySelect.append(".");
		querySelect.append(name);
	}

	@Override
	public void fillFromQuery(final ResultSet rs, final Field field, final Object data, final CountInOut count, final QueryOptions options, final List<LazyGetter> lazyCall)
			throws SQLException, IllegalArgumentException, IllegalAccessException {
		final List<Long> idList = DataAccess.getListOfIds(rs, count.value, SEPARATOR);
		field.set(data, idList);
		count.inc();
	}

	@Override
	public void createTables(//
			final String tableName, //
			final Field primaryField, //
			final Field field, //
			final StringBuilder mainTableBuilder, //
			final List<String> preActionList, //
			final List<String> postActionList, //
			final boolean createIfNotExist, //
			final boolean createDrop, //
			final int fieldId //
	) throws Exception {
		// TODO Auto-generated method stub

		DataFactory.createTablesSpecificType(tableName, field, mainTableBuilder, preActionList, postActionList, createIfNotExist, createDrop, fieldId, String.class);
	}
}
