package org.atriasoft.archidata.dataAccess.addOnMongo;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.List;
import java.util.UUID;

import org.atriasoft.archidata.annotation.AnnotationTools;
import org.atriasoft.archidata.annotation.AnnotationTools.FieldName;
import org.atriasoft.archidata.dataAccess.CountInOut;
import org.atriasoft.archidata.dataAccess.DBAccess;
import org.atriasoft.archidata.dataAccess.DBAccessMongo;
import org.atriasoft.archidata.dataAccess.LazyGetter;
import org.atriasoft.archidata.dataAccess.QueryAnd;
import org.atriasoft.archidata.dataAccess.QueryCondition;
import org.atriasoft.archidata.dataAccess.QueryOptions;
import org.atriasoft.archidata.dataAccess.addOnSQL.model.LinkTableGeneric;
import org.atriasoft.archidata.dataAccess.options.Condition;
import org.atriasoft.archidata.dataAccess.options.OptionSpecifyType;
import org.atriasoft.archidata.dataAccess.options.OverrideTableName;
import org.atriasoft.archidata.exception.DataAccessException;
import org.atriasoft.archidata.tools.ConfigBaseVariable;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
			final DBAccessMongo ioDb,
			final Field field,
			final Object rootObject,
			final QueryOptions options,
			final Document docSet,
			final Document docUnSet) throws Exception {

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
		if (objectClass != Long.class && objectClass != UUID.class && objectClass != ObjectId.class) {
			return false;
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
	public void fillFromDoc(
			final DBAccessMongo ioDb,
			final Document doc,
			final Field field,
			final Object data,
			final QueryOptions options,
			final List<LazyGetter> lazyCall) throws Exception {
		/*
		if (field.getType() != List.class) {
			LOGGER.error("Can not ManyToMany with other than List Model: {}", field.getType().getCanonicalName());
			return;
		}

		final String fieldName = AnnotationTools.getFieldName(field);
		if (!doc.containsKey(fieldName)) {
			field.set(data, null);
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
			if (foreignKeyType == Long.class) {
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
		*/
	}

	@Override
	public boolean isUpdateAsync(final Field field) {
		return true;
	}

	@Override
	public void asyncUpdate(
			final DBAccessMongo ioDb,
			final Object previousData,
			final Object localKey,
			final Field field,
			final Object data,
			final List<LazyGetter> actions,
			final QueryOptions options) throws Exception {
		//		if (field.getType() != List.class) {
		//			LOGGER.error("Can not ManyToMany with other than List Model: {}", field.getType().getCanonicalName());
		//			return;
		//		}
		//		final Class<?> objectClass = (Class<?>) ((ParameterizedType) field.getGenericType())
		//				.getActualTypeArguments()[0];
		//		if (objectClass != Long.class && objectClass != UUID.class) {
		//			throw new DataAccessException("Can not ManyToMany with other than List<Long> or List<UUID> Model: List<"
		//					+ objectClass.getCanonicalName() + ">");
		//		}
		//		final FieldName columnName = AnnotationTools.getFieldName(field, options);
		//		final String linkTableName = generateLinkTableName(tableName, columnName.inTable());
		//
		//		actions.add(() -> {
		//			ioDb.deleteWhere(LinkTableGeneric.class, new OverrideTableName(linkTableName),
		//					new Condition(new QueryCondition("object1Id", "=", localKey)),
		//					new OptionSpecifyType("object1Id", localKey.getClass()),
		//					new OptionSpecifyType("object2Id", objectClass));
		//		});
		//		asyncInsert(ioDb, tableName, localKey, field, data, actions, options);
	}

	@Override
	public boolean isInsertAsync(final Field field) {
		return true;
	}

	@Override
	public void asyncInsert(
			final DBAccessMongo ioDb,
			final Class<?> clazz,
			final Object localKey,
			final Field field,
			final Object data,
			final List<LazyGetter> actions,
			final QueryOptions options) throws Exception {
		//		if (data == null) {
		//			return;
		//		}
		//		if (field.getType() != List.class) {
		//			LOGGER.error("Can not ManyToMany with other than List Model: {}", field.getType().getCanonicalName());
		//			return;
		//		}
		//		final Class<?> objectClass = (Class<?>) ((ParameterizedType) field.getGenericType())
		//				.getActualTypeArguments()[0];
		//		if (objectClass != Long.class && objectClass != UUID.class) {
		//			throw new DataAccessException("Can not ManyToMany with other than List<Long> or List<UUID> Model: List<"
		//					+ objectClass.getCanonicalName() + ">");
		//		}
		//		final FieldName columnName = AnnotationTools.getFieldName(field, options);
		//		final String linkTableName = generateLinkTableName(tableName, columnName.inTable());
		//
		//		@SuppressWarnings("unchecked")
		//		final List<Object> dataCasted = (List<Object>) data;
		//		if (dataCasted.size() == 0) {
		//			return;
		//		}
		//		final List<LinkTableGeneric> insertElements = new ArrayList<>();
		//		for (final Object remoteKey : dataCasted) {
		//			if (remoteKey == null) {
		//				throw new DataAccessException("Try to insert remote key with null value");
		//			}
		//			insertElements.add(new LinkTableGeneric(localKey, remoteKey));
		//		}
		//		if (insertElements.size() == 0) {
		//			LOGGER.warn("Insert multiple link without any value (may have null in the list): {}", dataCasted);
		//			return;
		//		}
		//		actions.add(() -> {
		//			ioDb.insertMultiple(insertElements, new OverrideTableName(linkTableName),
		//					new OptionSpecifyType("object1Id", localKey.getClass()),
		//					new OptionSpecifyType("object2Id", objectClass));
		//		});
	}

	@Override
	public void drop(final DBAccessMongo ioDb, final String tableName, final Field field, final QueryOptions options)
			throws Exception {
		final String columnName = AnnotationTools.getFieldName(field, options).inTable();
		final String linkTableName = generateLinkTableName(tableName, columnName);
		ioDb.drop(LinkTableGeneric.class, new OverrideTableName(linkTableName),
				new OptionSpecifyType("object1Id", Long.class), new OptionSpecifyType("object2Id", Long.class));
	}

	@Override
	public void cleanAll(
			final DBAccessMongo ioDb,
			final String tableName,
			final Field field,
			final QueryOptions options) throws Exception {
		final String columnName = AnnotationTools.getFieldName(field, options).inTable();
		final String linkTableName = generateLinkTableName(tableName, columnName);
		final Class<?> objectClass = (Class<?>) ((ParameterizedType) field.getGenericType())
				.getActualTypeArguments()[0];
		if (objectClass != Long.class && objectClass != UUID.class) {
			throw new DataAccessException("Can not ManyToMany with other than List<Long> or List<UUID> Model: List<"
					+ objectClass.getCanonicalName() + ">");
		}
		ioDb.cleanAll(LinkTableGeneric.class, new OverrideTableName(linkTableName),
				new OptionSpecifyType("object1Id", Long.class), new OptionSpecifyType("object2Id", Long.class));
	}

	public static void addLink(
			final DBAccess ioDb,
			final Class<?> clazz,
			final long localKey,
			final String column,
			final long remoteKey) throws Exception {
		if (ioDb instanceof final DBAccessMongo daSQL) {
			final String tableName = AnnotationTools.getTableName(clazz);
			final String linkTableName = generateLinkTableName(tableName, column);
			/* final Class<?> objectClass = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0]; if (objectClass != Long.class && objectClass != UUID.class) { throw new
			 * DataAccessException("Can not ManyToMany with other than List<Long> or List<UUID> Model: List<" + objectClass.getCanonicalName() + ">"); } */
			final LinkTableGeneric insertElement = new LinkTableGeneric(localKey, remoteKey);
			daSQL.insert(insertElement, new OverrideTableName(linkTableName),
					new OptionSpecifyType("object1Id", Long.class), new OptionSpecifyType("object2Id", Long.class));
		} else if (ioDb instanceof DBAccessMongo) {

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
		if (ioDb instanceof final DBAccessMongo daSQL) {
			final String tableName = AnnotationTools.getTableName(clazz);
			final String linkTableName = generateLinkTableName(tableName, column);
			return daSQL.deleteWhere(LinkTableGeneric.class, new OverrideTableName(linkTableName),
					new Condition(new QueryAnd(new QueryCondition("object1Id", "=", localKey),
							new QueryCondition("object2Id", "=", remoteKey))),
					new OptionSpecifyType("object1Id", Long.class), new OptionSpecifyType("object2Id", Long.class));
		} else if (ioDb instanceof DBAccessMongo) {
			return 0L;
		} else {
			throw new DataAccessException("DataAccess Not managed");
		}
	}
}
