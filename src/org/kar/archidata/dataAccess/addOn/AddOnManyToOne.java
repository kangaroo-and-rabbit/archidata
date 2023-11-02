package org.kar.archidata.dataAccess.addOn;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import org.kar.archidata.annotation.AnnotationTools;
import org.kar.archidata.dataAccess.DataAccess;
import org.kar.archidata.dataAccess.DataAccessAddOn;
import org.kar.archidata.dataAccess.DataFactory;
import org.kar.archidata.dataAccess.QueryOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotNull;

public class AddOnManyToOne implements DataAccessAddOn {
	static final Logger LOGGER = LoggerFactory.getLogger(AddOnManyToMany.class);
	
	@Override
	public Class<?> getAnnotationClass() {
		return ManyToOne.class;
	}
	
	@Override
	public String getSQLFieldType(final Field elem) throws Exception {
		final String fieldName = AnnotationTools.getFieldName(elem);
		try {
			return DataFactory.convertTypeInSQL(Long.class, fieldName);
		} catch (final Exception e) {
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
	public int insertData(final PreparedStatement ps, final Field field, final Object rootObject, int iii) throws Exception {
		final Object data = field.get(rootObject);
		if (data == null) {
			ps.setNull(iii++, Types.BIGINT);
		} else if (field.getType() == Long.class) {
			final Long dataLong = (Long) data;
			ps.setLong(iii++, dataLong);
		} else {
			final Field idField = AnnotationTools.getFieldOfId(field.getType());
			final Object uid = idField.get(data);
			if (uid == null) {
				ps.setNull(iii++, Types.BIGINT);
				throw new Exception("Not implemented adding subClasses ==> add it manualy before...");
			} else {
				final Long dataLong = (Long) uid;
				ps.setLong(iii++, dataLong);
			}
		}
		return iii++;
	}
	
	@Override
	public boolean canInsert(final Field field) {
		if (field.getType() == Long.class) {
			return true;
		}
		final ManyToOne decorators = field.getDeclaredAnnotation(ManyToOne.class);
		if (field.getType() == decorators.targetEntity()) {
			return true;
		}
		return false;
	}

	@Override
	public boolean canRetrieve(final Field field) {
		if (field.getType() == Long.class) {
			return true;
		}
		final ManyToOne decorators = field.getDeclaredAnnotation(ManyToOne.class);
		if (field.getType() == decorators.targetEntity()) {
			return true;
		}
		return false;
	}
	
	@Override
	public int generateQuerry(@NotNull final String tableName, @NotNull final Field field, @NotNull final StringBuilder querry, @NotNull final String name, @NotNull final int elemCount,
			final QueryOptions options) throws Exception {
		if (field.getType() == Long.class) {
			querry.append(" ");
			querry.append(tableName);
			querry.append(".");
			querry.append(name);
			return elemCount + 1;
		}
		final ManyToOne decorators = field.getDeclaredAnnotation(ManyToOne.class);
		if (field.getType() == decorators.targetEntity()) {
			return DataAccess.generateSelectField(querry, field.getType(), options, elemCount);
		}
		return elemCount;
	}
	
	@Override
	public int fillFromQuerry(final ResultSet rs, final Field field, final Object data, final int count, final QueryOptions options)
			throws SQLException, IllegalArgumentException, IllegalAccessException {
		final Long foreignKey = rs.getLong(count);
		if (rs.wasNull()) {
			return 0;
		}
		field.set(data, foreignKey);
		return 1;
	}
	
	// TODO : refacto this table to manage a generic table with dynamic name to be serializable with the default system
	@Override
	public void createTables(final String tableName, final Field field, final StringBuilder mainTableBuilder, final List<String> preActionList, final List<String> postActionList,
			final boolean createIfNotExist, final boolean createDrop, final int fieldId) throws Exception {
		DataFactory.createTablesSpecificType(tableName, field, mainTableBuilder, preActionList, postActionList, createIfNotExist, createDrop, fieldId, Long.class);
	}
}
