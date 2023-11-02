package org.kar.archidata.dataAccess.addOn;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.kar.archidata.annotation.AnnotationTools;
import org.kar.archidata.dataAccess.DataAccess;
import org.kar.archidata.dataAccess.DataAccessAddOn;
import org.kar.archidata.dataAccess.DataFactory;
import org.kar.archidata.dataAccess.QueryAnd;
import org.kar.archidata.dataAccess.QueryCondition;
import org.kar.archidata.dataAccess.QueryOptions;
import org.kar.archidata.dataAccess.addOn.model.LinkTable;
import org.kar.archidata.util.ConfigBaseVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
	public int insertData(final PreparedStatement ps, final Field field, final Object rootObject, final int iii) throws SQLException, IllegalArgumentException, IllegalAccessException {
		return iii;
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

	@Override
	public int generateQuerry(@NotNull final String tableName, @NotNull final Field elem, @NotNull final StringBuilder querry, @NotNull final String name, @NotNull final int elemCount,
			final QueryOptions options) {
		final String linkTableName = generateLinkTableName(tableName, name);

		final String tmpVariable = "tmp_" + Integer.toString(elemCount);
		querry.append(" (SELECT GROUP_CONCAT(");
		querry.append(tmpVariable);
		querry.append(".object2Id");
		if (ConfigBaseVariable.getDBType().equals("sqlite")) {
			querry.append(", ");
		} else {
			querry.append("SEPARATOR ");
		}
		querry.append("'");
		querry.append(SEPARATOR);
		querry.append("') FROM ");
		querry.append(linkTableName);
		querry.append(" ");
		querry.append(tmpVariable);
		querry.append(" WHERE ");
		querry.append(tmpVariable);
		querry.append(".deleted = false AND ");
		querry.append(tableName);
		querry.append(".id = ");
		querry.append(tmpVariable);
		querry.append(".");
		querry.append("object1Id ");
		if (!ConfigBaseVariable.getDBType().equals("sqlite")) {
			querry.append(" GROUP BY ");
			querry.append(tmpVariable);
			querry.append(".object2Id");
		}
		querry.append(") AS ");
		querry.append(name);
		querry.append(" ");
		/*
		"              (SELECT GROUP_CONCAT(tmp.data_id SEPARATOR '-')" +
		"                      FROM cover_link_node tmp" +
		"                      WHERE tmp.deleted = false" +
		"                            AND node.id = tmp.node_id" +
		"                      GROUP BY tmp.node_id) AS covers" +
		*/
		return 1;
	}

	@Override
	public int fillFromQuerry(final ResultSet rs, final Field elem, final Object data, final int count, final QueryOptions options)
			throws SQLException, IllegalArgumentException, IllegalAccessException {
		final List<Long> idList = DataAccess.getListOfIds(rs, count, SEPARATOR);
		elem.set(data, idList);
		return 1;
	}

	public static void addLink(final Class<?> clazz, final long localKey, final String column, final long remoteKey) throws Exception {
		final String tableName = AnnotationTools.getTableName(clazz);
		final String linkTableName = generateLinkTableName(tableName, column);
		final LinkTable insertElement = new LinkTable(localKey, remoteKey);
		final QueryOptions options = new QueryOptions(QueryOptions.OVERRIDE_TABLE_NAME, linkTableName);
		DataAccess.insert(insertElement, options);

	}

	public static int removeLink(final Class<?> clazz, final long localKey, final String column, final long remoteKey) throws Exception {
		final String tableName = AnnotationTools.getTableName(clazz);
		final String linkTableName = generateLinkTableName(tableName, column);
		final QueryOptions options = new QueryOptions(QueryOptions.OVERRIDE_TABLE_NAME, linkTableName);
		final QueryAnd condition = new QueryAnd(new QueryCondition("object1Id", "=", localKey), new QueryCondition("object2Id", "=", remoteKey));
		return DataAccess.deleteWhere(LinkTable.class, condition, options);
	}

	@Override
	public void createTables(final String tableName, final Field field, final StringBuilder mainTableBuilder, final List<String> preActionList, final List<String> postActionList,
			final boolean createIfNotExist, final boolean createDrop, final int fieldId) throws Exception {
		final String linkTableName = generateLinkTableNameField(tableName, field);
		final QueryOptions options = new QueryOptions(QueryOptions.OVERRIDE_TABLE_NAME, linkTableName);
		final List<String> sqlCommand = DataFactory.createTable(LinkTable.class, options);
		postActionList.addAll(sqlCommand);
	}
}
