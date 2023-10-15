package org.kar.archidata.sqlWrapper.addOn;

import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.kar.archidata.GlobalConfiguration;
import org.kar.archidata.annotation.AnnotationTools;
import org.kar.archidata.db.DBEntry;
import org.kar.archidata.sqlWrapper.QuerryOptions;
import org.kar.archidata.sqlWrapper.SqlWrapper;
import org.kar.archidata.sqlWrapper.SqlWrapper.ExceptionDBInterface;
import org.kar.archidata.sqlWrapper.SqlWrapperAddOn;
import org.kar.archidata.sqlWrapper.StateLoad;
import org.kar.archidata.util.ConfigBaseVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.persistence.ManyToMany;

public class AddOnManyToMany implements SqlWrapperAddOn {
	static final Logger LOGGER = LoggerFactory.getLogger(AddOnManyToMany.class);
	
	/**
	 * Convert the list if external id in a string '-' separated
	 * @param ids List of value (null are removed)
	 * @return '-' string separated
	 */
	protected static String getStringOfIds(final List<Long> ids) {
		final List<Long> tmp = new ArrayList<>(ids);
		return tmp.stream().map(String::valueOf).collect(Collectors.joining("-"));
	}
	
	/**
	 * extract a list of "-" separated element from a SQL input data.
	 * @param rs Result Set of the BDD
	 * @param iii Id in the result set
	 * @return The list  of Long value
	 * @throws SQLException if an error is generated in the sql request.
	 */
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
	public int insertData(final PreparedStatement ps, final Object data, int iii) throws SQLException {
		if (data == null) {
			ps.setNull(iii++, Types.BIGINT);
		} else {
			// TODO: we must check if the model of data in a list of Long ... !!!!
			@SuppressWarnings("unchecked")
			final String dataTmp = getStringOfIds((List<Long>) data);
			ps.setString(iii++, dataTmp);
		}
		return iii++;
	}
	
	@Override
	public boolean isExternal() {
		// TODO Auto-generated method stub
		return false;
	}
	
	@Override
	public int generateQuerry(final String tableName, final Field elem, final StringBuilder querry, final String name, final List<StateLoad> autoClasify, QuerryOptions options) {
		
		autoClasify.add(StateLoad.ARRAY);
		String localName = name;
		if (name.endsWith("s")) {
			localName = name.substring(0, name.length() - 1);
		}
		final String tmpVariable = "tmp_" + Integer.toString(autoClasify.size());
		querry.append(" (SELECT GROUP_CONCAT(");
		querry.append(tmpVariable);
		querry.append(".");
		querry.append(localName);
		querry.append("_id SEPARATOR '-') FROM ");
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
		querry.append("_id GROUP BY ");
		querry.append(tmpVariable);
		querry.append(".");
		querry.append(tableName);
		querry.append("_id ) AS ");
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
	public int fillFromQuerry(final ResultSet rs, final Field elem, final Object data, final int count, QuerryOptions options) throws SQLException, IllegalArgumentException, IllegalAccessException {
		throw new IllegalAccessException("This Add-on has not the capability to insert data directly in DB");
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
			final String querry = "INSERT INTO " + tableName + "_link_" + table + " (create_date, modify_date, " + tableName + "_id, " + table + "_id)" + " VALUES (" + SqlWrapper.getDBNow() + ", "
					+ SqlWrapper.getDBNow() + ", ?, ?)";
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
		final String querry = "UPDATE `" + tableName + "_link_" + table + "` SET `modify_date`=" + SqlWrapper.getDBNow() + ", `deleted`=true WHERE `" + tableName + "_id` = ? AND `" + table
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
	public void createTables(final String tableName, final Field elem, final StringBuilder mainTableBuilder, final List<String> ListOtherTables, final boolean createIfNotExist,
			final boolean createDrop, final int fieldId) throws Exception {
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
			ListOtherTables.add(tableTmp.toString());
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
		ListOtherTables.add(otherTable.toString());
	}
}
