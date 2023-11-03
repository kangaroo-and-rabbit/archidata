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
import org.kar.archidata.dataAccess.CountInOut;
import org.kar.archidata.dataAccess.DataAccessAddOn;
import org.kar.archidata.dataAccess.DataFactory;
import org.kar.archidata.dataAccess.LazyGetter;
import org.kar.archidata.dataAccess.QueryOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotNull;

public class AddOnOneToMany implements DataAccessAddOn {
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
		return OneToMany.class;
	}

	@Override
	public String getSQLFieldType(final Field field) throws Exception {
		final String fieldName = AnnotationTools.getFieldName(field);
		try {
			return DataFactory.convertTypeInSQL(Long.class, fieldName);
		} catch (final Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public boolean isCompatibleField(final Field field) {
		final OneToMany decorators = field.getDeclaredAnnotation(OneToMany.class);
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
		return false;
	}
	
	@Override
	public boolean canRetrieve(final Field field) {
		return false;
	}

	@Override
	public void generateQuerry(@NotNull final String tableName, @NotNull final Field field, @NotNull final StringBuilder querrySelect, @NotNull final StringBuilder querry, @NotNull final String name,
			@NotNull final CountInOut elemCount, final QueryOptions options) {
		querrySelect.append(" ");
		querrySelect.append(tableName);
		querrySelect.append(".");
		querrySelect.append(name);
		elemCount.inc();
	}

	@Override
	public void fillFromQuerry(final ResultSet rs, final Field field, final Object data, final CountInOut count, final QueryOptions options, final List<LazyGetter> lazyCall)
			throws SQLException, IllegalArgumentException, IllegalAccessException {
		final Long foreignKey = rs.getLong(count.value);
		count.inc();
		if (!rs.wasNull()) {
			
			field.set(data, foreignKey);
		}
	}

	// TODO : refacto this table to manage a generic table with dynamic name to be serializable with the default system
	@Override
	public void createTables(final String tableName, final Field field, final StringBuilder mainTableBuilder, final List<String> preActionList, final List<String> postActionList,
			final boolean createIfNotExist, final boolean createDrop, final int fieldId) throws Exception {
		DataFactory.createTablesSpecificType(tableName, field, mainTableBuilder, preActionList, postActionList, createIfNotExist, createDrop, fieldId, Long.class);
	}
}
