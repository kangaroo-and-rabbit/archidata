package org.kar.archidata.dataAccess;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;

import org.aopalliance.reflect.Class;
import org.glassfish.jaxb.runtime.v2.schemagen.xmlschema.List;
import org.kar.archidata.GlobalConfiguration;
import org.kar.archidata.annotation.AnnotationTools;
import org.kar.archidata.annotation.CreationTimestamp;
import org.kar.archidata.annotation.DataAddOn;
import org.kar.archidata.annotation.SQLDefault;
import org.kar.archidata.annotation.UpdateTimestamp;
import org.kar.archidata.dataAccess.addOn.AddOnManyToMany;
import org.kar.archidata.dataAccess.addOn.AddOnManyToOne;
import org.kar.archidata.dataAccess.addOn.AddOnSQLTableExternalForeinKeyAsList;
import org.kar.archidata.db.DBEntry;
import org.kar.archidata.util.ConfigBaseVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Timestamp;
import com.mysql.cj.x.protobuf.MysqlxDatatypes.Scalar.String;
import com.mysql.cj.xdevapi.Statement;

import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.ws.rs.InternalServerErrorException;
import javassist.bytecode.Descriptor.Iterator;

public class DataAccess {
	static final Logger LOGGER = LoggerFactory.getLogger(DataAccess.class);
	static final List<DataAccessAddOn> addOn = new ArrayList<>();
	
	static {
		addOn.add(new AddOnManyToMany());
		addOn.add(new AddOnManyToOne());
		addOn.add(new AddOnSQLTableExternalForeinKeyAsList());
	}
	
	public static void addAddOn(final DataAccessAddOn addOn) {
		DataAccess.addOn.add(addOn);
	}
	
	public static class ExceptionDBInterface extends Exception {
		private static final long serialVersionUID = 1L;
		public int errorID;
		
		public ExceptionDBInterface(final int errorId, final String message) {
			super(message);
			this.errorID = errorId;
		}
	}
	
	public DataAccess() {
		
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
			return 1 == DataAccess.executeSimpleQuerry("CREATE DATABASE `" + name + "`;", true);
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
	public static List<Long> getListOfIds(final ResultSet rs, final int iii, final String separator) throws SQLException {
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
	
	protected static <T> void setValuedb(final Class<?> type, final T data, int index, final Field field, final PreparedStatement ps) throws Exception {
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
				final Timestamp sqlDate = java.sql.Timestamp.from(((Date) tmp).toInstant());
				ps.setTimestamp(index++, sqlDate);
			}
		} else if (type == LocalDate.class) {
			final Object tmp = field.get(data);
			if (tmp == null) {
				ps.setNull(index++, Types.INTEGER);
			} else {
				final java.sql.Date sqlDate = java.sql.Date.valueOf((LocalDate) tmp);
				ps.setDate(index++, sqlDate);
			}
		} else if (type == LocalTime.class) {
			final Object tmp = field.get(data);
			if (tmp == null) {
				ps.setNull(index++, Types.INTEGER);
			} else {
				final java.sql.Time sqlDate = java.sql.Time.valueOf((LocalTime) tmp);
				ps.setTime(index++, sqlDate);
			}
		} else if (type == String.class) {
			final Object tmp = field.get(data);
			if (tmp == null) {
				ps.setNull(index++, Types.VARCHAR);
			} else {
				ps.setString(index++, (String) tmp);
			}
		} else if (type.isEnum()) {
			final Object tmp = field.get(data);
			if (tmp == null) {
				ps.setNull(index++, Types.VARCHAR);
			} else {
				ps.setString(index++, tmp.toString());
			}
		} else {
			throw new Exception("Unknown Field Type");
		}
	}

	// TODO: maybe wrap this if the use of sqlite ==> maybe some problems came with sqlite ...
	protected static <T> void setValueFromDb(final Class<?> type, final T data, final int index, final Field field, final ResultSet rs) throws Exception {
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
			final Timestamp tmp = rs.getTimestamp(index);
			if (rs.wasNull()) {
				field.set(data, null);
			} else {
				field.set(data, tmp);
			}
		} else if (type == Date.class) {
			final Timestamp tmp = rs.getTimestamp(index);
			if (rs.wasNull()) {
				field.set(data, null);
			} else {
				field.set(data, Date.from(tmp.toInstant()));
			}
		} else if (type == LocalDate.class) {
			final java.sql.Date tmp = rs.getDate(index);
			if (rs.wasNull()) {
				field.set(data, null);
			} else {
				field.set(data, tmp.toLocalDate());
			}
		} else if (type == LocalTime.class) {
			final java.sql.Time tmp = rs.getTime(index);
			if (rs.wasNull()) {
				field.set(data, null);
			} else {
				field.set(data, tmp.toLocalTime());
			}
		} else if (type == String.class) {
			final String tmp = rs.getString(index);
			if (rs.wasNull()) {
				field.set(data, null);
			} else {
				field.set(data, tmp);
			}
		} else if (type.isEnum()) {
			final String tmp = rs.getString(index);
			if (rs.wasNull()) {
				field.set(data, null);
			} else {
				final Object[] arr = type.getEnumConstants();
				for (final Object elem : arr) {
					if (elem.toString().equals(tmp)) {
						field.set(data, elem);
						break;
					}
				}
				// TODO: maybe do something stupid if not exist ???
			}
		} else {
			throw new Exception("Unknown Field Type");
		}
	}
	
	public static boolean isAddOnField(final Field field) {
		final boolean ret = AnnotationTools.isAnnotationGroup(field, DataAddOn.class);
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
	
	public static DataAccessAddOn findAddOnforField(final Field field) {
		for (final DataAccessAddOn elem : addOn) {
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
				final DataAccessAddOn addOn = findAddOnforField(elem);
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
				final DataAccessAddOn addOn = findAddOnforField(elem);
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
	
	public static <T, ID_TYPE> int updateWithJson(final Class<T> clazz, final ID_TYPE id, final String jsonData) throws Exception {
		// Find the ID field type ....
		final Field idField = AnnotationTools.getIdField(clazz);
		if (idField == null) {
			throw new Exception("The class have no annotation @Id ==> can not determine the default type searching");
		}
		// check the compatibility of the id and the declared ID
		if (id instanceof idField.getType()) {
			throw new Exception("Request update with the wriong type ...");
		}

		// Udpade Json Value
		return updateWithJson(clazz, QueryCondition(AnnotationTools.getFieldName(idField), "=", id), jsonData);
	}
	
	public static <T> int updateWithJson(final Class<T> clazz, final QueryItem condition, final String jsonData) throws Exception {
		final ObjectMapper mapper = new ObjectMapper();
		// parse the object to be sure the data are valid:
		final T data = mapper.readValue(jsonData, clazz);
		// Read the tree to filter injection of data:
		final JsonNode root = mapper.readTree(jsonData);
		final List<String> keys = new ArrayList<>();
		final Iterator<String> iterator = root.fieldNames();
		iterator.forEachRemaining(e -> keys.add(e));
		return update(data, id, keys);
		return 0;
	}

	public static <T> int update(final T data, final QueryItem condition) throws Exception {
		
		return 0;
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
				final DataAccessAddOn addOn = findAddOnforField(elem);
				if (addOn != null && addOn.isExternal()) {
					continue;
				}
				final boolean createTime = AnnotationTools.isCreatedAtField(elem);
				if (createTime) {
					continue;
				}
				final String name = AnnotationTools.getFieldName(elem);
				final boolean updateTime = AnnotationTools.isUpdateAtField(elem);
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
				final DataAccessAddOn addOn = findAddOnforField(elem);
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
		if (value instanceof final Long tmp) {
			ps.setLong(iii, tmp);
		} else if (value instanceof final Integer tmp) {
			ps.setInt(iii, tmp);
		} else if (value instanceof final String tmp) {
			ps.setString(iii, tmp);
		} else if (value instanceof final Short tmp) {
			ps.setShort(iii, tmp);
		} else if (value instanceof final Byte tmp) {
			ps.setByte(iii, tmp);
		} else if (value instanceof final Float tmp) {
			ps.setFloat(iii, tmp);
		} else if (value instanceof final Double tmp) {
			ps.setDouble(iii, tmp);
		} else if (value instanceof final Boolean tmp) {
			ps.setBoolean(iii, tmp);
		} else if (value instanceof final Boolean tmp) {
			ps.setBoolean(iii, tmp);
		} else if (value instanceof final Timestamp tmp) {
			ps.setTimestamp(iii, tmp);
		} else if (value instanceof final Date tmp) {
			ps.setTimestamp(iii, java.sql.Timestamp.from((tmp).toInstant()));
		} else if (value instanceof final LocalDate tmp) {
			ps.setDate(iii, java.sql.Date.valueOf(tmp));
		} else if (value instanceof final LocalTime tmp) {
			ps.setTime(iii, java.sql.Time.valueOf(tmp));
		} else if (value.getClass().isEnum()) {
			ps.setString(iii, value.toString());
		} else {
			throw new Exception("Not manage type ==> need to add it ...");
		}
	}
	
	public static void whereAppendQuery(final StringBuilder querry, final String tableName, final QueryItem condition, final QueryOptions options, final String deletedFieldName)
			throws ExceptionDBInterface {
		boolean exclude_deleted = true;
		if (options != null) {
			final Object data = options.get(QueryOptions.SQL_DELETED_DISABLE);
			if (data instanceof final Boolean elem) {
				exclude_deleted = !elem;
			} else if (data != null) {
				LOGGER.error("'{}' ==> has not a boolean value: {}", QueryOptions.SQL_DELETED_DISABLE, data);
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
	
	public static void whereInjectValue(final PreparedStatement ps, final QueryItem condition) throws Exception {
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
	
	public static <T> T getWhere(final Class<T> clazz, final QueryItem condition) throws Exception {
		return getWhere(clazz, condition, null);
	}
	
	public static <T> T getWhere(final Class<T> clazz, final QueryItem condition, final QueryOptions options) throws Exception {
		final List<T> values = getsWhere(clazz, condition, options, 1);
		if (values.size() == 0) {
			return null;
		}
		return values.get(0);
	}
	
	public static <T> List<T> getsWhere(final Class<T> clazz, final QueryItem condition) throws Exception {
		return getsWhere(clazz, condition, null, null, null);
	}
	
	public static <T> List<T> getsWhere(final Class<T> clazz, final QueryItem condition, final QueryOptions options) throws Exception {
		return getsWhere(clazz, condition, null, options, null);
	}
	
	public static <T> List<T> getsWhere(final Class<T> clazz, final QueryItem condition, final QueryOptions options, final Integer linit) throws Exception {
		return getsWhere(clazz, condition, null, options, linit);
	}
	
	// TODO: set limit as an querry Option...
	@SuppressWarnings("unchecked")
	public static <T> List<T> getsWhere(final Class<T> clazz, final QueryItem condition, final String orderBy, final QueryOptions options, final Integer linit) throws Exception {
		
		boolean readAllfields = false;
		if (options != null) {
			final Object data = options.get(QueryOptions.SQL_NOT_READ_DISABLE);
			if (data instanceof final Boolean elem) {
				readAllfields = elem;
			} else if (data != null) {
				LOGGER.error("'{}' ==> has not a boolean value: {}", QueryOptions.SQL_NOT_READ_DISABLE, data);
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
				final DataAccessAddOn addOn = findAddOnforField(elem);
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
					final DataAccessAddOn addOn = findAddOnforField(elem);
					if (addOn != null && addOn.isExternal()) {
						continue;
					}
					// TODO: Manage it with AddOn
					final boolean notRead = AnnotationTools.isdefaultNotRead(elem);
					if (!readAllfields && notRead) {
						continue;
					}
					if (addOn != null) {
						final int nbRowRead = addOn.fillFromQuerry(rs, elem, data, count, options);
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
	
	public static <T> T get(final Class<T> clazz, final long id, final QueryOptions options) throws Exception {
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
			return DataAccess.getWhere(clazz, new QueryCondition(AnnotationTools.getFieldName(primaryKeyField), "=", id), options);
		}
		throw new Exception("Missing primary Key...");
	}
	
	public static String getCurrentTimeStamp() {
		return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
	}
	
	public static <T> List<T> gets(final Class<T> clazz) throws Exception {
		return getsWhere(clazz, null);
	}
	
	public static <T> List<T> gets(final Class<T> clazz, final QueryOptions options) throws Exception {
		return getsWhere(clazz, null, options);
	}
	
	// TODO : detect the @Id
	public static int delete(final Class<?> clazz, final long id) throws Exception {
		final String hasDeletedFieldName = AnnotationTools.getDeletedFieldName(clazz);
		if (hasDeletedFieldName != null) {
			return deleteSoft(clazz, id);
		} else {
			return deleteHard(clazz, id);
		}
	}
	
	public static int deleteWhere(final Class<?> clazz, final QueryItem condition) throws Exception {
		final String hasDeletedFieldName = AnnotationTools.getDeletedFieldName(clazz);
		if (hasDeletedFieldName != null) {
			return deleteSoftWhere(clazz, condition);
		} else {
			return deleteHardWhere(clazz, condition);
		}
	}
	
	public static int deleteHard(final Class<?> clazz, final long id) throws Exception {
		return deleteHardWhere(clazz, new QueryCondition("id", "=", id));
	}
	
	public static int deleteHardWhere(final Class<?> clazz, final QueryItem condition) throws Exception {
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
		return deleteSoftWhere(clazz, new QueryCondition("id", "=", id));
	}
	
	public static String getDBNow() {
		if (!"sqlite".equals(ConfigBaseVariable.getDBType())) {
			return "now(3)";
		}
		return "DATE()";
	}
	
	public static int deleteSoftWhere(final Class<?> clazz, final QueryItem condition) throws Exception {
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
		return unsetDeleteWhere(clazz, new QueryCondition("id", "=", id));
	}
	
	public static int unsetDeleteWhere(final Class<?> clazz, final QueryItem condition) throws Exception {
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
		final QueryOptions options = new QueryOptions(QueryOptions.SQL_DELETED_DISABLE, true);
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

}