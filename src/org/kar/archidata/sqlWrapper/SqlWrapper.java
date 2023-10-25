package org.kar.archidata.sqlWrapper;

import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.kar.archidata.GlobalConfiguration;
import org.kar.archidata.annotation.AnnotationTools;
import org.kar.archidata.annotation.CreationTimestamp;
import org.kar.archidata.annotation.DataAddOn;
import org.kar.archidata.annotation.SQLDefault;
import org.kar.archidata.annotation.SQLIfNotExists;
import org.kar.archidata.annotation.UpdateTimestamp;
import org.kar.archidata.db.DBEntry;
import org.kar.archidata.sqlWrapper.addOn.AddOnManyToMany;
import org.kar.archidata.sqlWrapper.addOn.AddOnManyToOne;
import org.kar.archidata.sqlWrapper.addOn.AddOnSQLTableExternalForeinKeyAsList;
import org.kar.archidata.util.ConfigBaseVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.GenerationType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.ws.rs.InternalServerErrorException;

public class SqlWrapper {
	static final Logger LOGGER = LoggerFactory.getLogger(SqlWrapper.class);
	static final List<SqlWrapperAddOn> addOn = new ArrayList<>();
	
	static {
		addOn.add(new AddOnManyToMany());
		addOn.add(new AddOnManyToOne());
		addOn.add(new AddOnSQLTableExternalForeinKeyAsList());
	}
	
	public static void addAddOn(final SqlWrapperAddOn addOn) {
		SqlWrapper.addOn.add(addOn);
	}
	
	public static class ExceptionDBInterface extends Exception {
		private static final long serialVersionUID = 1L;
		public int errorID;
		
		public ExceptionDBInterface(final int errorId, final String message) {
			super(message);
			this.errorID = errorId;
		}
	}
	
	public SqlWrapper() {
		
	}
	
	public static boolean isDBExist(final String name) throws InternalServerErrorException {
		if ("sqlite".equals(ConfigBaseVariable.getDBType())) {
			// no base manage in sqLite ...
			// TODO: check if the file exist or not ...
			return true;
		}
		DBEntry entry;
		try {
			entry = DBEntry.createInterface(GlobalConfiguration.dbConfig, true);
		} catch (final IOException ex) {
			// TODO Auto-generated catch block
			ex.printStackTrace();
			LOGGER.error("Can not check if the DB exist!!! {}", ex.getMessage());
			return false;
		}
		try {
			// TODO : Maybe connect with a temporary not specified connection interface to a db ...
			final PreparedStatement ps = entry.connection.prepareStatement("show databases");
			final ResultSet rs = ps.executeQuery();
			//LOGGER.info("List all tables:      equals? '{}'", name);
			while (rs.next()) {
				final String data = rs.getString(1);
				//LOGGER.info("  - '{}'", data);
				if (name.equals(data)) {
					return true;
				}
			}
			return false;
		} catch (final SQLException ex) {
			LOGGER.error("Can not check if the DB exist SQL-error !!! {}", ex.getMessage());
		} finally {
			try {
				entry.close();
			} catch (final IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			entry = null;
		}
		throw new InternalServerErrorException("Can Not manage the DB-access");
	}
	
	public static boolean createDB(final String name) {
		if ("sqlite".equals(ConfigBaseVariable.getDBType())) {
			// no base manage in sqLite ...
			// TODO: check if the file exist or not ...
			return true;
		}
		try {
			return 1 == SqlWrapper.executeSimpleQuerry("CREATE DATABASE `" + name + "`;", true);
		} catch (final SQLException | IOException ex) {
			ex.printStackTrace();
			LOGGER.error("Can not check if the DB exist!!! {}", ex.getMessage());
			return false;
		}
	}
	
	public static boolean isTableExist(final String name) throws InternalServerErrorException {
		try {
			String request = "";
			if ("sqlite".equals(ConfigBaseVariable.getDBType())) {
				request = """
						SELECT COUNT(*) AS total
						FROM sqlite_master
						WHERE type = 'table'
						AND name = ?;
						""";
				//  PreparedStatement ps = entry.connection.prepareStatement("show tables");
				final DBEntry entry = DBEntry.createInterface(GlobalConfiguration.dbConfig);
				final PreparedStatement ps = entry.connection.prepareStatement(request);
				ps.setString(1, name);
				final ResultSet ret = ps.executeQuery();
				final int count = ret.getInt("total");
				return count == 1;
			} else {
				final DBEntry entry = DBEntry.createInterface(GlobalConfiguration.dbConfig);
				// TODO : Maybe connect with a temporary not specified connection interface to a db ...
				final PreparedStatement ps = entry.connection.prepareStatement("show tables");
				final ResultSet rs = ps.executeQuery();
				//LOGGER.info("List all tables:      equals? '{}'", name);
				while (rs.next()) {
					final String data = rs.getString(1);
					//LOGGER.info("  - '{}'", data);
					if (name.equals(data)) {
						return true;
					}
				}
				return false;
			}
		} catch (final SQLException ex) {
			LOGGER.error("Can not check if the table exist SQL-error !!! {}", ex.getMessage());
		} catch (final IOException ex) {
			LOGGER.error("Can not check if the table exist!!! {}", ex.getMessage());
		}
		throw new InternalServerErrorException("Can Not manage the DB-access");
	}
	
	/**
	 * extract a list of "-" separated element from a SQL input data.
	 * @param rs Result Set of the BDD
	 * @param iii Id in the result set
	 * @return The list  of Long value
	 * @throws SQLException if an error is generated in the sql request.
	 */
	public static List<Long> getListOfIds(ResultSet rs, int iii, String separator) throws SQLException {
		String trackString = rs.getString(iii);
		if (rs.wasNull()) {
			return null;
		}
		List<Long> out = new ArrayList<>();
		String[] elements = trackString.split("-");
		for (String elem : elements) {
			Long tmp = Long.parseLong(elem);
			out.add(tmp);
		}
		return out;
	}
	
	public static String convertTypeInSQL(final Class<?> type) throws Exception {
		if (!"sqlite".equals(ConfigBaseVariable.getDBType())) {
			if (type == Long.class || type == long.class) {
				return "bigint";
			}
			if (type == Integer.class || type == int.class) {
				return "int";
			}
			if (type == Boolean.class || type == boolean.class) {
				return "tinyint(1)";
			}
			if (type == Float.class || type == float.class) {
				return "float";
			}
			if (type == Double.class || type == double.class) {
				return "double";
			}
			if (type == Timestamp.class) {
				return "timestamp(3)";
			}
			if (type == Date.class) {
				return "date";
			}
			if (type == String.class) {
				return "text";
			}
		} else {
			if (type == Long.class || type == long.class) {
				return "INTEGER";
			}
			if (type == Integer.class || type == int.class) {
				return "INTEGER";
			}
			if (type == Boolean.class || type == boolean.class) {
				return "INTEGER";
			}
			if (type == Float.class || type == float.class) {
				return "REAL";
			}
			if (type == Double.class || type == double.class) {
				return "REAL";
			}
			if (type == Timestamp.class) {
				return "DATETIME";
			}
			if (type == Date.class) {
				return "DATETIME";
			}
			if (type == String.class) {
				return "text";
			}
		}
		throw new Exception("Imcompatible type of element in object for: " + type.getCanonicalName());
	}
	
	protected static <T> void setValuedb(final Class<?> type, final T data, int index, final Field field, final PreparedStatement ps)
			throws IllegalArgumentException, IllegalAccessException, SQLException {
		if (type == Long.class) {
			final Object tmp = field.get(data);
			if (tmp == null) {
				ps.setNull(index++, Types.BIGINT);
			} else {
				ps.setLong(index++, (Long) tmp);
			}
		} else if (type == long.class) {
			ps.setLong(index++, field.getLong(data));
		} else if (type == Integer.class) {
			final Object tmp = field.get(data);
			if (tmp == null) {
				ps.setNull(index++, Types.INTEGER);
			} else {
				ps.setInt(index++, (Integer) tmp);
			}
		} else if (type == int.class) {
			ps.setInt(index++, field.getInt(data));
		} else if (type == Float.class) {
			final Object tmp = field.get(data);
			if (tmp == null) {
				ps.setNull(index++, Types.FLOAT);
			} else {
				ps.setFloat(index++, (Float) tmp);
			}
		} else if (type == float.class) {
			ps.setFloat(index++, field.getFloat(data));
		} else if (type == Double.class) {
			final Object tmp = field.get(data);
			if (tmp == null) {
				ps.setNull(index++, Types.DOUBLE);
			} else {
				ps.setDouble(index++, (Double) tmp);
			}
		} else if (type == Double.class) {
			ps.setDouble(index++, field.getDouble(data));
		} else if (type == Boolean.class) {
			final Object tmp = field.get(data);
			if (tmp == null) {
				ps.setNull(index++, Types.INTEGER);
			} else {
				ps.setBoolean(index++, (Boolean) tmp);
			}
		} else if (type == boolean.class) {
			ps.setBoolean(index++, field.getBoolean(data));
		} else if (type == Timestamp.class) {
			final Object tmp = field.get(data);
			if (tmp == null) {
				ps.setNull(index++, Types.INTEGER);
			} else {
				ps.setTimestamp(index++, (Timestamp) tmp);
			}
		} else if (type == Date.class) {
			final Object tmp = field.get(data);
			if (tmp == null) {
				ps.setNull(index++, Types.INTEGER);
			} else {
				ps.setDate(index++, (Date) tmp);
			}
		} else if (type == String.class) {
			final Object tmp = field.get(data);
			if (tmp == null) {
				ps.setNull(index++, Types.VARCHAR);
			} else {
				ps.setString(index++, (String) tmp);
			}
		}
	}
	
	protected static <T> void setValueFromDb(final Class<?> type, final T data, final int index, final Field field, final ResultSet rs)
			throws IllegalArgumentException, IllegalAccessException, SQLException {
		if (type == Long.class) {
			final Long tmp = rs.getLong(index);
			if (rs.wasNull()) {
				field.set(data, null);
			} else {
				//logger.debug("       ==> {}", tmp);
				field.set(data, tmp);
			}
		} else if (type == long.class) {
			final Long tmp = rs.getLong(index);
			if (rs.wasNull()) {
				//field.set(data, null);
			} else {
				field.setLong(data, tmp);
			}
		} else if (type == Integer.class) {
			final Integer tmp = rs.getInt(index);
			if (rs.wasNull()) {
				field.set(data, null);
			} else {
				field.set(data, tmp);
			}
		} else if (type == int.class) {
			final Integer tmp = rs.getInt(index);
			if (rs.wasNull()) {
				//field.set(data, null);
			} else {
				field.setInt(data, tmp);
			}
		} else if (type == Float.class) {
			final Float tmp = rs.getFloat(index);
			if (rs.wasNull()) {
				field.set(data, null);
			} else {
				field.set(data, tmp);
			}
		} else if (type == float.class) {
			final Float tmp = rs.getFloat(index);
			if (rs.wasNull()) {
				//field.set(data, null);
			} else {
				field.setFloat(data, tmp);
			}
		} else if (type == Double.class) {
			final Double tmp = rs.getDouble(index);
			if (rs.wasNull()) {
				field.set(data, null);
			} else {
				field.set(data, tmp);
			}
		} else if (type == double.class) {
			final Double tmp = rs.getDouble(index);
			if (rs.wasNull()) {
				//field.set(data, null);
			} else {
				field.setDouble(data, tmp);
			}
		} else if (type == Boolean.class) {
			final Boolean tmp = rs.getBoolean(index);
			if (rs.wasNull()) {
				field.set(data, null);
			} else {
				field.set(data, tmp);
			}
		} else if (type == boolean.class) {
			final Boolean tmp = rs.getBoolean(index);
			if (rs.wasNull()) {
				//field.set(data, null);
			} else {
				field.setBoolean(data, tmp);
			}
		} else if (type == Timestamp.class) {
			try {
				final Timestamp tmp = rs.getTimestamp(index);
				if (rs.wasNull()) {
					field.set(data, null);
				} else {
					field.set(data, tmp);
				}
			} catch (java.sql.SQLException ex) {
				try {
					final Date tmp = rs.getDate(index);
					if (rs.wasNull()) {
						
						field.set(data, null);
					} else {
						field.set(data, new Timestamp(tmp.toInstant().toEpochMilli()));
					}
				} catch (java.sql.SQLException ex2) {
					final String tmp = rs.getString(index);
					LOGGER.error("plop {}", tmp);
				}
			}
		} else if (type == Date.class) {
			final Date tmp = rs.getDate(index);
			if (rs.wasNull()) {
				field.set(data, null);
			} else {
				field.set(data, tmp);
			}
		} else if (type == String.class) {
			final String tmp = rs.getString(index);
			if (rs.wasNull()) {
				field.set(data, null);
			} else {
				field.set(data, tmp);
			}
		}
	}
	
	public static boolean isAddOnField(final Field field) {
		boolean ret = AnnotationTools.isAnnotationGroup(field, DataAddOn.class);
		if (ret) {
			return true;
		}
		// The specific element of the JPA manage fy generic add-on system:
		if (field.getDeclaredAnnotationsByType(ManyToMany.class).length != 0) {
			return true;
		}
		if (field.getDeclaredAnnotationsByType(ManyToOne.class).length != 0) {
			return true;
		}
		return ret;
	}
	
	public static SqlWrapperAddOn findAddOnforField(final Field field) {
		for (final SqlWrapperAddOn elem : addOn) {
			if (elem.isCompatibleField(field)) {
				return elem;
			}
		}
		return null;
	}
	
	public static <T> T insert(final T data) throws Exception {
		final Class<?> clazz = data.getClass();
		//public static NodeSmall createNode(String typeInNode, String name, String descrition, Long parentId) {
		
		DBEntry entry = DBEntry.createInterface(GlobalConfiguration.dbConfig);
		// real add in the BDD:
		try {
			final String tableName = AnnotationTools.getTableName(clazz);
			//boolean createIfNotExist = clazz.getDeclaredAnnotationsByType(SQLIfNotExists.class).length != 0;
			final StringBuilder querry = new StringBuilder();
			querry.append("INSERT INTO `");
			querry.append(tableName);
			querry.append("` (");
			
			boolean firstField = true;
			int count = 0;
			for (final Field elem : clazz.getFields()) {
				// static field is only for internal global declaration ==> remove it ..
				if (java.lang.reflect.Modifier.isStatic(elem.getModifiers())) {
					continue;
				}
				if (AnnotationTools.isPrimaryKey(elem)) {
					continue;
				}
				final SqlWrapperAddOn addOn = findAddOnforField(elem);
				if (addOn != null && addOn.isExternal()) {
					continue;
				}
				final boolean createTime = elem.getDeclaredAnnotationsByType(CreationTimestamp.class).length != 0;
				if (createTime) {
					continue;
				}
				final boolean updateTime = elem.getDeclaredAnnotationsByType(UpdateTimestamp.class).length != 0;
				if (updateTime) {
					continue;
				}
				if (!elem.getClass().isPrimitive()) {
					final Object tmp = elem.get(data);
					if (tmp == null && elem.getDeclaredAnnotationsByType(SQLDefault.class).length != 0) {
						continue;
					}
				}
				count++;
				final String name = AnnotationTools.getFieldName(elem);
				if (firstField) {
					firstField = false;
				} else {
					querry.append(",");
				}
				querry.append(" `");
				querry.append(name);
				querry.append("`");
			}
			firstField = true;
			querry.append(") VALUES (");
			for (int iii = 0; iii < count; iii++) {
				if (firstField) {
					firstField = false;
				} else {
					querry.append(",");
				}
				querry.append("?");
			}
			querry.append(")");
			//LOGGER.warn("generate the querry: '{}'", querry.toString());
			// prepare the request:
			final PreparedStatement ps = entry.connection.prepareStatement(querry.toString(), Statement.RETURN_GENERATED_KEYS);
			Field primaryKeyField = null;
			int iii = 1;
			for (final Field elem : clazz.getFields()) {
				// static field is only for internal global declaration ==> remove it ..
				if (java.lang.reflect.Modifier.isStatic(elem.getModifiers())) {
					continue;
				}
				if (AnnotationTools.isPrimaryKey(elem)) {
					primaryKeyField = elem;
					continue;
				}
				final SqlWrapperAddOn addOn = findAddOnforField(elem);
				if (addOn != null && addOn.isExternal()) {
					continue;
				}
				final boolean createTime = elem.getDeclaredAnnotationsByType(CreationTimestamp.class).length != 0;
				if (createTime) {
					continue;
				}
				final boolean updateTime = elem.getDeclaredAnnotationsByType(UpdateTimestamp.class).length != 0;
				if (updateTime) {
					continue;
				}
				if (addOn != null) {
					// Add-on specific insertion.
					final Object tmp = elem.get(data);
					iii = addOn.insertData(ps, tmp, iii);
				} else {
					// Generic class insertion...
					final Class<?> type = elem.getType();
					if (!type.isPrimitive()) {
						final Object tmp = elem.get(data);
						if (tmp == null && elem.getDeclaredAnnotationsByType(SQLDefault.class).length != 0) {
							continue;
						}
					}
					setValuedb(type, data, iii++, elem, ps);
				}
				count++;
			}
			// execute the request
			final int affectedRows = ps.executeUpdate();
			if (affectedRows == 0) {
				throw new SQLException("Creating node failed, no rows affected.");
			}
			Long uniqueSQLID = null;
			// Retrieve uid inserted
			try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
				if (generatedKeys.next()) {
					uniqueSQLID = generatedKeys.getLong(1);
				} else {
					throw new SQLException("Creating node failed, no ID obtained (1).");
				}
			} catch (final Exception ex) {
				LOGGER.error("Can not get the UID key inserted ... ");
				ex.printStackTrace();
				throw new SQLException("Creating node failed, no ID obtained (2).");
			}
			if (primaryKeyField != null) {
				if (primaryKeyField.getType() == Long.class) {
					primaryKeyField.set(data, uniqueSQLID);
				} else if (primaryKeyField.getType() == long.class) {
					primaryKeyField.setLong(data, uniqueSQLID);
				} else {
					LOGGER.error("Can not manage the primary filed !!!");
				}
			}
			//ps.execute();
		} catch (final SQLException ex) {
			ex.printStackTrace();
		} finally {
			entry.close();
			entry = null;
		}
		return data;
	}
	
	// seems a good idea, but very dangerous if we not filter input data... if set an id it can be complicated...
	public static <T> T insertWithJson(final Class<T> clazz, final String jsonData) throws Exception {
		final ObjectMapper mapper = new ObjectMapper();
		// parse the object to be sure the data are valid:
		final T data = mapper.readValue(jsonData, clazz);
		
		return insert(data);
	}
	
	public static <T> int update(final Class<T> clazz, final long id, final String jsonData) throws Exception {
		final ObjectMapper mapper = new ObjectMapper();
		// parse the object to be sure the data are valid:
		final T data = mapper.readValue(jsonData, clazz);
		// Read the tree to filter injection of data:
		final JsonNode root = mapper.readTree(jsonData);
		final List<String> keys = new ArrayList<>();
		final Iterator<String> iterator = root.fieldNames();
		iterator.forEachRemaining(e -> keys.add(e));
		return update(data, id, keys);
	}
	
	/**
	 *
	 * @param <T>
	 * @param data
	 * @param id
	 * @param filterValue
	 * @return the affected rows.
	 * @throws Exception
	 */
	public static <T> int update(final T data, final long id, final List<String> filterValue) throws Exception {
		final Class<?> clazz = data.getClass();
		//public static NodeSmall createNode(String typeInNode, String name, String description, Long parentId) {
		
		DBEntry entry = DBEntry.createInterface(GlobalConfiguration.dbConfig);
		// real add in the BDD:
		try {
			final String tableName = AnnotationTools.getTableName(clazz);
			//boolean createIfNotExist = clazz.getDeclaredAnnotationsByType(SQLIfNotExists.class).length != 0;
			final StringBuilder querry = new StringBuilder();
			querry.append("UPDATE `");
			querry.append(tableName);
			querry.append("` SET ");
			
			boolean firstField = true;
			Field primaryKeyField = null;
			for (final Field elem : clazz.getFields()) {
				// static field is only for internal global declaration ==> remove it ..
				if (java.lang.reflect.Modifier.isStatic(elem.getModifiers())) {
					continue;
				}
				if (AnnotationTools.isPrimaryKey(elem)) {
					primaryKeyField = elem;
					continue;
				}
				final SqlWrapperAddOn addOn = findAddOnforField(elem);
				if (addOn != null && addOn.isExternal()) {
					continue;
				}
				final boolean createTime = elem.getDeclaredAnnotationsByType(CreationTimestamp.class).length != 0;
				if (createTime) {
					continue;
				}
				final String name = AnnotationTools.getFieldName(elem);
				final boolean updateTime = elem.getDeclaredAnnotationsByType(UpdateTimestamp.class).length != 0;
				if (!updateTime && !filterValue.contains(name)) {
					continue;
				}
				if (!elem.getClass().isPrimitive()) {
					final Object tmp = elem.get(data);
					if (tmp == null && elem.getDeclaredAnnotationsByType(SQLDefault.class).length != 0) {
						continue;
					}
				}
				if (firstField) {
					firstField = false;
				} else {
					querry.append(",");
				}
				querry.append(" `");
				querry.append(name);
				querry.append("` = ");
				if (updateTime) {
					querry.append(getDBNow());
					querry.append(" ");
				} else {
					querry.append("? ");
				}
			}
			querry.append(" WHERE `");
			querry.append(AnnotationTools.getFieldName(primaryKeyField));
			querry.append("` = ?");
			firstField = true;
			// logger.debug("generate the querry: '{}'", querry.toString());
			// prepare the request:
			final PreparedStatement ps = entry.connection.prepareStatement(querry.toString(), Statement.RETURN_GENERATED_KEYS);
			int iii = 1;
			for (final Field elem : clazz.getFields()) {
				// static field is only for internal global declaration ==> remove it ..
				if (java.lang.reflect.Modifier.isStatic(elem.getModifiers())) {
					continue;
				}
				if (AnnotationTools.isPrimaryKey(elem)) {
					continue;
				}
				final SqlWrapperAddOn addOn = findAddOnforField(elem);
				if (addOn != null && !addOn.canUpdate()) {
					continue;
				}
				final boolean createTime = elem.getDeclaredAnnotationsByType(CreationTimestamp.class).length != 0;
				if (createTime) {
					continue;
				}
				final String name = AnnotationTools.getFieldName(elem);
				final boolean updateTime = elem.getDeclaredAnnotationsByType(UpdateTimestamp.class).length != 0;
				if (updateTime || !filterValue.contains(name)) {
					continue;
				}
				if (addOn == null) {
					final Class<?> type = elem.getType();
					if (!type.isPrimitive()) {
						final Object tmp = elem.get(data);
						if (tmp == null && elem.getDeclaredAnnotationsByType(SQLDefault.class).length != 0) {
							continue;
						}
					}
					setValuedb(type, data, iii++, elem, ps);
				} else {
					iii = addOn.insertData(ps, data, iii);
				}
			}
			ps.setLong(iii++, id);
			return ps.executeUpdate();
		} catch (final SQLException ex) {
			ex.printStackTrace();
		} finally {
			entry.close();
			entry = null;
		}
		return 0;
	}
	
	static void addElement(final PreparedStatement ps, final Object value, final int iii) throws Exception {
		if (value.getClass() == Long.class) {
			ps.setLong(iii, (Long) value);
		} else if (value.getClass() == Integer.class) {
			ps.setInt(iii, (Integer) value);
		} else if (value.getClass() == String.class) {
			ps.setString(iii, (String) value);
		} else if (value.getClass() == Short.class) {
			ps.setShort(iii, (Short) value);
		} else if (value.getClass() == Byte.class) {
			ps.setByte(iii, (Byte) value);
		} else if (value.getClass() == Float.class) {
			ps.setFloat(iii, (Float) value);
		} else if (value.getClass() == Double.class) {
			ps.setDouble(iii, (Double) value);
		} else if (value.getClass() == Boolean.class) {
			ps.setBoolean(iii, (Boolean) value);
		} else if (value.getClass() == Boolean.class) {
			ps.setBoolean(iii, (Boolean) value);
		} else if (value.getClass() == Timestamp.class) {
			ps.setTimestamp(iii, (Timestamp) value);
		} else if (value.getClass() == Date.class) {
			ps.setDate(iii, (Date) value);
		} else {
			throw new Exception("Not manage type ==> need to add it ...");
		}
	}
	
	public static void whereAppendQuery(final StringBuilder querry, final String tableName, final QuerryItem condition, final QuerryOptions options, String deletedFieldName)
			throws ExceptionDBInterface {
		boolean exclude_deleted = true;
		if (options != null) {
			Object data = options.get(QuerryOptions.SQL_DELETED_DISABLE);
			if (data instanceof Boolean elem) {
				exclude_deleted = !elem;
			} else {
				if (data != null) {
					LOGGER.error("'{}' ==> has not a boolean value: {}", QuerryOptions.SQL_DELETED_DISABLE, data);
				}
			}
		}
		// Check if we have a condition to generate
		if (condition == null) {
			if (exclude_deleted && deletedFieldName != null) {
				querry.append(" WHERE ");
				querry.append(tableName);
				querry.append(".");
				querry.append(deletedFieldName);
				querry.append(" = false ");
			}
			return;
		}
		querry.append(" WHERE (");
		condition.generateQuerry(querry, tableName);
		
		querry.append(") ");
		if (exclude_deleted && deletedFieldName != null) {
			querry.append("AND ");
			querry.append(tableName);
			querry.append(".");
			querry.append(deletedFieldName);
			querry.append(" = false ");
		}
	}
	
	public static void whereInjectValue(final PreparedStatement ps, final QuerryItem condition) throws Exception {
		// Check if we have a condition to generate
		if (condition == null) {
			return;
		}
		int iii = 1;
		iii = condition.injectQuerry(ps, iii);
	}
	
	public static int executeSimpleQuerry(final String querry, final boolean root) throws SQLException, IOException {
		final DBEntry entry = DBEntry.createInterface(GlobalConfiguration.dbConfig, root);
		final Statement stmt = entry.connection.createStatement();
		return stmt.executeUpdate(querry);
	}
	
	public static int executeSimpleQuerry(final String querry) throws SQLException, IOException {
		return executeSimpleQuerry(querry, false);
	}
	
	public static boolean executeQuerry(final String querry, final boolean root) throws SQLException, IOException {
		final DBEntry entry = DBEntry.createInterface(GlobalConfiguration.dbConfig, root);
		final Statement stmt = entry.connection.createStatement();
		return stmt.execute(querry);
	}
	
	public static boolean executeQuerry(final String querry) throws SQLException, IOException {
		return executeQuerry(querry, false);
	}
	
	public static <T> T getWhere(final Class<T> clazz, final QuerryItem condition) throws Exception {
		return getWhere(clazz, condition, null);
	}
	
	public static <T> T getWhere(final Class<T> clazz, final QuerryItem condition, final QuerryOptions options) throws Exception {
		final List<T> values = getsWhere(clazz, condition, options, 1);
		if (values.size() == 0) {
			return null;
		}
		return values.get(0);
	}
	
	public static <T> List<T> getsWhere(final Class<T> clazz, final QuerryItem condition) throws Exception {
		return getsWhere(clazz, condition, null, null, null);
	}
	
	public static <T> List<T> getsWhere(final Class<T> clazz, final QuerryItem condition, final QuerryOptions options) throws Exception {
		return getsWhere(clazz, condition, null, options, null);
	}
	
	public static <T> List<T> getsWhere(final Class<T> clazz, final QuerryItem condition, final QuerryOptions options, final Integer linit) throws Exception {
		return getsWhere(clazz, condition, null, options, linit);
	}
	
	// TODO: set limit as an querry Option...
	@SuppressWarnings("unchecked")
	public static <T> List<T> getsWhere(final Class<T> clazz, final QuerryItem condition, final String orderBy, final QuerryOptions options, final Integer linit) throws Exception {
		
		boolean readAllfields = false;
		if (options != null) {
			Object data = options.get(QuerryOptions.SQL_NOT_READ_DISABLE);
			if (data instanceof Boolean elem) {
				readAllfields = elem;
			} else {
				if (data != null) {
					LOGGER.error("'{}' ==> has not a boolean value: {}", QuerryOptions.SQL_NOT_READ_DISABLE, data);
				}
			}
		}
		
		DBEntry entry = DBEntry.createInterface(GlobalConfiguration.dbConfig);
		final List<T> outs = new ArrayList<>();
		// real add in the BDD:
		try {
			final String tableName = AnnotationTools.getTableName(clazz);
			//boolean createIfNotExist = clazz.getDeclaredAnnotationsByType(SQLIfNotExists.class).length != 0;
			final StringBuilder querry = new StringBuilder();
			querry.append("SELECT ");
			//querry.append(tableName);
			//querry.append(" SET ");
			
			boolean firstField = true;
			int count = 0;
			final String deletedFieldName = AnnotationTools.getDeletedFieldName(clazz);
			for (final Field elem : clazz.getFields()) {
				// static field is only for internal global declaration ==> remove it ..
				if (java.lang.reflect.Modifier.isStatic(elem.getModifiers())) {
					continue;
				}
				final SqlWrapperAddOn addOn = findAddOnforField(elem);
				if (addOn != null && addOn.isExternal()) {
					continue;
				}
				// TODO: Manage it with AddOn
				final boolean notRead = AnnotationTools.isdefaultNotRead(elem);
				if (!readAllfields && notRead) {
					continue;
				}
				final String name = AnnotationTools.getFieldName(elem);
				count++;
				if (firstField) {
					firstField = false;
				} else {
					querry.append(",");
				}
				querry.append(" ");
				if (addOn != null) {
					addOn.generateQuerry(tableName, elem, querry, name, count, options);
				} else {
					querry.append(tableName);
					querry.append(".");
					querry.append(name);
				}
			}
			querry.append(" FROM `");
			querry.append(tableName);
			querry.append("` ");
			whereAppendQuery(querry, tableName, condition, options, deletedFieldName);
			if (orderBy != null && orderBy.length() >= 1) {
				querry.append(" ORDER BY ");
				//querry.append(tableName);
				//querry.append(".");
				querry.append(orderBy);
			}
			if (linit != null && linit >= 1) {
				querry.append(" LIMIT " + linit);
			}
			firstField = true;
			LOGGER.debug("generate the querry: '{}'", querry.toString());
			// prepare the request:
			final PreparedStatement ps = entry.connection.prepareStatement(querry.toString(), Statement.RETURN_GENERATED_KEYS);
			whereInjectValue(ps, condition);
			// execute the request
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				// TODO: manage class that is defined inside a class ==> Not manage for now...
				final Object data = clazz.getConstructors()[0].newInstance();
				count = 1;
				for (final Field elem : clazz.getFields()) {
					// static field is only for internal global declaration ==> remove it ..
					if (java.lang.reflect.Modifier.isStatic(elem.getModifiers())) {
						continue;
					}
					final SqlWrapperAddOn addOn = findAddOnforField(elem);
					if (addOn != null && addOn.isExternal()) {
						continue;
					}
					// TODO: Manage it with AddOn
					final boolean notRead = AnnotationTools.isdefaultNotRead(elem);
					if (!readAllfields && notRead) {
						continue;
					}
					if (addOn != null) {
						int nbRowRead = addOn.fillFromQuerry(rs, elem, data, count, options);
						count += nbRowRead;
					} else {
						setValueFromDb(elem.getType(), data, count, elem, rs);
						count++;
					}
				}
				final T out = (T) data;
				outs.add(out);
			}
			
		} catch (final SQLException ex) {
			ex.printStackTrace();
			throw ex;
		} catch (final Exception ex) {
			ex.printStackTrace();
		} finally {
			entry.close();
			entry = null;
		}
		return outs;
	}
	
	// TODO : detect the @Id
	public static <T> T get(final Class<T> clazz, final long id) throws Exception {
		return get(clazz, id, null);
	}
	
	public static <T> T get(final Class<T> clazz, final long id, final QuerryOptions options) throws Exception {
		Field primaryKeyField = null;
		for (final Field elem : clazz.getFields()) {
			// static field is only for internal global declaration ==> remove it ..
			if (java.lang.reflect.Modifier.isStatic(elem.getModifiers())) {
				continue;
			}
			if (AnnotationTools.isPrimaryKey(elem)) {
				primaryKeyField = elem;
			}
		}
		if (primaryKeyField != null) {
			return SqlWrapper.getWhere(clazz, new QuerryCondition(AnnotationTools.getFieldName(primaryKeyField), "=", id), options);
		}
		throw new Exception("Missing primary Key...");
	}
	
	public static String getCurrentTimeStamp() {
		return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
	}
	
	public static <T> List<T> gets(final Class<T> clazz) throws Exception {
		return getsWhere(clazz, null);
	}
	
	public static <T> List<T> gets(final Class<T> clazz, final QuerryOptions options) throws Exception {
		return getsWhere(clazz, null, options);
	}
	
	// TODO : detect the @Id
	public static int delete(final Class<?> clazz, final long id) throws Exception {
		String hasDeletedFieldName = AnnotationTools.getDeletedFieldName(clazz);
		if (hasDeletedFieldName != null) {
			return deleteSoft(clazz, id);
		} else {
			return deleteHard(clazz, id);
		}
	}
	
	public static int delete(final Class<?> clazz, final QuerryItem condition) throws Exception {
		String hasDeletedFieldName = AnnotationTools.getDeletedFieldName(clazz);
		if (hasDeletedFieldName != null) {
			return deleteSoftWhere(clazz, condition);
		} else {
			return deleteHardWhere(clazz, condition);
		}
	}
	
	public static int deleteHard(final Class<?> clazz, final long id) throws Exception {
		return deleteHardWhere(clazz, new QuerryCondition("id", "=", id));
	}
	
	public static int deleteHardWhere(final Class<?> clazz, final QuerryItem condition) throws Exception {
		final String tableName = AnnotationTools.getTableName(clazz);
		final String deletedFieldName = AnnotationTools.getDeletedFieldName(clazz);
		// find the deleted field
		
		DBEntry entry = DBEntry.createInterface(GlobalConfiguration.dbConfig);
		final StringBuilder querry = new StringBuilder();
		querry.append("DELETE FROM `");
		querry.append(tableName);
		querry.append("` ");
		whereAppendQuery(querry, tableName, condition, null, deletedFieldName);
		try {
			LOGGER.debug("APPLY: {}", querry.toString());
			final PreparedStatement ps = entry.connection.prepareStatement(querry.toString());
			whereInjectValue(ps, condition);
			return ps.executeUpdate();
		} finally {
			entry.close();
			entry = null;
		}
	}
	
	private static int deleteSoft(final Class<?> clazz, final long id) throws Exception {
		return deleteSoftWhere(clazz, new QuerryCondition("id", "=", id));
	}
	
	public static String getDBNow() {
		if (!"sqlite".equals(ConfigBaseVariable.getDBType())) {
			return "now(3)";
		}
		return "DATE()";
	}
	
	public static int deleteSoftWhere(final Class<?> clazz, final QuerryItem condition) throws Exception {
		final String tableName = AnnotationTools.getTableName(clazz);
		final String deletedFieldName = AnnotationTools.getDeletedFieldName(clazz);
		/*
		String updateFieldName = null;
		if ("sqlite".equalsIgnoreCase(ConfigBaseVariable.getDBType())) {
			updateFieldName = AnnotationTools.getUpdatedFieldName(clazz);
		}
		*/
		// find the deleted field
		
		DBEntry entry = DBEntry.createInterface(GlobalConfiguration.dbConfig);
		final StringBuilder querry = new StringBuilder();
		querry.append("UPDATE `");
		querry.append(tableName);
		querry.append("` SET `");
		querry.append(deletedFieldName);
		querry.append("`=true ");
		/*
		 * The trigger work well, but the timestamp is store @ seconds...
		if (updateFieldName != null) {
			// done only in SQLite (the trigger does not work...
			querry.append(", `");
			querry.append(updateFieldName);
			querry.append("`=DATE()");
		}
		*/
		whereAppendQuery(querry, tableName, condition, null, deletedFieldName);
		try {
			LOGGER.debug("APPLY UPDATE: {}", querry.toString());
			final PreparedStatement ps = entry.connection.prepareStatement(querry.toString());
			whereInjectValue(ps, condition);
			return ps.executeUpdate();
		} finally {
			entry.close();
			entry = null;
		}
	}
	
	public static int unsetDelete(final Class<?> clazz, final long id) throws Exception {
		return unsetDeleteWhere(clazz, new QuerryCondition("id", "=", id));
	}
	
	public static int unsetDeleteWhere(final Class<?> clazz, final QuerryItem condition) throws Exception {
		final String tableName = AnnotationTools.getTableName(clazz);
		final String deletedFieldName = AnnotationTools.getDeletedFieldName(clazz);
		if (deletedFieldName == null) {
			throw new Exception("The class " + clazz.getCanonicalName() + " has no deleted field");
		}
		DBEntry entry = DBEntry.createInterface(GlobalConfiguration.dbConfig);
		final StringBuilder querry = new StringBuilder();
		querry.append("UPDATE `");
		querry.append(tableName);
		querry.append("` SET `");
		querry.append(deletedFieldName);
		querry.append("`=false ");
		/*
		 * is is needed only for SQLite ???
		querry.append("`modify_date`=");
		querry.append(getDBNow());
		querry.append(", ");
		*/
		// need to disable the deleted false because the model must be unselected to be updated.
		QuerryOptions options = new QuerryOptions(QuerryOptions.SQL_DELETED_DISABLE, true);
		whereAppendQuery(querry, tableName, condition, options, deletedFieldName);
		try {
			final PreparedStatement ps = entry.connection.prepareStatement(querry.toString());
			whereInjectValue(ps, condition);
			return ps.executeUpdate();
		} finally {
			entry.close();
			entry = null;
		}
	}
	
	public static List<String> createTable(final Class<?> clazz) throws Exception {
		return createTable(clazz, true);
	}
	
	private static boolean isFieldFromSuperClass(Class<?> model, String filedName) {
		Class<?> superClass = model.getSuperclass();
		if (superClass == null) {
			return false;
		}
		for (Field field : superClass.getFields()) {
			String name;
			try {
				name = AnnotationTools.getFieldName(field);
				if (filedName.equals(name)) {
					return true;
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				LOGGER.trace("Catch error field name in parent create data table: {}", e.getMessage());
			}
		}
		return false;
	}
	
	public static void createTablesSpecificType(final String tableName, final Field elem, final StringBuilder mainTableBuilder, final List<String> preOtherTables, final List<String> postOtherTables,
			final boolean createIfNotExist, final boolean createDrop, final int fieldId, final Class<?> classModel) throws Exception {
		final String name = AnnotationTools.getFieldName(elem);
		final Integer limitSize = AnnotationTools.getLimitSize(elem);
		final boolean notNull = AnnotationTools.getNotNull(elem);
		
		final boolean primaryKey = AnnotationTools.isPrimaryKey(elem);
		final GenerationType strategy = AnnotationTools.getStrategy(elem);
		
		final boolean createTime = elem.getDeclaredAnnotationsByType(CreationTimestamp.class).length != 0;
		final boolean updateTime = elem.getDeclaredAnnotationsByType(UpdateTimestamp.class).length != 0;
		final String comment = AnnotationTools.getComment(elem);
		final String defaultValue = AnnotationTools.getDefault(elem);
		
		if (fieldId == 0) {
			mainTableBuilder.append("\n\t\t`");
		} else {
			mainTableBuilder.append(",\n\t\t`");
		}
		mainTableBuilder.append(name);
		mainTableBuilder.append("` ");
		String typeValue = null;
		typeValue = convertTypeInSQL(classModel);
		if ("text".equals(typeValue) && !"sqlite".equals(ConfigBaseVariable.getDBType())) {
			if (limitSize != null) {
				mainTableBuilder.append("varchar(");
				mainTableBuilder.append(limitSize);
				mainTableBuilder.append(")");
			} else {
				mainTableBuilder.append("text");
				if (!"sqlite".equals(ConfigBaseVariable.getDBType())) {
					mainTableBuilder.append(" CHARACTER SET utf8");
				}
			}
		} else {
			mainTableBuilder.append(typeValue);
		}
		mainTableBuilder.append(" ");
		if (notNull) {
			if (!primaryKey || !"sqlite".equalsIgnoreCase(ConfigBaseVariable.getDBType())) {
				mainTableBuilder.append("NOT NULL ");
			}
			if (defaultValue == null) {
				if (updateTime || createTime) {
					mainTableBuilder.append("DEFAULT CURRENT_TIMESTAMP");
					if (!"sqlite".equals(ConfigBaseVariable.getDBType())) {
						mainTableBuilder.append("(3)");
					}
					mainTableBuilder.append(" ");
				}
				if (updateTime) {
					if (!"sqlite".equals(ConfigBaseVariable.getDBType())) {
						mainTableBuilder.append("ON UPDATE CURRENT_TIMESTAMP");
						mainTableBuilder.append("(3)");
					} else {
						// TODO: add trigger: 
						/*
						CREATE TRIGGER your_table_trig AFTER UPDATE ON your_table
						 BEGIN
						  update your_table SET updated_on = datetime('now') WHERE user_id = NEW.user_id;
						 END;
						*/
						StringBuilder triggerBuilder = new StringBuilder();
						triggerBuilder.append("CREATE TRIGGER ");
						triggerBuilder.append(tableName);
						triggerBuilder.append("_update_trigger AFTER UPDATE ON ");
						triggerBuilder.append(tableName);
						triggerBuilder.append(" \nBEGIN \n    update ");
						triggerBuilder.append(tableName);
						triggerBuilder.append(" SET ");
						triggerBuilder.append(name);
						triggerBuilder.append(" = datetime('now') WHERE  id = NEW.id; \n");
						triggerBuilder.append("END;");
						
						postOtherTables.add(triggerBuilder.toString());
					}
					
					mainTableBuilder.append(" ");
				}
			} else {
				mainTableBuilder.append("DEFAULT ");
				if ("CURRENT_TIMESTAMP(3)".equals(defaultValue) && "sqlite".equals(ConfigBaseVariable.getDBType())) {
					mainTableBuilder.append("CURRENT_TIMESTAMP");
				} else {
					mainTableBuilder.append(defaultValue);
				}
				mainTableBuilder.append(" ");
				if (updateTime) {
					if (!"sqlite".equals(ConfigBaseVariable.getDBType())) {
						mainTableBuilder.append("ON UPDATE CURRENT_TIMESTAMP");
						mainTableBuilder.append("(3)");
					}
					mainTableBuilder.append(" ");
				}
			}
		} else if (defaultValue == null) {
			if (updateTime || createTime) {
				if ("sqlite".equals(ConfigBaseVariable.getDBType())) {
					mainTableBuilder.append("DEFAULT CURRENT_TIMESTAMP ");
				} else {
					mainTableBuilder.append("DEFAULT CURRENT_TIMESTAMP(3) ");
				}
			} else if (primaryKey) {
				mainTableBuilder.append("NOT NULL ");
			} else {
				mainTableBuilder.append("DEFAULT NULL ");
			}
		} else {
			mainTableBuilder.append("DEFAULT ");
			mainTableBuilder.append(defaultValue);
			mainTableBuilder.append(" ");
			
		}
		if (primaryKey && "sqlite".equals(ConfigBaseVariable.getDBType())) {
			mainTableBuilder.append("PRIMARY KEY ");
			
		}
		if (strategy == GenerationType.IDENTITY) {
			if (!"sqlite".equals(ConfigBaseVariable.getDBType())) {
				mainTableBuilder.append("AUTO_INCREMENT ");
			} else {
				mainTableBuilder.append("AUTOINCREMENT ");
			}
		} else if (strategy != null) {
			throw new Exception("Can not generate a stategy different of IDENTITY");
		}
		
		if (comment != null && !"sqlite".equals(ConfigBaseVariable.getDBType())) {
			mainTableBuilder.append("COMMENT '");
			mainTableBuilder.append(comment.replace('\'', '\''));
			mainTableBuilder.append("' ");
		}
	}
	
	public static List<String> createTable(final Class<?> clazz, final boolean createDrop) throws Exception {
		final String tableName = AnnotationTools.getTableName(clazz);
		final boolean createIfNotExist = clazz.getDeclaredAnnotationsByType(SQLIfNotExists.class).length != 0;
		final List<String> preActionList = new ArrayList<>();
		final List<String> postActionList = new ArrayList<>();
		final StringBuilder out = new StringBuilder();
		// Drop Table
		if (createIfNotExist && createDrop) {
			final StringBuilder tableTmp = new StringBuilder();
			tableTmp.append("DROP TABLE IF EXISTS `");
			tableTmp.append(tableName);
			tableTmp.append("`;");
			postActionList.add(tableTmp.toString());
		}
		// create Table:
		out.append("CREATE TABLE `");
		out.append(tableName);
		out.append("` (");
		int fieldId = 0;
		LOGGER.debug("===> TABLE `{}`", tableName);
		final List<String> primaryKeys = new ArrayList<>();
		
		for (final Field elem : clazz.getFields()) {
			// DEtect the primary key (support only one primary key right now...
			if (AnnotationTools.isPrimaryKey(elem)) {
				primaryKeys.add(AnnotationTools.getFieldName(elem));
			}
		}
		// Here we insert the data in the reverse mode ==> the parent class add there parameter at the start (we reorder the field with the parenting). 
		StringBuilder tmpOut = new StringBuilder();
		StringBuilder reverseOut = new StringBuilder();
		List<String> alreadyAdded = new ArrayList<>();
		Class<?> currentClazz = clazz;
		while (currentClazz != null) {
			fieldId = 0;
			LOGGER.info("parse class: '{}'", currentClazz.getCanonicalName());
			for (final Field elem : clazz.getFields()) {
				// static field is only for internal global declaration ==> remove it ..
				if (java.lang.reflect.Modifier.isStatic(elem.getModifiers())) {
					continue;
				}
				final String dataName = AnnotationTools.getFieldName(elem);
				if (isFieldFromSuperClass(currentClazz, dataName)) {
					LOGGER.trace("        SKIP:  '{}'", elem.getName());
					continue;
				}
				if (alreadyAdded.contains(dataName)) {
					LOGGER.trace("        SKIP2: '{}'", elem.getName());
					continue;
				}
				alreadyAdded.add(dataName);
				LOGGER.info("        + '{}'", elem.getName());
				if (isAddOnField(elem)) {
					final SqlWrapperAddOn addOn = findAddOnforField(elem);
					LOGGER.info("Create type for: {} ==> {} (ADD-ON)", AnnotationTools.getFieldName(elem), elem.getType());
					if (addOn != null) {
						addOn.createTables(tableName, elem, tmpOut, preActionList, postActionList, createIfNotExist, createDrop, fieldId);
					} else {
						throw new Exception(
								"Element matked as add-on but add-on does not loaded: table:" + tableName + " field name=" + AnnotationTools.getFieldName(elem) + " type=" + elem.getType());
					}
				} else {
					LOGGER.info("Create type for: {} ==> {}", AnnotationTools.getFieldName(elem), elem.getType());
					SqlWrapper.createTablesSpecificType(tableName, elem, tmpOut, preActionList, postActionList, createIfNotExist, createDrop, fieldId, elem.getType());
				}
				fieldId++;
			}
			boolean dataInThisObject = tmpOut.toString().length() > 0;
			if (dataInThisObject) {
				boolean dataInPreviousObject = reverseOut.toString().length() > 0;
				if (dataInPreviousObject) {
					tmpOut.append(", ");
					tmpOut.append(reverseOut.toString());
				}
				reverseOut = tmpOut;
				tmpOut = new StringBuilder();
			}
			currentClazz = currentClazz.getSuperclass();
			if (currentClazz == Object.class) {
				break;
			}
		}
		out.append(reverseOut.toString());
		if (primaryKeys.size() != 0 && !"sqlite".equals(ConfigBaseVariable.getDBType())) {
			out.append(",\n\tPRIMARY KEY (`");
			for (int iii = 0; iii < primaryKeys.size(); iii++) {
				if (iii != 0) {
					out.append(",");
				}
				out.append(primaryKeys.get(iii));
			}
			out.append("`)");
		}
		out.append("\n\t)");
		if (!"sqlite".equals(ConfigBaseVariable.getDBType())) {
			out.append(" ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci");
		}
		out.append(";");
		preActionList.add(out.toString());
		preActionList.addAll(postActionList);
		return preActionList;
	}
	
}