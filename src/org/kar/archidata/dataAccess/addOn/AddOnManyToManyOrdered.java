package org.kar.archidata.dataAccess.addOn;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import org.kar.archidata.GlobalConfiguration;
import org.kar.archidata.annotation.AnnotationTools;
import org.kar.archidata.annotation.addOn.DataAddOnManyToManyOrdered;
import org.kar.archidata.dataAccess.QueryOptions;
import org.kar.archidata.dataAccess.DataAccess;
import org.kar.archidata.dataAccess.DataAccessAddOn;
import org.kar.archidata.dataAccess.DataAccess.ExceptionDBInterface;
import org.kar.archidata.db.DBEntry;
import org.kar.archidata.util.ConfigBaseVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.validation.constraints.NotNull;

/**
 * Manage the decorator element @DataAddOnManyToManyOrdered to be injected in the DB.
 * The objective of this table is to manage a link between 2 table that have a specific order (Only work in 1 direction)
 */
public class AddOnManyToManyOrdered implements DataAccessAddOn {
	static final Logger LOGGER = LoggerFactory.getLogger(AddOnManyToManyOrdered.class);
	static final String SEPARATOR = "-";
	
	@Override
	public Class<?> getAnnotationClass() {
		return DataAddOnManyToManyOrdered.class;
	}
	
	@Override
	public String getSQLFieldType(final Field elem) {
		return null;
	}
	
	@Override
	public boolean isCompatibleField(final Field elem) {
		final DataAddOnManyToManyOrdered decorators = elem.getDeclaredAnnotation(DataAddOnManyToManyOrdered.class);
		return decorators != null;
	}
	
	@Override
	public int insertData(final PreparedStatement ps, final Object data, int iii) throws SQLException {
		return iii;
	}
	
	@Override
	public boolean isExternal() {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public int generateQuerry(@NotNull String tableName, @NotNull Field elem, @NotNull StringBuilder querry, @NotNull String name, @NotNull int elemCount, QueryOptions options) {
		String localName = name;
		if (name.endsWith("s")) {
			localName = name.substring(0, name.length() - 1);
		}
		final String tmpVariable = "tmp_" + Integer.toString(elemCount);
		querry.append(" (SELECT GROUP_CONCAT(");
		querry.append(tmpVariable);
		querry.append(".");
		querry.append(localName);
		querry.append("_id ");
		if (ConfigBaseVariable.getDBType().equals("sqlite")) {
			querry.append(", ");
		} else {
			querry.append("SEPARATOR ");
		}
		querry.append("'");
		querry.append(SEPARATOR);
		querry.append("') FROM ");
		querry.append(tableName);
		querry.append("_link_");
		querry.append(localName);
		querry.append(" ");
		querry.append(tmpVariable);
		querry.append(" WHERE ");
		querry.append(tmpVariable);
		querry.append(".deleted = false AND ");
		querry.append(tableName);
		querry.append(".id = ");
		querry.append(tmpVariable);
		querry.append(".");
		querry.append(tableName);
		querry.append("_id ORDER BY ");
		querry.append(tmpVariable);
		querry.append(".order ASC");
		querry.append(" GROUP BY ");
		querry.append(tmpVariable);
		querry.append(".");
		querry.append(tableName);
		querry.append("_id ) AS ");
		querry.append(name);
		querry.append(" ");
		return 1;
	}
	
	@Override
	public int fillFromQuerry(final ResultSet rs, final Field elem, final Object data, final int count, QueryOptions options) throws SQLException, IllegalArgumentException, IllegalAccessException {
		//throw new IllegalAccessException("This Add-on has not the capability to insert data directly in DB");
		return 0;
	}
	
	@Override
	public boolean canUpdate() {
		return false;
	}
	
	public static void addLink(final Class<?> clazz, final long localKey, final String table, final long remoteKey) throws Exception {
		final String tableName = AnnotationTools.getTableName(clazz);
		DBEntry entry = DBEntry.createInterface(GlobalConfiguration.dbConfig);
		long uniqueSQLID = -1;
		// real add in the BDD:
		try {
			// prepare the request:
			final String querry = "INSERT INTO " + tableName + "_link_" + table + " (create_date, modify_date, " + tableName + "_id, " + table + "_id)" + " VALUES (" + DataAccess.getDBNow() + ", "
					+ DataAccess.getDBNow() + ", ?, ?)";
			final PreparedStatement ps = entry.connection.prepareStatement(querry, Statement.RETURN_GENERATED_KEYS);
			int iii = 1;
			ps.setLong(iii++, localKey);
			ps.setLong(iii++, remoteKey);
			// execute the request
			final int affectedRows = ps.executeUpdate();
			if (affectedRows == 0) {
				throw new SQLException("Creating data failed, no rows affected.");
			}
			// retrieve uid inserted
			try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
				if (generatedKeys.next()) {
					uniqueSQLID = generatedKeys.getLong(1);
				} else {
					throw new SQLException("Creating user failed, no ID obtained (1).");
				}
			} catch (final Exception ex) {
				LOGGER.debug("Can not get the UID key inserted ... ");
				ex.printStackTrace();
				throw new SQLException("Creating user failed, no ID obtained (2).");
			}
		} catch (final SQLException ex) {
			ex.printStackTrace();
			throw new ExceptionDBInterface(500, "SQL error: " + ex.getMessage());
		} finally {
			entry.close();
			entry = null;
		}
	}
	
	public static void removeLink(final Class<?> clazz, final long localKey, final String table, final long remoteKey) throws Exception {
		final String tableName = AnnotationTools.getTableName(clazz);
		DBEntry entry = DBEntry.createInterface(GlobalConfiguration.dbConfig);
		final String querry = "UPDATE `" + tableName + "_link_" + table + "` SET `modify_date`=" + DataAccess.getDBNow() + ", `deleted`=true WHERE `" + tableName + "_id` = ? AND `" + table
				+ "_id` = ?";
		try {
			final PreparedStatement ps = entry.connection.prepareStatement(querry);
			int iii = 1;
			ps.setLong(iii++, localKey);
			ps.setLong(iii++, remoteKey);
			ps.executeUpdate();
		} catch (final SQLException ex) {
			ex.printStackTrace();
			throw new ExceptionDBInterface(500, "SQL error: " + ex.getMessage());
		} finally {
			entry.close();
			entry = null;
		}
	}
	
	// TODO : refacto this table to manage a generic table with dynamic name to be serializable with the default system
	@Override
	public void createTables(final String tableName, final Field elem, final StringBuilder mainTableBuilder, final List<String> preActionList, List<String> postActionList,
			final boolean createIfNotExist, final boolean createDrop, final int fieldId) throws Exception {
		final String name = AnnotationTools.getFieldName(elem);
		String localName = name;
		if (name.endsWith("s")) {
			localName = name.substring(0, name.length() - 1);
		}
		if (createIfNotExist && createDrop) {
			final StringBuilder tableTmp = new StringBuilder();
			tableTmp.append("DROP TABLE IF EXISTS `");
			tableTmp.append(tableName);
			tableTmp.append("_link_");
			tableTmp.append(localName);
			tableTmp.append("`;");
			postActionList.add(tableTmp.toString());
		}
		final StringBuilder otherTable = new StringBuilder();
		otherTable.append("CREATE TABLE `");
		otherTable.append(tableName);
		otherTable.append("_link_");
		otherTable.append(localName);
		otherTable.append("`(\n");
		if (!ConfigBaseVariable.getDBType().equals("sqlite")) {
			otherTable.append("\t\t`id` bigint NOT NULL AUTO_INCREMENT,\n");
			otherTable.append("\t\t`deleted` tinyint(1) NOT NULL DEFAULT '0',\n");
			otherTable.append("\t\t`createdAt` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),\n");
			otherTable.append("\t\t`updatedAt` timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),\n");
		} else {
			otherTable.append("\t\t`id` INTEGER PRIMARY KEY AUTOINCREMENT,\n");
			otherTable.append("\t\t`deleted` INTEGER NOT NULL DEFAULT '0',\n");
			otherTable.append("\t\t`createdAt` INTEGER NOT NULL DEFAULT CURRENT_TIMESTAMP,\n");
			otherTable.append("\t\t`updatedAt` INTEGER NOT NULL DEFAULT CURRENT_TIMESTAMP,\n");
		}
		otherTable.append("\t\t`");
		otherTable.append(tableName);
		if (!ConfigBaseVariable.getDBType().equals("sqlite")) {
			otherTable.append("_id` bigint NOT NULL,\n");
		} else {
			otherTable.append("_id` INTEGER NOT NULL,\n");
		}
		otherTable.append("\t\t`order` INTEGER NOT NULL DEFAULT 0,\n");
		otherTable.append("\t\t`");
		otherTable.append(localName);
		if (!ConfigBaseVariable.getDBType().equals("sqlite")) {
			otherTable.append("_id` bigint NOT NULL\n");
		} else {
			otherTable.append("_id` INTEGER NOT NULL\n");
		}
		if (!ConfigBaseVariable.getDBType().equals("sqlite")) {
			otherTable.append("\t, PRIMARY KEY (`id`)\n");
		}
		otherTable.append("\t)");
		if (!ConfigBaseVariable.getDBType().equals("sqlite")) {
			otherTable.append(" ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;\n\n");
		}
		otherTable.append(";");
		postActionList.add(otherTable.toString());
	}
}
