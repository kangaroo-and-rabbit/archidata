package org.kar.archidata.dataAccess.addOn;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Types;
import java.util.List;
import java.util.UUID;

import org.kar.archidata.annotation.AnnotationTools;
import org.kar.archidata.dataAccess.CountInOut;
import org.kar.archidata.dataAccess.DataAccess;
import org.kar.archidata.dataAccess.DataAccessAddOn;
import org.kar.archidata.dataAccess.DataFactory;
import org.kar.archidata.dataAccess.LazyGetter;
import org.kar.archidata.dataAccess.QueryOptions;
import org.kar.archidata.exception.DataAccessException;
import org.kar.archidata.tools.UuidUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.FetchType;
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
	public void insertData(final PreparedStatement ps, final Field field, final Object rootObject, final CountInOut iii) throws Exception {
		final Object data = field.get(rootObject);
		if (data == null) {
			ps.setNull(iii.value, Types.BIGINT);
		} else if (field.getType() == Long.class) {
			final Long dataLong = (Long) data;
			ps.setLong(iii.value, dataLong);
		} else {
			final Field idField = AnnotationTools.getFieldOfId(field.getType());
			final Object uid = idField.get(data);
			if (uid == null) {
				ps.setNull(iii.value, Types.BIGINT);
				throw new DataAccessException("Not implemented adding subClasses ==> add it manualy before...");
			} else {
				final Long dataLong = (Long) uid;
				ps.setLong(iii.value, dataLong);
			}
		}
		iii.inc();
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
	public boolean isInsertAsync(final Field field) throws Exception {
		return false;
	}

	@Override
	public boolean canRetrieve(final Field field) {
		if (field.getType() == Long.class
				|| field.getType() == Integer.class
				|| field.getType() == Short.class
				|| field.getType() == String.class
				|| field.getType() == UUID.class) {
			return true;
		}
		final ManyToOne decorators = field.getDeclaredAnnotation(ManyToOne.class);
		if (field.getType() == decorators.targetEntity()) {
			return true;
		}
		return false;
	}

	@Override
	public void generateQuerry(@NotNull final String tableName, @NotNull final Field field, @NotNull final StringBuilder querrySelect, @NotNull final StringBuilder querry, @NotNull final String name,
			@NotNull final CountInOut elemCount, final QueryOptions options) throws Exception {
		if (field.getType() == Long.class
				|| field.getType() == Integer.class
				|| field.getType() == Short.class
				|| field.getType() == String.class
				|| field.getType() == UUID.class) {
			querrySelect.append(" ");
			querrySelect.append(tableName);
			querrySelect.append(".");
			querrySelect.append(name);
			elemCount.inc();
			return;
		}
		final ManyToOne decorators = field.getDeclaredAnnotation(ManyToOne.class);
		if (field.getType() == decorators.targetEntity()) {
			if (decorators.fetch() == FetchType.EAGER) {
				// TODO: rework this to have a lazy mode ...
				DataAccess.generateSelectField(querrySelect, querry, field.getType(), options, elemCount);
				final Class<?> subType = field.getType();
				final String subTableName = AnnotationTools.getTableName(subType);
				final Field idField = AnnotationTools.getFieldOfId(subType);
				querry.append("LEFT OUTER JOIN `");
				querry.append(subTableName);
				querry.append("` ON ");
				querry.append(subTableName);
				querry.append(".");
				querry.append(AnnotationTools.getFieldName(idField));
				querry.append(" = ");
				querry.append(tableName);
				querry.append(".");
				querry.append(AnnotationTools.getFieldName(field));
			} else {
				querrySelect.append(" ");
				querrySelect.append(tableName);
				querrySelect.append(".");
				querrySelect.append(name);
				elemCount.inc();
				return;
			}
		}

		/* SELECT k.id, r.id FROM `right` k LEFT OUTER JOIN `rightDescription` r ON k.rightDescriptionId=r.id */
	}

	@Override
	public void fillFromQuerry(final ResultSet rs, final Field field, final Object data, final CountInOut count, final QueryOptions options, final List<LazyGetter> lazyCall) throws Exception {
		if (field.getType() == Long.class) {
			final Long foreignKey = rs.getLong(count.value);
			count.inc();
			if (!rs.wasNull()) {
				field.set(data, foreignKey);
			}
			return;
		}
		if (field.getType() == Integer.class) {
			final Integer foreignKey = rs.getInt(count.value);
			count.inc();
			if (!rs.wasNull()) {
				field.set(data, foreignKey);
			}
			return;
		}
		if (field.getType() == Short.class) {
			final Short foreignKey = rs.getShort(count.value);
			count.inc();
			if (!rs.wasNull()) {
				field.set(data, foreignKey);
			}
			return;
		}
		if (field.getType() == String.class) {
			final String foreignKey = rs.getString(count.value);
			count.inc();
			if (!rs.wasNull()) {
				field.set(data, foreignKey);
			}
			return;
		}
		if (field.getType() == UUID.class) {
			final byte[] tmp = rs.getBytes(count.value);
			count.inc();
			if (!rs.wasNull()) {
				final UUID foreignKey = UuidUtils.asUuid(tmp);
				field.set(data, foreignKey);
			}
			return;
		}
		final Class<?> objectClass = field.getType();
		final ManyToOne decorators = field.getDeclaredAnnotation(ManyToOne.class);
		if (decorators == null) {
			return;
		}
		if (objectClass == decorators.targetEntity()) {
			if (decorators.fetch() == FetchType.EAGER) {
				final CountInOut countNotNull = new CountInOut(0);
				final Object dataNew = DataAccess.createObjectFromSQLRequest(rs, objectClass, count, countNotNull, options, lazyCall);
				if (dataNew != null && countNotNull.value != 0) {
					field.set(data, dataNew);
				}
			} else {
				// here we have the field, the data and the the remote value ==> can create callback that generate the update of the value ...
				final Long foreignKey = rs.getLong(count.value);
				count.inc();
				if (!rs.wasNull()) {
					// In the lazy mode, the request is done in asynchronous mode, they will be done after...
					final LazyGetter lambda = () -> {
						// TODO: update to have get with abstract types ....
						final Object foreignData = DataAccess.get(decorators.targetEntity(), foreignKey);
						if (foreignData == null) {
							return;
						}
						field.set(data, foreignData);
					};
					lazyCall.add(lambda);
				}
			}
		}
	}

	// TODO : refacto this table to manage a generic table with dynamic name to be serialisable with the default system
	@Override
	public void createTables(final String tableName, final Field field, final StringBuilder mainTableBuilder, final List<String> preActionList, final List<String> postActionList,
			final boolean createIfNotExist, final boolean createDrop, final int fieldId) throws Exception {
		DataFactory.createTablesSpecificType(tableName, field, mainTableBuilder, preActionList, postActionList, createIfNotExist, createDrop, fieldId, Long.class);
	}
}
