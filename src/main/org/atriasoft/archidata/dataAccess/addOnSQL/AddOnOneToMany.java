package org.atriasoft.archidata.dataAccess.addOnSQL;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.atriasoft.archidata.annotation.AnnotationTools;
import org.atriasoft.archidata.annotation.AnnotationTools.FieldName;
import org.atriasoft.archidata.dataAccess.CountInOut;
import org.atriasoft.archidata.dataAccess.DBAccessSQL;
import org.atriasoft.archidata.dataAccess.DataFactory;
import org.atriasoft.archidata.dataAccess.LazyGetter;
import org.atriasoft.archidata.dataAccess.QueryCondition;
import org.atriasoft.archidata.dataAccess.QueryOptions;
import org.atriasoft.archidata.dataAccess.options.Condition;
import org.atriasoft.archidata.exception.DataAccessException;
import org.atriasoft.archidata.tools.ConfigBaseVariable;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotNull;

public class AddOnOneToMany implements DataAccessAddOn {
	static final Logger LOGGER = LoggerFactory.getLogger(AddOnOneToMany.class);
	static final String SEPARATOR_LONG = "-";

	/** Convert the list if external id in a string '-' separated
	 * @param ids List of value (null are removed)
	 * @return '-' string separated */
	protected static String getStringOfIds(final List<Long> ids) {
		final List<Long> tmp = new ArrayList<>(ids);
		return tmp.stream().map(String::valueOf).collect(Collectors.joining("-"));
	}

	/** extract a list of "-" separated element from a SQL input data.
	 * @param rs Result Set of the BDD
	 * @param iii Id in the result set
	 * @return The list of Long value
	 * @throws SQLException if an error is generated in the sql request. */
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
	public String getSQLFieldType(final Field field, final QueryOptions options) throws Exception {
		final FieldName fieldName = AnnotationTools.getFieldName(field, options);
		try {
			return DataFactory.convertTypeInSQL(Long.class, fieldName.inTable());
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
	public void insertData(
			final DBAccessSQL ioDb,
			final PreparedStatement ps,
			final Field field,
			final Object rootObject,
			final CountInOut iii) throws SQLException, IllegalArgumentException, IllegalAccessException {
		throw new IllegalAccessException("Can not generate an inset of @OneToMany");
	}

	@Override
	public boolean canInsert(final Field field) {
		return false;
	}

	@Override
	public boolean isInsertAsync(final Field field) throws Exception {
		// TODO: can be implemented later...
		return false;
	}

	@Override
	public boolean canRetrieve(final Field field) {
		if (field.getType() != List.class) {
			return false;
		}
		final Class<?> objectClass = (Class<?>) ((ParameterizedType) field.getGenericType())
				.getActualTypeArguments()[0];
		if (objectClass == Long.class || objectClass == UUID.class || objectClass == ObjectId.class) {
			return true;
		}
		final OneToMany decorators = field.getDeclaredAnnotation(OneToMany.class);
		if (decorators == null) {
			return false;
		}
		if (decorators.targetEntity() == objectClass) {
			return true;
		}
		return false;
	}

	public void generateConcatQuery(
			@NotNull final String tableName,
			@NotNull final String primaryKey,
			@NotNull final Field field,
			@NotNull final StringBuilder querySelect,
			@NotNull final StringBuilder query,
			@NotNull final String name,
			@NotNull final CountInOut count,
			final QueryOptions options,
			final Class<?> targetEntity,
			final String mappedBy) throws Exception {
		final Class<?> objectClass = (Class<?>) ((ParameterizedType) field.getGenericType())
				.getActualTypeArguments()[0];
		final String remoteTableName = AnnotationTools.getTableName(targetEntity);
		final FieldName remoteTablePrimaryKeyName = AnnotationTools
				.getFieldName(AnnotationTools.getPrimaryKeyField(targetEntity), options);
		final String tmpRemoteVariable = "tmp_" + Integer.toString(count.value);
		final String remoteDeletedFieldName = AnnotationTools.getDeletedFieldName(targetEntity);

		querySelect.append(" (SELECT GROUP_CONCAT(");
		querySelect.append(tmpRemoteVariable);
		querySelect.append(".");
		querySelect.append(remoteTablePrimaryKeyName.inTable());
		querySelect.append(" ");
		if ("sqlite".equals(ConfigBaseVariable.getDBType())) {
			querySelect.append(", ");
		} else {
			querySelect.append("SEPARATOR ");
		}
		querySelect.append("'");
		if (objectClass == Long.class) {
			querySelect.append(SEPARATOR_LONG);
		}
		querySelect.append("') FROM ");
		querySelect.append(remoteTableName);
		querySelect.append(" ");
		querySelect.append(tmpRemoteVariable);
		querySelect.append(" WHERE ");
		if (remoteDeletedFieldName != null) {
			querySelect.append(tmpRemoteVariable);
			querySelect.append(".");
			querySelect.append(remoteDeletedFieldName);
			querySelect.append(" = false");
			querySelect.append(" AND ");
		}
		querySelect.append(tableName);
		querySelect.append(".");
		querySelect.append(primaryKey);
		querySelect.append(" = ");
		querySelect.append(tmpRemoteVariable);
		querySelect.append(".");
		querySelect.append(mappedBy);
		querySelect.append(" ");
		querySelect.append(") AS ");
		querySelect.append(name);
		querySelect.append(" ");
	}

	@Override
	public void generateQuery(
			@NotNull final String tableName,
			@NotNull final String primaryKey,
			@NotNull final Field field,
			@NotNull final StringBuilder querySelect,
			@NotNull final StringBuilder query,
			@NotNull final String name,
			@NotNull final CountInOut count,
			final QueryOptions options) throws Exception {
		if (field.getType() != List.class) {
			return;
		}
		final Class<?> objectClass = (Class<?>) ((ParameterizedType) field.getGenericType())
				.getActualTypeArguments()[0];
		final OneToMany decorators = field.getDeclaredAnnotation(OneToMany.class);
		if (decorators == null) {
			return;
		}
		// TODO: manage better the eager and lazy !!
		if (objectClass == Long.class || objectClass == UUID.class || objectClass == ObjectId.class) {
			generateConcatQuery(tableName, primaryKey, field, querySelect, query, name, count, options,
					decorators.targetEntity(), decorators.mappedBy());
			return;
		}
		if (objectClass == decorators.targetEntity()) {
			if (decorators.fetch() == FetchType.EAGER) {
				throw new DataAccessException("EAGER is not supported for list of element...");
			} else {
				// Force a copy of the primaryKey to permit the async retrieve of the data
				querySelect.append(" ");
				querySelect.append(tableName);
				querySelect.append(".");
				querySelect.append(primaryKey);
				querySelect.append(" AS tmp_");
				querySelect.append(Integer.toString(count.value));
			}
		}
	}

	@Override
	public void fillFromQuery(
			final DBAccessSQL ioDb,
			final ResultSet rs,
			final Field field,
			final Object data,
			final CountInOut count,
			final QueryOptions options,
			final List<LazyGetter> lazyCall) throws Exception {
		try {
			if (field.getType() != List.class) {
				LOGGER.error("Can not OneToMany with other than List Model: {}", field.getType().getCanonicalName());
				return;
			}
			final Class<?> objectClass = (Class<?>) ((ParameterizedType) field.getGenericType())
					.getActualTypeArguments()[0];
			final OneToMany decorators = field.getDeclaredAnnotation(OneToMany.class);
			if (decorators == null) {
				return;
			}
			if (objectClass == Long.class) {
				final List<Long> idList = ioDb.getListOfIds(rs, count.value, SEPARATOR_LONG);
				field.set(data, idList);
				count.inc();
				return;
			} else if (objectClass == UUID.class) {
				final List<UUID> idList = ioDb.getListOfRawUUIDs(rs, count.value);
				field.set(data, idList);
				count.inc();
				return;
			} else if (objectClass == ObjectId.class) {
				final List<ObjectId> idList = ioDb.getListOfRawOIDs(rs, count.value);
				field.set(data, idList);
				count.inc();
				return;
			}
			if (objectClass == decorators.targetEntity()) {
				final String destinationField = decorators.mappedBy();
				final Field typeDestination = AnnotationTools.getFieldNamed(objectClass, destinationField);
				final Class<?> destinationClass = typeDestination.getType();

				Long parentIdTmp = null;
				UUID parendUuidTmp = null;
				ObjectId parendOidTmp = null;
				if (destinationClass == Long.class) {
					final String modelData = rs.getString(count.value);
					parentIdTmp = Long.valueOf(modelData);
					count.inc();
				} else if (destinationClass == UUID.class) {
					final List<UUID> idList = ioDb.getListOfRawUUIDs(rs, count.value);
					parendUuidTmp = idList.get(0);
					count.inc();
				} else if (destinationClass == ObjectId.class) {
					final List<ObjectId> idList = ioDb.getListOfRawOIDs(rs, count.value);
					parendOidTmp = idList.get(0);
					count.inc();
				}
				final Long parentId = parentIdTmp;
				final UUID parendUuid = parendUuidTmp;
				final ObjectId parendOid = parendOidTmp;
				final String mappingKey = decorators.mappedBy();
				// We get the parent ID ... ==> need to request the list of elements
				if (objectClass == Long.class) {
					LOGGER.error("Need to retreive all primary key of all elements.");
					//field.set(data, idList);
					return;
				} else if (objectClass == UUID.class) {
					LOGGER.error("Need to retreive all primary key of all elements");
					//field.set(data, idList);
					return;
				} else if (objectClass == ObjectId.class) {
					LOGGER.error("Need to retreive all primary key of all elements");
					//field.set(data, idList);
					return;
				}
				if (objectClass == decorators.targetEntity()) {
					if (decorators.fetch() == FetchType.EAGER) {
						throw new DataAccessException("EAGER is not supported for list of element...");
					} else if (parentId != null) {
						// In the lazy mode, the request is done in asynchronous mode, they will be done after...
						final LazyGetter lambda = (List<LazyGetter> actions) -> {
							final Object foreignData = ioDb.getsWhere(decorators.targetEntity(),
									new Condition(new QueryCondition(mappingKey, "=", parentId)));
							if (foreignData == null) {
								return;
							}
							field.set(data, foreignData);
						};
						lazyCall.add(lambda);
					} else if (parendUuid != null) {
						final LazyGetter lambda = (List<LazyGetter> actions) -> {
							final Object foreignData = ioDb.getsWhere(decorators.targetEntity(),
									new Condition(new QueryCondition(mappingKey, "=", parendUuid)));
							if (foreignData == null) {
								return;
							}
							field.set(data, foreignData);
						};
						lazyCall.add(lambda);
					} else if (parendOid != null) {
						final LazyGetter lambda = (List<LazyGetter> actions) -> {
							final Object foreignData = ioDb.getsWhere(decorators.targetEntity(),
									new Condition(new QueryCondition(mappingKey, "=", parendOid)));
							if (foreignData == null) {
								return;
							}
							field.set(data, foreignData);
						};
						lazyCall.add(lambda);
					}
				}
			}
		} catch (final Exception ex) {
			ex.printStackTrace();
			LOGGER.error("Fail to parse remote {}", ex.getMessage());
		}
	}

}
