package org.atriasoft.archidata.dataAccess.addOnSQL;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
import org.atriasoft.archidata.exception.SystemException;
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

	public static String hashTo64Chars(final String input) {
		try {
			final MessageDigest digest = MessageDigest.getInstance("SHA-256");
			final byte[] hash = digest.digest(input.getBytes());
			final StringBuilder hexString = new StringBuilder();
			for (final byte b : hash) {
				final String hex = Integer.toHexString(0xff & b);
				if (hex.length() == 1) {
					hexString.append('0');
				}
				hexString.append(hex);
			}
			return hexString.toString().substring(0, 64);
		} catch (final NoSuchAlgorithmException e) {
			throw new RuntimeException("Erreur lors du hachage de la chaîne", e);
		}
	}

	public static String hashIfNeeded(final String input) {
		if (input.length() > 64) {
			// Keep only the 50 first chars
			final String truncated = input.substring(0, Math.min(input.length(), 50));
			final String fullHash = hashTo64Chars(input);
			final String hashPart = fullHash.substring(0, 14);
			return truncated + hashPart;
		}
		return input;
	}

	public record LinkTableWithMode(
			String tableName,
			boolean first) {}

	public static LinkTableWithMode generateLinkTableNameField(
			final String tableName,
			final Field field,
			final QueryOptions options) throws Exception {
		return generateLinkTableName(tableName, field);
	}

	public static LinkTableWithMode generateLinkTableName(
			final String tableAName,
			final String tableAFieldName,
			final String tableBName,
			final String tableBFieldName) {
		if (tableAName.compareTo(tableBName) < 0) {
			return new LinkTableWithMode(
					hashIfNeeded(tableAName + "_" + tableAFieldName + "_link_" + tableBName + "_" + tableBFieldName),
					true);
		}
		return new LinkTableWithMode(
				hashIfNeeded(tableBName + "_" + tableBFieldName + "_link_" + tableAName + "_" + tableAFieldName),
				false);
	}

	public static LinkTableWithMode generateLinkTableName(
			final String tableAName,
			final String tableAFieldName,
			final ManyToMany manyToMany) throws SystemException {
		if (manyToMany == null) {
			throw new SystemException("@ManyMany is a null pointer " + tableAName);
		}
		if (manyToMany.targetEntity() == null) {
			throw new SystemException("@ManyMany target entity is a null pointer: " + tableAName);
		}
		if (manyToMany.mappedBy() == null || manyToMany.mappedBy().isEmpty()) {
			throw new SystemException("@ManyMany mapped by is not defined: " + tableAName);
		}
		final String tableNameRemote = AnnotationTools.getTableName(manyToMany.targetEntity());
		return generateLinkTableName(tableAName, tableAFieldName, tableNameRemote, manyToMany.mappedBy());
	}

	public static LinkTableWithMode generateLinkTableName(final String tableAName, final Field field)
			throws SystemException {
		if (field == null) {
			// TODO: throw !!!!
		}
		final FieldName columnName = AnnotationTools.getFieldName(field, null);
		final ManyToMany manyToMany = AnnotationTools.get(field, ManyToMany.class);
		return generateLinkTableName(tableAName, columnName.inTable(), manyToMany);
	}

	public static LinkTableWithMode generateLinkTableName(final Class<?> clazz, final String fieldName)
			throws SystemException {
		if (clazz == null) {
			throw new SystemException("@ManyMany class reference is a null pointer ");
		}
		if (fieldName == null || fieldName.isEmpty() || fieldName.isBlank()) {
			throw new SystemException("@ManyMany field of class reference is not defined");
		}
		final String tableName = AnnotationTools.getTableName(clazz);
		final Field requestedField = AnnotationTools.getFieldNamed(clazz, fieldName);
		return generateLinkTableName(tableName, requestedField);
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
		final LinkTableWithMode linkTable = generateLinkTableName(tableName, name, manyToMany);
		final Class<?> objectClass = (Class<?>) ((ParameterizedType) field.getGenericType())
				.getActualTypeArguments()[0];
		final String tmpVariable = "tmp_" + Integer.toString(count.value);
		querySelect.append(" (SELECT GROUP_CONCAT(");
		querySelect.append(tmpVariable);
		if (linkTable.first()) {
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
		querySelect.append(linkTable.tableName());
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
		if (linkTable.first()) {
			querySelect.append("object1Id ");
		} else {
			querySelect.append("object2Id ");
		}
		if (!"sqlite".equals(ConfigBaseVariable.getDBType())) {
			querySelect.append(" GROUP BY ");
			querySelect.append(tmpVariable);
			if (linkTable.first()) {
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
		final LinkTableWithMode linkTable = generateLinkTableName(tableName, field);
		final String obj1 = linkTable.first ? "object1Id" : "object2Id";
		final String obj2 = linkTable.first ? "object2Id" : "object1Id";

		actions.add(() -> {
			ioDb.deleteWhere(LinkTableGeneric.class, new OverrideTableName(linkTable.tableName()),
					new Condition(new QueryCondition(obj1, "=", localKey)),
					new OptionSpecifyType(obj1, localKey.getClass()), new OptionSpecifyType(obj2, objectClass));
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
		final LinkTableWithMode linkTable = generateLinkTableName(tableName, field);
		@SuppressWarnings("unchecked")
		final List<Object> dataCasted = (List<Object>) data;
		if (dataCasted.size() == 0) {
			return;
		}
		final String obj1 = linkTable.first ? "object1Id" : "object2Id";
		final String obj2 = linkTable.first ? "object2Id" : "object1Id";
		final List<LinkTableGeneric> insertElements = new ArrayList<>();
		for (final Object remoteKey : dataCasted) {
			if (remoteKey == null) {
				throw new DataAccessException("Try to insert remote key with null value");
			}
			if (linkTable.first) {
				insertElements.add(new LinkTableGeneric(localKey, remoteKey));
			} else {
				insertElements.add(new LinkTableGeneric(remoteKey, localKey));
			}
		}
		if (insertElements.size() == 0) {
			LOGGER.warn("Insert multiple link without any value (may have null in the list): {}", dataCasted);
			return;
		}
		actions.add(() -> {
			ioDb.insertMultiple(insertElements, new OverrideTableName(linkTable.tableName()),
					new OptionSpecifyType(obj1, localKey.getClass()), new OptionSpecifyType(obj2, objectClass));
		});
	}

	@Override
	public void drop(final DBAccessSQL ioDb, final String tableName, final Field field, final QueryOptions options)
			throws Exception {
		final LinkTableWithMode linkTable = generateLinkTableName(tableName, field);
		ioDb.drop(LinkTableGeneric.class, new OverrideTableName(linkTable.tableName()));
	}

	@Override
	public void cleanAll(final DBAccessSQL ioDb, final String tableName, final Field field, final QueryOptions options)
			throws Exception {
		final LinkTableWithMode linkTable = generateLinkTableName(tableName, field);
		ioDb.cleanAll(LinkTableGeneric.class, new OverrideTableName(linkTable.tableName()));
	}

	public static void addLink(
			final DBAccess ioDb,
			final Class<?> clazz,
			final Object localKey,
			final String column,
			final Object remoteKey) throws Exception {
		if (ioDb instanceof final DBAccessSQL daSQL) {
			final LinkTableWithMode linkTable = generateLinkTableName(clazz, column);
			final LinkTableGeneric insertElement = linkTable.first ? new LinkTableGeneric(localKey, remoteKey)
					: new LinkTableGeneric(remoteKey, localKey);
			final String obj1 = linkTable.first ? "object1Id" : "object2Id";
			final String obj2 = linkTable.first ? "object2Id" : "object1Id";
			daSQL.insert(insertElement, new OverrideTableName(linkTable.tableName()),
					new OptionSpecifyType(obj1, localKey.getClass()),
					new OptionSpecifyType(obj2, remoteKey.getClass()));
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
			final LinkTableWithMode linkTable = generateLinkTableName(clazz, column);
			final String obj1 = linkTable.first ? "object1Id" : "object2Id";
			final String obj2 = linkTable.first ? "object2Id" : "object1Id";
			return daSQL.deleteWhere(LinkTableGeneric.class, new OverrideTableName(linkTable.tableName()),
					new Condition(new QueryAnd(new QueryCondition(obj1, "=", localKey),
							new QueryCondition(obj2, "=", remoteKey))),
					new OptionSpecifyType(obj1, localKey.getClass()),
					new OptionSpecifyType(obj2, remoteKey.getClass()));
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
		if (manyToMany.mappedBy() == null || manyToMany.mappedBy().length() == 0) {
			throw new SystemException("MappedBy must be set in ManyMany: " + tableName + " " + field.getName());
		}
		final LinkTableWithMode linkTable = generateLinkTableNameField(tableName, field, options);
		if (linkTable.first()) {
			final QueryOptions options2 = new QueryOptions(new OverrideTableName(linkTable.tableName()));
			final Class<?> objectClass = (Class<?>) ((ParameterizedType) field.getGenericType())
					.getActualTypeArguments()[0];
			final Class<?> primaryType = primaryField.getType();
			final String obj1 = linkTable.first ? "object1Id" : "object2Id";
			final String obj2 = linkTable.first ? "object2Id" : "object1Id";
			options2.add(new OptionSpecifyType(obj1, primaryType));
			options2.add(new OptionSpecifyType(obj2, objectClass));
			final List<String> sqlCommand = DataFactory.createTable(LinkTableGeneric.class, options2);
			postActionList.addAll(sqlCommand);
		}
	}
}
