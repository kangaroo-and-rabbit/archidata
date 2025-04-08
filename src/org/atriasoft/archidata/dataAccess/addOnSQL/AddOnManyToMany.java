package org.atriasoft.archidata.dataAccess.addOnSQL;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.atriasoft.archidata.annotation.AnnotationTools;
import org.atriasoft.archidata.annotation.AnnotationTools.FieldName;
import org.atriasoft.archidata.dataAccess.CountInOut;
import org.atriasoft.archidata.dataAccess.DBAccess;
import org.atriasoft.archidata.dataAccess.DBAccessMorphia;
import org.atriasoft.archidata.dataAccess.DBAccessSQL;
import org.atriasoft.archidata.dataAccess.DataFactory;
import org.atriasoft.archidata.dataAccess.LazyGetter;
import org.atriasoft.archidata.dataAccess.QueryAnd;
import org.atriasoft.archidata.dataAccess.QueryCondition;
import org.atriasoft.archidata.dataAccess.QueryInList;
import org.atriasoft.archidata.dataAccess.QueryOptions;
import org.atriasoft.archidata.dataAccess.addOnSQL.model.LinkTableGeneric;
import org.atriasoft.archidata.dataAccess.options.Condition;
import org.atriasoft.archidata.dataAccess.options.OptionSpecifyType;
import org.atriasoft.archidata.dataAccess.options.OverrideTableName;
import org.atriasoft.archidata.exception.DataAccessException;
import org.atriasoft.archidata.tools.ConfigBaseVariable;
import org.bson.types.ObjectId;
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
		if (objectClass == Long.class || objectClass == UUID.class || objectClass == ObjectId.class) {
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

	public static String generateLinkTableNameField(
			final String tableName,
			final Field field,
			final QueryOptions options) throws Exception {
		final FieldName name = AnnotationTools.getFieldName(field, options);
		return generateLinkTableName(tableName, name.inTable());
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
		} else if (objectClass == UUID.class) {
			// ???
		} else if (objectClass == ObjectId.class) {
			// ???
		} else {
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
		if (objectClass == Long.class || objectClass == UUID.class || objectClass == ObjectId.class) {
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
		} else if (objectClass == ObjectId.class) {
			final List<ObjectId> idList = ioDb.getListOfRawOIDs(rs, count.value);
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
					final FieldName idField = AnnotationTools.getFieldName(AnnotationTools.getIdField(objectClass),
							options);
					// In the lazy mode, the request is done in asynchronous mode, they will be done after...
					final LazyGetter lambda = () -> {
						final List<Long> childs = new ArrayList<>(idList);
						// TODO: update to have get with abstract types ....
						@SuppressWarnings("unchecked")
						final Object foreignData = ioDb.getsWhere(decorators.targetEntity(),
								new Condition(new QueryInList<>(idField.inTable(), childs)));
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
					final FieldName idField = AnnotationTools.getFieldName(AnnotationTools.getIdField(objectClass),
							options);
					// In the lazy mode, the request is done in asynchronous mode, they will be done after...
					final LazyGetter lambda = () -> {
						final List<UUID> childs = new ArrayList<>(idList);
						// TODO: update to have get with abstract types ....
						@SuppressWarnings("unchecked")
						final Object foreignData = ioDb.getsWhere(decorators.targetEntity(),
								new Condition(new QueryInList<>(idField.inTable(), childs)));
						if (foreignData == null) {
							return;
						}
						field.set(data, foreignData);
					};
					lazyCall.add(lambda);
				}
			} else if (foreignKeyType == ObjectId.class) {
				final List<ObjectId> idList = ioDb.getListOfRawOIDs(rs, count.value);
				// field.set(data, idList);
				count.inc();
				if (idList != null && idList.size() > 0) {
					final FieldName idField = AnnotationTools.getFieldName(AnnotationTools.getIdField(objectClass),
							options);
					// In the lazy mode, the request is done in asynchronous mode, they will be done after...
					final LazyGetter lambda = () -> {
						final List<ObjectId> childs = new ArrayList<>(idList);
						// TODO: update to have get with abstract types ....
						@SuppressWarnings("unchecked")
						final Object foreignData = ioDb.getsWhere(decorators.targetEntity(),
								new Condition(new QueryInList<>(idField.inTable(), childs)));
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
			final List<LazyGetter> actions,
			final QueryOptions options) throws Exception {
		if (field.getType() != List.class) {
			LOGGER.error("Can not ManyToMany with other than List Model: {}", field.getType().getCanonicalName());
			return;
		}
		final Class<?> objectClass = (Class<?>) ((ParameterizedType) field.getGenericType())
				.getActualTypeArguments()[0];
		if (objectClass != Long.class && objectClass != UUID.class && objectClass != ObjectId.class) {
			throw new DataAccessException(
					"Can not ManyToMany with other than List<Long> or List<UUID> or List<ObjectId> Model: List<"
							+ objectClass.getCanonicalName() + ">");
		}
		final FieldName columnName = AnnotationTools.getFieldName(field, options);
		final String linkTableName = generateLinkTableName(tableName, columnName.inTable());

		actions.add(() -> {
			ioDb.deleteWhere(LinkTableGeneric.class, new OverrideTableName(linkTableName),
					new Condition(new QueryCondition("object1Id", "=", localKey)),
					new OptionSpecifyType("object1Id", localKey.getClass()),
					new OptionSpecifyType("object2Id", objectClass));
		});
		asyncInsert(ioDb, tableName, localKey, field, data, actions, options);
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
			final List<LazyGetter> actions,
			final QueryOptions options) throws Exception {
		if (data == null) {
			return;
		}
		if (field.getType() != List.class) {
			LOGGER.error("Can not ManyToMany with other than List Model: {}", field.getType().getCanonicalName());
			return;
		}
		final Class<?> objectClass = (Class<?>) ((ParameterizedType) field.getGenericType())
				.getActualTypeArguments()[0];
		if (objectClass != Long.class && objectClass != UUID.class && objectClass != ObjectId.class) {
			throw new DataAccessException(
					"Can not ManyToMany with other than List<Long> or List<UUID> or List<ObjectId> Model: List<"
							+ objectClass.getCanonicalName() + ">");
		}
		final FieldName columnName = AnnotationTools.getFieldName(field, options);
		final String linkTableName = generateLinkTableName(tableName, columnName.inTable());
		@SuppressWarnings("unchecked")
		final List<Object> dataCasted = (List<Object>) data;
		if (dataCasted.size() == 0) {
			return;
		}
		final List<LinkTableGeneric> insertElements = new ArrayList<>();
		for (final Object remoteKey : dataCasted) {
			if (remoteKey == null) {
				throw new DataAccessException("Try to insert remote key with null value");
			}
			insertElements.add(new LinkTableGeneric(localKey, remoteKey));
		}
		if (insertElements.size() == 0) {
			LOGGER.warn("Insert multiple link without any value (may have null in the list): {}", dataCasted);
			return;
		}
		actions.add(() -> {
			ioDb.insertMultiple(insertElements, new OverrideTableName(linkTableName),
					new OptionSpecifyType("object1Id", localKey.getClass()),
					new OptionSpecifyType("object2Id", objectClass));
		});
	}

	@Override
	public void drop(final DBAccessSQL ioDb, final String tableName, final Field field, final QueryOptions options)
			throws Exception {
		final FieldName columnName = AnnotationTools.getFieldName(field, options);
		final String linkTableName = generateLinkTableName(tableName, columnName.inTable());
		ioDb.drop(LinkTableGeneric.class, new OverrideTableName(linkTableName));
	}

	@Override
	public void cleanAll(final DBAccessSQL ioDb, final String tableName, final Field field, final QueryOptions options)
			throws Exception {
		final FieldName columnName = AnnotationTools.getFieldName(field, options);
		final String linkTableName = generateLinkTableName(tableName, columnName.inTable());
		ioDb.cleanAll(LinkTableGeneric.class, new OverrideTableName(linkTableName));
	}

	public static void addLink(
			final DBAccess ioDb,
			final Class<?> clazz,
			final Object localKey,
			final String column,
			final Object remoteKey) throws Exception {
		if (ioDb instanceof final DBAccessSQL daSQL) {
			final String tableName = AnnotationTools.getTableName(clazz);
			final String linkTableName = generateLinkTableName(tableName, column);
			final LinkTableGeneric insertElement = new LinkTableGeneric(localKey, remoteKey);
			daSQL.insert(insertElement, new OverrideTableName(linkTableName),
					new OptionSpecifyType("object1Id", localKey.getClass()),
					new OptionSpecifyType("object2Id", remoteKey.getClass()));
		} else if (ioDb instanceof final DBAccessMorphia dam) {

		} else {
			throw new DataAccessException("DataAccess Not managed");
		}

	}

	public static long removeLink(
			final DBAccess ioDb,
			final Class<?> clazz,
			final Object localKey,
			final String column,
			final Object remoteKey) throws Exception {
		if (ioDb instanceof final DBAccessSQL daSQL) {
			final String tableName = AnnotationTools.getTableName(clazz);
			final String linkTableName = generateLinkTableName(tableName, column);
			return daSQL.deleteWhere(LinkTableGeneric.class, new OverrideTableName(linkTableName),
					new Condition(new QueryAnd(new QueryCondition("object1Id", "=", localKey),
							new QueryCondition("object2Id", "=", remoteKey))),
					new OptionSpecifyType("object1Id", localKey.getClass()),
					new OptionSpecifyType("object2Id", remoteKey.getClass()));
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
			final int fieldId,
			final QueryOptions options) throws Exception {
		final ManyToMany manyToMany = AnnotationTools.getManyToMany(field);
		if (manyToMany.mappedBy() != null && manyToMany.mappedBy().length() != 0) {
			// not the reference model to create base:
			return;
		}
		final String linkTableName = generateLinkTableNameField(tableName, field, options);
		final QueryOptions options2 = new QueryOptions(new OverrideTableName(linkTableName));
		final Class<?> objectClass = (Class<?>) ((ParameterizedType) field.getGenericType())
				.getActualTypeArguments()[0];
		final Class<?> primaryType = primaryField.getType();
		options2.add(new OptionSpecifyType("object1Id", primaryType));
		options2.add(new OptionSpecifyType("object2Id", objectClass));
		final List<String> sqlCommand = DataFactory.createTable(LinkTableGeneric.class, options2);
		postActionList.addAll(sqlCommand);
	}
}
