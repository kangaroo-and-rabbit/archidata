package org.kar.archidata.dataAccess.addOnSQL;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.kar.archidata.annotation.AnnotationTools;
import org.kar.archidata.dataAccess.CountInOut;
import org.kar.archidata.dataAccess.DBAccess;
import org.kar.archidata.dataAccess.DBAccessMorphia;
import org.kar.archidata.dataAccess.DBAccessSQL;
import org.kar.archidata.dataAccess.DataFactory;
import org.kar.archidata.dataAccess.LazyGetter;
import org.kar.archidata.dataAccess.QueryAnd;
import org.kar.archidata.dataAccess.QueryCondition;
import org.kar.archidata.dataAccess.QueryInList;
import org.kar.archidata.dataAccess.QueryOptions;
import org.kar.archidata.dataAccess.addOnSQL.model.LinkTableLongLong;
import org.kar.archidata.dataAccess.addOnSQL.model.LinkTableLongUUID;
import org.kar.archidata.dataAccess.addOnSQL.model.LinkTableUUIDLong;
import org.kar.archidata.dataAccess.addOnSQL.model.LinkTableUUIDUUID;
import org.kar.archidata.dataAccess.options.Condition;
import org.kar.archidata.dataAccess.options.OverrideTableName;
import org.kar.archidata.exception.DataAccessException;
import org.kar.archidata.tools.ConfigBaseVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;
import jakarta.validation.constraints.NotNull;

public class AddOnManyToMany implements DataAccessAddOn {
	static final Logger LOGGER = LoggerFactory.getLogger(AddOnManyToMany.class);
	static final String SEPARATOR_LONG = "-";
	static final String SEPARATOR_UUID = "_";

	@Override
	public Class<?> getAnnotationClass() {
		return ManyToMany.class;
	}

	@Override
	public String getSQLFieldType(final Field elem) {
		return null;
	}

	@Override
	public boolean isCompatibleField(final Field elem) {
		final ManyToMany decorators = elem.getDeclaredAnnotation(ManyToMany.class);
		return decorators != null;
	}

	@Override
	public void insertData(
			final DBAccessSQL ioDb,
			final PreparedStatement ps,
			final Field field,
			final Object rootObject,
			final CountInOut iii) throws SQLException, IllegalArgumentException, IllegalAccessException {

	}

	@Override
	public boolean canInsert(final Field field) {
		return false;
	}

	@Override
	public boolean canRetrieve(final Field field) {
		if (field.getType() != List.class) {
			return false;
		}
		final Class<?> objectClass = (Class<?>) ((ParameterizedType) field.getGenericType())
				.getActualTypeArguments()[0];
		if (objectClass == Long.class || objectClass == UUID.class) {
			return true;
		}
		final ManyToMany decorators = field.getDeclaredAnnotation(ManyToMany.class);
		if (decorators == null) {
			return false;
		}
		if (decorators.targetEntity() == objectClass) {
			return true;
		}
		return false;
	}

	public static String generateLinkTableNameField(final String tableName, final Field field) throws Exception {
		final String name = AnnotationTools.getFieldName(field);
		return generateLinkTableName(tableName, name);
	}

	public static String generateLinkTableName(final String tableName, final String name) {
		String localName = name;
		if (name.endsWith("s")) {
			localName = name.substring(0, name.length() - 1);
		}
		return tableName + "_link_" + localName;
	}

	public void generateConcatQuery(
			@NotNull final String tableName,
			@NotNull final String primaryKey,
			@NotNull final Field field,
			@NotNull final StringBuilder querySelect,
			@NotNull final StringBuilder query,
			@NotNull final String name,
			@NotNull final CountInOut count,
			final QueryOptions options) throws Exception {
		final ManyToMany manyToMany = AnnotationTools.getManyToMany(field);
		String linkTableName = generateLinkTableName(tableName, name);
		if (manyToMany.mappedBy() != null && manyToMany.mappedBy().length() != 0) {
			// TODO:  get the remote table name .....
			final String remoteTableName = AnnotationTools.getTableName(manyToMany.targetEntity());
			linkTableName = generateLinkTableName(remoteTableName, manyToMany.mappedBy());
		}
		final Class<?> objectClass = (Class<?>) ((ParameterizedType) field.getGenericType())
				.getActualTypeArguments()[0];
		final String tmpVariable = "tmp_" + Integer.toString(count.value);
		querySelect.append(" (SELECT GROUP_CONCAT(");
		querySelect.append(tmpVariable);
		if (manyToMany.mappedBy() == null || manyToMany.mappedBy().length() == 0) {
			querySelect.append(".object2Id ");
		} else {
			querySelect.append(".object1Id ");
		}
		if ("sqlite".equals(ConfigBaseVariable.getDBType())) {
			querySelect.append(", ");
		} else {
			querySelect.append("SEPARATOR ");
		}
		querySelect.append("'");
		if (objectClass == Long.class) {
			querySelect.append(SEPARATOR_LONG);
		} else if (objectClass == UUID.class) {} else {
			final Class<?> foreignKeyType = AnnotationTools.getPrimaryKeyField(objectClass).getType();
			if (foreignKeyType == Long.class) {
				querySelect.append(SEPARATOR_LONG);
			}
		}
		querySelect.append("') FROM ");
		querySelect.append(linkTableName);
		querySelect.append(" ");
		querySelect.append(tmpVariable);
		querySelect.append(" WHERE ");
		querySelect.append(tmpVariable);
		querySelect.append(".deleted = false");
		querySelect.append(" AND ");
		querySelect.append(tableName);
		querySelect.append(".");
		querySelect.append(primaryKey);
		querySelect.append(" = ");
		querySelect.append(tmpVariable);
		querySelect.append(".");
		if (manyToMany.mappedBy() == null || manyToMany.mappedBy().length() == 0) {
			querySelect.append("object1Id ");
		} else {
			querySelect.append("object2Id ");
		}
		if (!"sqlite".equals(ConfigBaseVariable.getDBType())) {
			querySelect.append(" GROUP BY ");
			querySelect.append(tmpVariable);
			if (manyToMany.mappedBy() == null || manyToMany.mappedBy().length() == 0) {
				querySelect.append(".object1Id");
			} else {
				querySelect.append(".object2Id");
			}
		}
		querySelect.append(") AS ");
		querySelect.append(name);
		querySelect.append(" ");
		/* "              (SELECT GROUP_CONCAT(tmp.data_id SEPARATOR '-')" + "                      FROM cover_link_node tmp" + "                      WHERE tmp.deleted = false" +
		 * "                            AND node.id = tmp.node_id" + "                      GROUP BY tmp.node_id) AS covers" + */
		count.inc();
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
		// TODO: manage better the eager and lazy !!
		if (objectClass == Long.class || objectClass == UUID.class) {
			generateConcatQuery(tableName, primaryKey, field, querySelect, query, name, count, options);
		}
		final ManyToMany decorators = field.getDeclaredAnnotation(ManyToMany.class);
		if (decorators == null) {
			return;
		}
		if (objectClass == decorators.targetEntity()) {
			if (decorators.fetch() == FetchType.EAGER) {
				throw new DataAccessException("EAGER is not supported for list of element...");
			} else {
				generateConcatQuery(tableName, primaryKey, field, querySelect, query, name, count, options);
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
		if (field.getType() != List.class) {
			LOGGER.error("Can not ManyToMany with other than List Model: {}", field.getType().getCanonicalName());
			return;
		}
		final Class<?> objectClass = (Class<?>) ((ParameterizedType) field.getGenericType())
				.getActualTypeArguments()[0];
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
		}
		final ManyToMany decorators = field.getDeclaredAnnotation(ManyToMany.class);
		if (decorators == null) {
			return;
		}
		if (objectClass == decorators.targetEntity()) {
			final Class<?> foreignKeyType = AnnotationTools.getPrimaryKeyField(objectClass).getType();
			if (decorators.fetch() == FetchType.EAGER) {
				throw new DataAccessException("EAGER is not supported for list of element...");
			} else if (foreignKeyType == Long.class) {
				final List<Long> idList = ioDb.getListOfIds(rs, count.value, SEPARATOR_LONG);
				// field.set(data, idList);
				count.inc();
				if (idList != null && idList.size() > 0) {
					final String idField = AnnotationTools.getFieldName(AnnotationTools.getIdField(objectClass));
					// In the lazy mode, the request is done in asynchronous mode, they will be done after...
					final LazyGetter lambda = () -> {
						final List<Long> childs = new ArrayList<>(idList);
						// TODO: update to have get with abstract types ....
						@SuppressWarnings("unchecked")
						final Object foreignData = ioDb.getsWhere(decorators.targetEntity(),
								new Condition(new QueryInList<>(idField, childs)));
						if (foreignData == null) {
							return;
						}
						field.set(data, foreignData);
					};
					lazyCall.add(lambda);
				}
			} else if (foreignKeyType == UUID.class) {
				final List<UUID> idList = ioDb.getListOfRawUUIDs(rs, count.value);
				// field.set(data, idList);
				count.inc();
				if (idList != null && idList.size() > 0) {
					final String idField = AnnotationTools.getFieldName(AnnotationTools.getIdField(objectClass));
					// In the lazy mode, the request is done in asynchronous mode, they will be done after...
					final LazyGetter lambda = () -> {
						final List<UUID> childs = new ArrayList<>(idList);
						// TODO: update to have get with abstract types ....
						@SuppressWarnings("unchecked")
						final Object foreignData = ioDb.getsWhere(decorators.targetEntity(),
								new Condition(new QueryInList<>(idField, childs)));
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

	@Override
	public boolean isUpdateAsync(final Field field) {
		return true;
	}

	@Override
	public void asyncUpdate(
			final DBAccessSQL ioDb,
			final String tableName,
			final Object localKey,
			final Field field,
			final Object data,
			final List<LazyGetter> actions) throws Exception {
		if (field.getType() != List.class) {
			LOGGER.error("Can not ManyToMany with other than List Model: {}", field.getType().getCanonicalName());
			return;
		}
		final Class<?> objectClass = (Class<?>) ((ParameterizedType) field.getGenericType())
				.getActualTypeArguments()[0];
		if (objectClass != Long.class && objectClass != UUID.class) {
			throw new DataAccessException("Can not ManyToMany with other than List<Long> or List<UUID> Model: List<"
					+ objectClass.getCanonicalName() + ">");
		}
		final String columnName = AnnotationTools.getFieldName(field);
		final String linkTableName = generateLinkTableName(tableName, columnName);

		if (localKey instanceof final Long localKeyLong) {
			if (objectClass == Long.class) {
				actions.add(() -> {
					ioDb.deleteWhere(LinkTableLongLong.class, new OverrideTableName(linkTableName),
							new Condition(new QueryCondition("object1Id", "=", localKeyLong)));
				});
				asyncInsert(ioDb, tableName, localKey, field, data, actions);
			} else {
				actions.add(() -> {
					ioDb.deleteWhere(LinkTableLongUUID.class, new OverrideTableName(linkTableName),
							new Condition(new QueryCondition("object1Id", "=", localKeyLong)));
				});
				asyncInsert(ioDb, tableName, localKey, field, data, actions);
			}
		} else if (localKey instanceof final UUID localKeyUUID) {
			if (objectClass == Long.class) {
				actions.add(() -> {
					ioDb.deleteWhere(LinkTableUUIDLong.class, new OverrideTableName(linkTableName),
							new Condition(new QueryCondition("object1Id", "=", localKeyUUID)));
				});
				asyncInsert(ioDb, tableName, localKey, field, data, actions);
			} else {
				actions.add(() -> {
					ioDb.deleteWhere(LinkTableUUIDUUID.class, new OverrideTableName(linkTableName),
							new Condition(new QueryCondition("object1Id", "=", localKeyUUID)));
				});
				asyncInsert(ioDb, tableName, localKey, field, data, actions);
			}
		}
	}

	@Override
	public boolean isInsertAsync(final Field field) {
		return true;
	}

	@Override
	public void asyncInsert(
			final DBAccessSQL ioDb,
			final String tableName,
			final Object localKey,
			final Field field,
			final Object data,
			final List<LazyGetter> actions) throws Exception {
		if (data == null) {
			return;
		}
		if (field.getType() != List.class) {
			LOGGER.error("Can not ManyToMany with other than List Model: {}", field.getType().getCanonicalName());
			return;
		}
		final Class<?> objectClass = (Class<?>) ((ParameterizedType) field.getGenericType())
				.getActualTypeArguments()[0];
		if (objectClass != Long.class && objectClass != UUID.class) {
			throw new DataAccessException("Can not ManyToMany with other than List<Long> or List<UUID> Model: List<"
					+ objectClass.getCanonicalName() + ">");
		}
		final String columnName = AnnotationTools.getFieldName(field);
		final String linkTableName = generateLinkTableName(tableName, columnName);
		if (localKey instanceof final Long localKeyLong) {
			if (objectClass == Long.class) {
				// ========================================================
				// == Link a "Long" primary Key with List<Long>
				// ========================================================
				@SuppressWarnings("unchecked")
				final List<Long> dataCasted = (List<Long>) data;
				if (dataCasted.size() == 0) {
					return;
				}
				final List<LinkTableLongLong> insertElements = new ArrayList<>();
				for (final Long remoteKey : dataCasted) {
					if (remoteKey == null) {
						throw new DataAccessException("Try to insert remote key with null value");
					}
					insertElements.add(new LinkTableLongLong(localKeyLong, remoteKey));
				}
				if (insertElements.size() == 0) {
					LOGGER.warn("Insert multiple link without any value (may have null in the list): {}", dataCasted);
					return;
				}
				actions.add(() -> {
					ioDb.insertMultiple(insertElements, new OverrideTableName(linkTableName));
				});
			} else {
				// ========================================================
				// == Link a "Long" primary Key with List<UUID>
				// ========================================================
				@SuppressWarnings("unchecked")
				final List<UUID> dataCasted = (List<UUID>) data;
				if (dataCasted.size() == 0) {
					return;
				}
				final List<LinkTableLongUUID> insertElements = new ArrayList<>();
				for (final UUID remoteKey : dataCasted) {
					if (remoteKey == null) {
						throw new DataAccessException("Try to insert remote key with null value");
					}
					insertElements.add(new LinkTableLongUUID(localKeyLong, remoteKey));
				}
				if (insertElements.size() == 0) {
					LOGGER.warn("Insert multiple link without any value (may have null in the list): {}", dataCasted);
					return;
				}
				actions.add(() -> {
					ioDb.insertMultiple(insertElements, new OverrideTableName(linkTableName));
				});
			}
		} else if (localKey instanceof final UUID localKeyUUID) {
			if (objectClass == Long.class) {
				// ========================================================
				// == Link a "UUID" primary Key with List<Long>
				// ========================================================
				@SuppressWarnings("unchecked")
				final List<Long> dataCasted = (List<Long>) data;
				if (dataCasted.size() == 0) {
					return;
				}
				final List<LinkTableUUIDLong> insertElements = new ArrayList<>();
				for (final Long remoteKey : dataCasted) {
					if (remoteKey == null) {
						throw new DataAccessException("Try to insert remote key with null value");
					}
					insertElements.add(new LinkTableUUIDLong(localKeyUUID, remoteKey));
				}
				if (insertElements.size() == 0) {
					LOGGER.warn("Insert multiple link without any value (may have null in the list): {}", dataCasted);
					return;
				}
				actions.add(() -> {
					ioDb.insertMultiple(insertElements, new OverrideTableName(linkTableName));
				});
			} else {
				// ========================================================
				// == Link a "UUID" primary Key with List<UUID>
				// ========================================================
				@SuppressWarnings("unchecked")
				final List<UUID> dataCasted = (List<UUID>) data;
				if (dataCasted.size() == 0) {
					return;
				}
				final List<LinkTableUUIDUUID> insertElements = new ArrayList<>();
				for (final UUID remoteKey : dataCasted) {
					if (remoteKey == null) {
						throw new DataAccessException("Try to insert remote key with null value");
					}
					insertElements.add(new LinkTableUUIDUUID(localKeyUUID, remoteKey));
				}
				if (insertElements.size() == 0) {
					LOGGER.warn("Insert multiple link without any value (may have null in the list): {}", dataCasted);
					return;
				}
				actions.add(() -> {
					ioDb.insertMultiple(insertElements, new OverrideTableName(linkTableName));
				});
			}
		} else {
			throw new DataAccessException("Not manage access of remte key like ManyToMany other than Long or UUID: "
					+ localKey.getClass().getCanonicalName());
		}

	}

	@Override
	public void drop(final DBAccessSQL ioDb, final String tableName, final Field field) throws Exception {
		final String columnName = AnnotationTools.getFieldName(field);
		final String linkTableName = generateLinkTableName(tableName, columnName);
		final Class<?> objectClass = (Class<?>) ((ParameterizedType) field.getGenericType())
				.getActualTypeArguments()[0];
		if (objectClass != Long.class && objectClass != UUID.class) {
			throw new DataAccessException("Can not ManyToMany with other than List<Long> or List<UUID> Model: List<"
					+ objectClass.getCanonicalName() + ">");
		}
		ioDb.drop(LinkTableLongLong.class, new OverrideTableName(linkTableName));
	}

	@Override
	public void cleanAll(final DBAccessSQL ioDb, final String tableName, final Field field) throws Exception {
		final String columnName = AnnotationTools.getFieldName(field);
		final String linkTableName = generateLinkTableName(tableName, columnName);
		final Class<?> objectClass = (Class<?>) ((ParameterizedType) field.getGenericType())
				.getActualTypeArguments()[0];
		if (objectClass != Long.class && objectClass != UUID.class) {
			throw new DataAccessException("Can not ManyToMany with other than List<Long> or List<UUID> Model: List<"
					+ objectClass.getCanonicalName() + ">");
		}
		ioDb.cleanAll(LinkTableLongLong.class, new OverrideTableName(linkTableName));
	}

	public static void addLink(
			final DBAccess ioDb,
			final Class<?> clazz,
			final long localKey,
			final String column,
			final long remoteKey) throws Exception {
		if (ioDb instanceof final DBAccessSQL daSQL) {
			final String tableName = AnnotationTools.getTableName(clazz);
			final String linkTableName = generateLinkTableName(tableName, column);
			/* final Class<?> objectClass = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0]; if (objectClass != Long.class && objectClass != UUID.class) { throw new
			 * DataAccessException("Can not ManyToMany with other than List<Long> or List<UUID> Model: List<" + objectClass.getCanonicalName() + ">"); } */
			final LinkTableLongLong insertElement = new LinkTableLongLong(localKey, remoteKey);
			daSQL.insert(insertElement, new OverrideTableName(linkTableName));
		} else if (ioDb instanceof final DBAccessMorphia dam) {

		} else {
			throw new DataAccessException("DataAccess Not managed");
		}

	}

	public static long removeLink(
			final DBAccess ioDb,
			final Class<?> clazz,
			final long localKey,
			final String column,
			final long remoteKey) throws Exception {
		if (ioDb instanceof final DBAccessSQL daSQL) {
			final String tableName = AnnotationTools.getTableName(clazz);
			final String linkTableName = generateLinkTableName(tableName, column);
			return daSQL.deleteWhere(LinkTableLongLong.class, new OverrideTableName(linkTableName),
					new Condition(new QueryAnd(new QueryCondition("object1Id", "=", localKey),
							new QueryCondition("object2Id", "=", remoteKey))));
		} else if (ioDb instanceof final DBAccessMorphia dam) {
			return 0L;
		} else {
			throw new DataAccessException("DataAccess Not managed");
		}
	}

	@Override
	public void createTables(
			final String tableName,
			final Field primaryField,
			final Field field,
			final StringBuilder mainTableBuilder,
			final List<String> preActionList,
			final List<String> postActionList,
			final boolean createIfNotExist,
			final boolean createDrop,
			final int fieldId) throws Exception {

		final ManyToMany manyToMany = AnnotationTools.getManyToMany(field);
		if (manyToMany.mappedBy() != null && manyToMany.mappedBy().length() != 0) {
			// not the reference model to create base:
			return;
		}
		final String linkTableName = generateLinkTableNameField(tableName, field);
		final QueryOptions options = new QueryOptions(new OverrideTableName(linkTableName));
		final Class<?> objectClass = (Class<?>) ((ParameterizedType) field.getGenericType())
				.getActualTypeArguments()[0];
		if (objectClass != Long.class && objectClass != UUID.class) {
			throw new DataAccessException("Can not ManyToMany with other than List<Long> or List<UUID> Model: List<"
					+ objectClass.getCanonicalName() + ">");
		}
		final Class<?> primaryType = primaryField.getType();
		if (primaryType == Long.class) {
			if (objectClass == Long.class) {
				final List<String> sqlCommand = DataFactory.createTable(LinkTableLongLong.class, options);
				postActionList.addAll(sqlCommand);
			} else {
				final List<String> sqlCommand = DataFactory.createTable(LinkTableLongUUID.class, options);
				postActionList.addAll(sqlCommand);
			}
		} else if (primaryType == UUID.class) {
			if (objectClass == Long.class) {
				final List<String> sqlCommand = DataFactory.createTable(LinkTableUUIDLong.class, options);
				postActionList.addAll(sqlCommand);
			} else {
				final List<String> sqlCommand = DataFactory.createTable(LinkTableUUIDUUID.class, options);
				postActionList.addAll(sqlCommand);
			}
		} else {
			throw new DataAccessException("Can not ManyToMany with other than primary key type Long or UUID Model: "
					+ primaryType.getCanonicalName());
		}
	}
}
