package org.kar.archidata.dataAccess.addOn;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.kar.archidata.annotation.AnnotationTools;
import org.kar.archidata.dataAccess.CountInOut;
import org.kar.archidata.dataAccess.DataAccess;
import org.kar.archidata.dataAccess.DataAccessAddOn;
import org.kar.archidata.dataAccess.DataFactory;
import org.kar.archidata.dataAccess.LazyGetter;
import org.kar.archidata.dataAccess.QueryAnd;
import org.kar.archidata.dataAccess.QueryCondition;
import org.kar.archidata.dataAccess.QueryInList;
import org.kar.archidata.dataAccess.QueryOptions;
import org.kar.archidata.dataAccess.addOn.model.LinkTable;
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
	static final String SEPARATOR = "-";

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
	public void insertData(final PreparedStatement ps, final Field field, final Object rootObject, final CountInOut iii) throws SQLException, IllegalArgumentException, IllegalAccessException {

	}

	@Override
	public boolean canInsert(final Field field) {
		return false;
	}

	@Override
	public boolean canRetrieve(final Field field) {
		return true;
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

	public void generateConcatQuerry(@NotNull final String tableName, @NotNull final Field field, @NotNull final StringBuilder querrySelect, @NotNull final StringBuilder querry,
			@NotNull final String name, @NotNull final CountInOut elemCount, final QueryOptions options) {

		final String linkTableName = generateLinkTableName(tableName, name);
		final String tmpVariable = "tmp_" + Integer.toString(elemCount.value);
		querrySelect.append(" (SELECT GROUP_CONCAT(");
		querrySelect.append(tmpVariable);
		querrySelect.append(".object2Id ");
		if ("sqlite".equals(ConfigBaseVariable.getDBType())) {
			querrySelect.append(", ");
		} else {
			querrySelect.append("SEPARATOR ");
		}
		querrySelect.append("'");
		querrySelect.append(SEPARATOR);
		querrySelect.append("') FROM ");
		querrySelect.append(linkTableName);
		querrySelect.append(" ");
		querrySelect.append(tmpVariable);
		querrySelect.append(" WHERE ");
		/* querrySelect.append(tmpVariable); querrySelect.append(".deleted = false AND "); */
		querrySelect.append(tableName);
		querrySelect.append(".id = ");
		querrySelect.append(tmpVariable);
		querrySelect.append(".");
		querrySelect.append("object1Id ");
		if (!"sqlite".equals(ConfigBaseVariable.getDBType())) {
			querrySelect.append(" GROUP BY ");
			querrySelect.append(tmpVariable);
			querrySelect.append(".object1Id");
		}
		querrySelect.append(") AS ");
		querrySelect.append(name);
		querrySelect.append(" ");
		/* "              (SELECT GROUP_CONCAT(tmp.data_id SEPARATOR '-')" + "                      FROM cover_link_node tmp" + "                      WHERE tmp.deleted = false" +
		 * "                            AND node.id = tmp.node_id" + "                      GROUP BY tmp.node_id) AS covers" + */
		elemCount.inc();
	}

	@Override
	public void generateQuerry(@NotNull final String tableName, @NotNull final Field field, @NotNull final StringBuilder querrySelect, @NotNull final StringBuilder querry, @NotNull final String name,
			@NotNull final CountInOut elemCount, final QueryOptions options) throws Exception {
		if (field.getType() != List.class) {
			return;
		}
		final Class<?> objectClass = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
		if (objectClass == Long.class) {
			generateConcatQuerry(tableName, field, querrySelect, querry, name, elemCount, options);
		}
		final ManyToMany decorators = field.getDeclaredAnnotation(ManyToMany.class);
		if (decorators == null) {
			return;
		}
		if (objectClass == decorators.targetEntity()) {
			if (decorators.fetch() == FetchType.EAGER) {
				throw new DataAccessException("EAGER is not supported for list of element...");
			} else {
				generateConcatQuerry(tableName, field, querrySelect, querry, name, elemCount, options);
			}
		}
	}

	@Override
	public void fillFromQuerry(final ResultSet rs, final Field field, final Object data, final CountInOut count, final QueryOptions options, final List<LazyGetter> lazyCall) throws Exception {
		if (field.getType() != List.class) {
			LOGGER.error("Can not ManyToMany with other than List Model: {}", field.getType().getCanonicalName());
			return;
		}
		final Class<?> objectClass = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
		if (objectClass == Long.class) {
			final List<Long> idList = DataAccess.getListOfIds(rs, count.value, SEPARATOR);
			field.set(data, idList);
			count.inc();
		} else {
			LOGGER.error("Can not ManyToMany with other than List<Long> Model: List<{}>", objectClass.getCanonicalName());
			return;
		}
		final ManyToMany decorators = field.getDeclaredAnnotation(ManyToMany.class);
		if (decorators == null) {
			return;
		}
		if (objectClass == decorators.targetEntity()) {
			if (decorators.fetch() == FetchType.EAGER) {
				throw new DataAccessException("EAGER is not supported for list of element...");
			} else {
				final List<Long> idList = DataAccess.getListOfIds(rs, count.value, SEPARATOR);
				// field.set(data, idList);
				count.inc();
				if (idList != null && idList.size() > 0) {
					final String idField = AnnotationTools.getFieldName(AnnotationTools.getIdField(objectClass));
					// In the lazy mode, the request is done in asynchronous mode, they will be done after...
					final LazyGetter lambda = () -> {
						final List<Long> childs = new ArrayList<>(idList);
						// TODO: update to have get with abstract types ....
						@SuppressWarnings("unchecked")
						final Object foreignData = DataAccess.getsWhere(decorators.targetEntity(), new Condition(new QueryInList<>(idField, childs)));
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
	public void asyncUpdate(final String tableName, final Object localKey, final Field field, final Object data, final List<LazyGetter> actions) throws Exception {
		if (field.getType() != List.class) {
			LOGGER.error("Can not ManyToMany with other than List Model: {}", field.getType().getCanonicalName());
			return;
		}
		final String columnName = AnnotationTools.getFieldName(field);
		final String linkTableName = generateLinkTableName(tableName, columnName);
		actions.add(() -> {
			DataAccess.deleteWhere(LinkTable.class, new OverrideTableName(linkTableName), new Condition(new QueryCondition("object1Id", "=", localKey)));
		});
		asyncInsert(tableName, localKey, field, data, actions);
	}

	@Override
	public boolean isInsertAsync(final Field field) {
		return true;
	}

	@Override
	public void asyncInsert(final String tableName, final Object localKey, final Field field, final Object data, final List<LazyGetter> actions) throws Exception {
		if (data == null) {
			return;
		}
		if (field.getType() != List.class) {
			LOGGER.error("Can not ManyToMany with other than List Model: {}", field.getType().getCanonicalName());
			return;
		}
		final String columnName = AnnotationTools.getFieldName(field);
		final String linkTableName = generateLinkTableName(tableName, columnName);
		final Class<?> objectClass = (Class<?>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
		if (objectClass != Long.class) {
			LOGGER.error("Can not ManyToMany with other than List<Long> Model: List<{}>", objectClass.getCanonicalName());
			return;
		}
		@SuppressWarnings("unchecked")
		final List<Long> dataCasted = (List<Long>) data;
		if (dataCasted.size() == 0) {
			return;
		}
		final List<LinkTable> insertElements = new ArrayList<>();
		for (final Long remoteKey : dataCasted) {
			if (remoteKey == null) {
				continue;
			}
			if (localKey instanceof final Long localKeyLong) {
				insertElements.add(new LinkTable(localKeyLong, remoteKey));
			} else {
				throw new DataAccessException("Not manage access of remte key like ManyToMany other than Long: " + localKey.getClass().getCanonicalName());
			}
		}
		if (insertElements.size() == 0) {
			LOGGER.warn("Insert multiple link without any value (may have null in the list): {}", dataCasted);
			return;
		}
		DataAccess.insertMultiple(insertElements, new OverrideTableName(linkTableName));
	}

	public static void addLink(final Class<?> clazz, final long localKey, final String column, final long remoteKey) throws Exception {
		final String tableName = AnnotationTools.getTableName(clazz);
		final String linkTableName = generateLinkTableName(tableName, column);
		final LinkTable insertElement = new LinkTable(localKey, remoteKey);
		DataAccess.insert(insertElement, new OverrideTableName(linkTableName));

	}

	public static int removeLink(final Class<?> clazz, final long localKey, final String column, final long remoteKey) throws Exception {
		final String tableName = AnnotationTools.getTableName(clazz);
		final String linkTableName = generateLinkTableName(tableName, column);
		return DataAccess.deleteWhere(LinkTable.class, new OverrideTableName(linkTableName),
				new Condition(new QueryAnd(new QueryCondition("object1Id", "=", localKey), new QueryCondition("object2Id", "=", remoteKey))));
	}

	@Override
	public void createTables(final String tableName, final Field field, final StringBuilder mainTableBuilder, final List<String> preActionList, final List<String> postActionList,
			final boolean createIfNotExist, final boolean createDrop, final int fieldId) throws Exception {
		final String linkTableName = generateLinkTableNameField(tableName, field);
		final QueryOptions options = new QueryOptions(new OverrideTableName(linkTableName));
		final List<String> sqlCommand = DataFactory.createTable(LinkTable.class, options);
		postActionList.addAll(sqlCommand);
	}
}
