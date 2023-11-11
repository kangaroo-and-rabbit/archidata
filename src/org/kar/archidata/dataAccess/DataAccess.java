package org.kar.archidata.dataAccess;

import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.kar.archidata.GlobalConfiguration;
import org.kar.archidata.annotation.AnnotationTools;
import org.kar.archidata.annotation.CreationTimestamp;
import org.kar.archidata.annotation.DataDefault;
import org.kar.archidata.annotation.UpdateTimestamp;
import org.kar.archidata.dataAccess.addOn.AddOnDataJson;
import org.kar.archidata.dataAccess.addOn.AddOnManyToMany;
import org.kar.archidata.dataAccess.addOn.AddOnManyToOne;
import org.kar.archidata.dataAccess.addOn.AddOnSQLTableExternalForeinKeyAsList;
import org.kar.archidata.db.DBEntry;
import org.kar.archidata.util.ConfigBaseVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.ws.rs.InternalServerErrorException;

public class DataAccess {
	static final Logger LOGGER = LoggerFactory.getLogger(DataAccess.class);
	static final List<DataAccessAddOn> addOn = new ArrayList<>();

	static {
		addOn.add(new AddOnManyToMany());
		addOn.add(new AddOnManyToOne());
		addOn.add(new AddOnSQLTableExternalForeinKeyAsList());
		addOn.add(new AddOnDataJson());
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

	protected static <T> void setValuedb(final Class<?> type, final T data, final CountInOut iii, final Field field, final PreparedStatement ps) throws Exception {
		if (type == Long.class) {
			final Object tmp = field.get(data);
			if (tmp == null) {
				ps.setNull(iii.value, Types.BIGINT);
			} else {
				ps.setLong(iii.value, (Long) tmp);
			}
		} else if (type == long.class) {
			ps.setLong(iii.value, field.getLong(data));
		} else if (type == Integer.class) {
			final Object tmp = field.get(data);
			if (tmp == null) {
				ps.setNull(iii.value, Types.INTEGER);
			} else {
				ps.setInt(iii.value, (Integer) tmp);
			}
		} else if (type == int.class) {
			ps.setInt(iii.value, field.getInt(data));
		} else if (type == Float.class) {
			final Object tmp = field.get(data);
			if (tmp == null) {
				ps.setNull(iii.value, Types.FLOAT);
			} else {
				ps.setFloat(iii.value, (Float) tmp);
			}
		} else if (type == float.class) {
			ps.setFloat(iii.value, field.getFloat(data));
		} else if (type == Double.class) {
			final Object tmp = field.get(data);
			if (tmp == null) {
				ps.setNull(iii.value, Types.DOUBLE);
			} else {
				ps.setDouble(iii.value, (Double) tmp);
			}
		} else if (type == Double.class) {
			ps.setDouble(iii.value, field.getDouble(data));
		} else if (type == Boolean.class) {
			final Object tmp = field.get(data);
			if (tmp == null) {
				ps.setNull(iii.value, Types.INTEGER);
			} else {
				ps.setBoolean(iii.value, (Boolean) tmp);
			}
		} else if (type == boolean.class) {
			ps.setBoolean(iii.value, field.getBoolean(data));
		} else if (type == Timestamp.class) {
			final Object tmp = field.get(data);
			if (tmp == null) {
				ps.setNull(iii.value, Types.INTEGER);
			} else {
				ps.setTimestamp(iii.value, (Timestamp) tmp);
			}
		} else if (type == Date.class) {
			final Object tmp = field.get(data);
			if (tmp == null) {
				ps.setNull(iii.value, Types.INTEGER);
			} else {
				final Timestamp sqlDate = java.sql.Timestamp.from(((Date) tmp).toInstant());
				ps.setTimestamp(iii.value, sqlDate);
			}
		} else if (type == LocalDate.class) {
			final Object tmp = field.get(data);
			if (tmp == null) {
				ps.setNull(iii.value, Types.INTEGER);
			} else {
				final java.sql.Date sqlDate = java.sql.Date.valueOf((LocalDate) tmp);
				ps.setDate(iii.value, sqlDate);
			}
		} else if (type == LocalTime.class) {
			final Object tmp = field.get(data);
			if (tmp == null) {
				ps.setNull(iii.value, Types.INTEGER);
			} else {
				final java.sql.Time sqlDate = java.sql.Time.valueOf((LocalTime) tmp);
				ps.setTime(iii.value, sqlDate);
			}
		} else if (type == String.class) {
			final Object tmp = field.get(data);
			if (tmp == null) {
				ps.setNull(iii.value, Types.VARCHAR);
			} else {
				ps.setString(iii.value, (String) tmp);
			}
		} else if (type.isEnum()) {
			final Object tmp = field.get(data);
			if (tmp == null) {
				ps.setNull(iii.value, Types.VARCHAR);
			} else {
				ps.setString(iii.value, tmp.toString());
			}
		} else {
			throw new Exception("Unknown Field Type");
		}
		iii.inc();
	}
	
	// TODO: maybe wrap this if the use of sqlite ==> maybe some problems came with sqlite ...
	protected static <T> void setValueFromDb(final Class<?> type, final Object data, final CountInOut count, final Field field, final ResultSet rs, final CountInOut countNotNull) throws Exception {
		if (type == Long.class) {
			final Long tmp = rs.getLong(count.value);
			if (rs.wasNull()) {
				field.set(data, null);
			} else {
				//logger.debug("       ==> {}", tmp);
				field.set(data, tmp);
				countNotNull.inc();
			}
		} else if (type == long.class) {
			final Long tmp = rs.getLong(count.value);
			if (rs.wasNull()) {
				//field.set(data, null);
			} else {
				field.setLong(data, tmp);
				countNotNull.inc();
			}
		} else if (type == Integer.class) {
			final Integer tmp = rs.getInt(count.value);
			if (rs.wasNull()) {
				field.set(data, null);
			} else {
				field.set(data, tmp);
				countNotNull.inc();
			}
		} else if (type == int.class) {
			final Integer tmp = rs.getInt(count.value);
			if (rs.wasNull()) {
				//field.set(data, null);
			} else {
				field.setInt(data, tmp);
				countNotNull.inc();
			}
		} else if (type == Float.class) {
			final Float tmp = rs.getFloat(count.value);
			if (rs.wasNull()) {
				field.set(data, null);
			} else {
				field.set(data, tmp);
				countNotNull.inc();
			}
		} else if (type == float.class) {
			final Float tmp = rs.getFloat(count.value);
			if (rs.wasNull()) {
				//field.set(data, null);
			} else {
				field.setFloat(data, tmp);
				countNotNull.inc();
			}
		} else if (type == Double.class) {
			final Double tmp = rs.getDouble(count.value);
			if (rs.wasNull()) {
				field.set(data, null);
			} else {
				field.set(data, tmp);
				countNotNull.inc();
			}
		} else if (type == double.class) {
			final Double tmp = rs.getDouble(count.value);
			if (rs.wasNull()) {
				//field.set(data, null);
			} else {
				field.setDouble(data, tmp);
				countNotNull.inc();
			}
		} else if (type == Boolean.class) {
			final Boolean tmp = rs.getBoolean(count.value);
			if (rs.wasNull()) {
				field.set(data, null);
			} else {
				field.set(data, tmp);
				countNotNull.inc();
			}
		} else if (type == boolean.class) {
			final Boolean tmp = rs.getBoolean(count.value);
			if (rs.wasNull()) {
				//field.set(data, null);
			} else {
				field.setBoolean(data, tmp);
				countNotNull.inc();
			}
		} else if (type == Timestamp.class) {
			final Timestamp tmp = rs.getTimestamp(count.value);
			if (rs.wasNull()) {
				field.set(data, null);
			} else {
				field.set(data, tmp);
				countNotNull.inc();
			}
		} else if (type == Date.class) {
			try {
				final Timestamp tmp = rs.getTimestamp(count.value);
				if (rs.wasNull()) {
					field.set(data, null);
				} else {
					field.set(data, Date.from(tmp.toInstant()));
					countNotNull.inc();
				}
			} catch (final SQLException ex) {
				final String tmp = rs.getString(count.value);
				LOGGER.error("Fail to parse the SQL time !!! {}", tmp);
				LOGGER.error("Fail to parse the SQL time !!! {}", Date.parse(tmp));
				if (rs.wasNull()) {
					field.set(data, null);
				} else {
					field.set(data, Date.parse(tmp));
					countNotNull.inc();
				}
			}
		} else if (type == LocalDate.class) {
			final java.sql.Date tmp = rs.getDate(count.value);
			if (rs.wasNull()) {
				field.set(data, null);
			} else {
				field.set(data, tmp.toLocalDate());
				countNotNull.inc();
			}
		} else if (type == LocalTime.class) {
			final java.sql.Time tmp = rs.getTime(count.value);
			if (rs.wasNull()) {
				field.set(data, null);
			} else {
				field.set(data, tmp.toLocalTime());
				countNotNull.inc();
			}
		} else if (type == String.class) {
			final String tmp = rs.getString(count.value);
			if (rs.wasNull()) {
				field.set(data, null);
			} else {
				field.set(data, tmp);
				countNotNull.inc();
			}
		} else if (type.isEnum()) {
			final String tmp = rs.getString(count.value);
			if (rs.wasNull()) {
				field.set(data, null);
			} else {
				final Object[] arr = type.getEnumConstants();
				for (final Object elem : arr) {
					if (elem.toString().equals(tmp)) {
						field.set(data, elem);
						countNotNull.inc();
						break;
					}
				}
				// TODO: maybe do something stupid if not exist ???
			}
		} else {
			throw new Exception("Unknown Field Type");
		}
		count.inc();
	}

	public static boolean isAddOnField(final Field field) {
		return findAddOnforField(field) != null;
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
		return insert(data, null);
	}
	
	public static <T> T insert(final T data, final QueryOptions options) throws Exception {
		final Class<?> clazz = data.getClass();
		
		DBEntry entry = DBEntry.createInterface(GlobalConfiguration.dbConfig);
		// real add in the BDD:
		try {
			final String tableName = AnnotationTools.getTableName(clazz, options);
			//boolean createIfNotExist = clazz.getDeclaredAnnotationsByType(SQLIfNotExists.class).length != 0;
			final StringBuilder query = new StringBuilder();
			query.append("INSERT INTO `");
			query.append(tableName);
			query.append("` (");

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
				if (addOn != null && !addOn.canInsert(elem)) {
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
					if (tmp == null && elem.getDeclaredAnnotationsByType(DataDefault.class).length != 0) {
						continue;
					}
				}
				count++;
				final String name = AnnotationTools.getFieldName(elem);
				if (firstField) {
					firstField = false;
				} else {
					query.append(",");
				}
				query.append(" `");
				query.append(name);
				query.append("`");
			}
			firstField = true;
			query.append(") VALUES (");
			for (int iii = 0; iii < count; iii++) {
				if (firstField) {
					firstField = false;
				} else {
					query.append(",");
				}
				query.append("?");
			}
			query.append(")");
			LOGGER.warn("generate the query: '{}'", query.toString());
			// prepare the request:
			final PreparedStatement ps = entry.connection.prepareStatement(query.toString(), Statement.RETURN_GENERATED_KEYS);
			Field primaryKeyField = null;
			final CountInOut iii = new CountInOut(1);
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
				if (addOn != null && !addOn.canInsert(elem)) {
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
					addOn.insertData(ps, elem, data, iii);
				} else {
					// Generic class insertion...
					final Class<?> type = elem.getType();
					if (!type.isPrimitive()) {
						final Object tmp = elem.get(data);
						if (tmp == null && elem.getDeclaredAnnotationsByType(DataDefault.class).length != 0) {
							continue;
						}
					}
					setValuedb(type, data, iii, elem, ps);
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

	/**
	 * Update an object with the inserted json data
	 *
	 * @param <T> Type of the object to insert
	 * @param <ID_TYPE> Master key on the object manage with @Id
	 * @param clazz Class reference of the insertion model
	 * @param id Key to insert data
	 * @param jsonData Json data  (partial) values to update
	 * @return the number of object updated
	 * @throws Exception
	 */
	public static <T, ID_TYPE> int updateWithJson(final Class<T> clazz, final ID_TYPE id, final String jsonData) throws Exception {
		// Find the ID field type ....
		final Field idField = AnnotationTools.getIdField(clazz);
		if (idField == null) {
			throw new Exception("The class have no annotation @Id ==> can not determine the default type searching");
		}
		// check the compatibility of the id and the declared ID
		final Class<?> typeClass = idField.getType();
		if (id == typeClass) {
			throw new Exception("Request update with the wrong type ...");
		}
		// Udpade Json Value
		return updateWithJson(clazz, new QueryCondition(AnnotationTools.getFieldName(idField), "=", id), jsonData);
	}

	public static <T> int updateWithJson(final Class<T> clazz, final QueryItem condition, final String jsonData) throws Exception {
		final ObjectMapper mapper = new ObjectMapper();
		// parse the object to be sure the data are valid:
		final T data = mapper.readValue(jsonData, clazz);
		// Read the tree to filter injection of data:
		final JsonNode root = mapper.readTree(jsonData);
		final List<String> keys = new ArrayList<>();
		final var iterator = root.fieldNames();
		iterator.forEachRemaining(e -> keys.add(e));
		return updateWhere(data, condition, null, keys);
	}

	public static <T, ID_TYPE> int update(final T data, final ID_TYPE id) throws Exception {
		return update(data, id, null);
	}

	public static <T> int updateWhere(final T data, final QueryItem condition) throws Exception {
		return updateWhere(data, condition, null, null);
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
	public static <T, ID_TYPE> int update(final T data, final ID_TYPE id, final List<String> filterValue) throws Exception {
		// Find the ID field type ....
		final Field idField = AnnotationTools.getIdField(data.getClass());
		if (idField == null) {
			throw new Exception("The class have no annotation @Id ==> can not determine the default type searching");
		}
		// check the compatibility of the id and the declared ID
		final Class<?> typeClass = idField.getType();
		if (id == typeClass) {
			throw new Exception("Request update with the wriong type ...");
		}
		return updateWhere(data, new QueryCondition(AnnotationTools.getFieldName(idField), "=", id), null, filterValue);
	}

	public static <T> int updateWhere(final T data, final QueryItem condition, final QueryOptions options, final List<String> filterValue) throws Exception {
		final Class<?> clazz = data.getClass();
		//public static NodeSmall createNode(String typeInNode, String name, String description, Long parentId) {

		DBEntry entry = DBEntry.createInterface(GlobalConfiguration.dbConfig);
		// real add in the BDD:
		try {
			final String tableName = AnnotationTools.getTableName(clazz, options);
			//boolean createIfNotExist = clazz.getDeclaredAnnotationsByType(SQLIfNotExists.class).length != 0;
			final StringBuilder query = new StringBuilder();
			query.append("UPDATE `");
			query.append(tableName);
			query.append("` SET ");

			boolean firstField = true;
			for (final Field field : clazz.getFields()) {
				// static field is only for internal global declaration ==> remove it ..
				if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
					continue;
				}
				final String name = AnnotationTools.getFieldName(field);
				if (filterValue != null) {
					if (!filterValue.contains(name)) {
						continue;
					}
				} else if (AnnotationTools.isGenericField(field)) {
					continue;
				}
				final DataAccessAddOn addOn = findAddOnforField(field);
				if (addOn != null && !addOn.canInsert(field)) {
					continue;
				}
				if (!field.getClass().isPrimitive()) {
					final Object tmp = field.get(data);
					if (tmp == null && field.getDeclaredAnnotationsByType(DataDefault.class).length != 0) {
						continue;
					}
				}
				if (firstField) {
					firstField = false;
				} else {
					query.append(",");
				}
				query.append(" `");
				query.append(name);
				query.append("` = ? ");
			}
			query.append(" ");
			final String deletedFieldName = AnnotationTools.getDeletedFieldName(clazz);
			whereAppendQuery(query, tableName, condition, null, deletedFieldName);
			firstField = true;
			LOGGER.debug("generate the query: '{}'", query.toString());
			// prepare the request:
			final PreparedStatement ps = entry.connection.prepareStatement(query.toString(), Statement.RETURN_GENERATED_KEYS);
			final CountInOut iii = new CountInOut(1);
			for (final Field field : clazz.getFields()) {
				// static field is only for internal global declaration ==> remove it ..
				if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
					continue;
				}
				final String name = AnnotationTools.getFieldName(field);
				if (filterValue != null) {
					if (!filterValue.contains(name)) {
						continue;
					}
				} else if (AnnotationTools.isGenericField(field)) {
					continue;
				}
				final DataAccessAddOn addOn = findAddOnforField(field);
				if (addOn != null && !addOn.canInsert(field)) {
					continue;
				}
				if (addOn == null) {
					final Class<?> type = field.getType();
					if (!type.isPrimitive()) {
						final Object tmp = field.get(data);
						if (tmp == null && field.getDeclaredAnnotationsByType(DataDefault.class).length != 0) {
							continue;
						}
					}
					setValuedb(type, data, iii, field, ps);
				} else {
					addOn.insertData(ps, field, data, iii);
				}
			}
			whereInjectValue(ps, condition, iii);
			
			return ps.executeUpdate();
		} catch (final SQLException ex) {
			ex.printStackTrace();
		} finally {
			entry.close();
			entry = null;
		}
		return 0;
	}

	static void addElement(final PreparedStatement ps, final Object value, final CountInOut iii) throws Exception {
		if (value instanceof final Long tmp) {
			ps.setLong(iii.value, tmp);
		} else if (value instanceof final Integer tmp) {
			ps.setInt(iii.value, tmp);
		} else if (value instanceof final String tmp) {
			ps.setString(iii.value, tmp);
		} else if (value instanceof final Short tmp) {
			ps.setShort(iii.value, tmp);
		} else if (value instanceof final Byte tmp) {
			ps.setByte(iii.value, tmp);
		} else if (value instanceof final Float tmp) {
			ps.setFloat(iii.value, tmp);
		} else if (value instanceof final Double tmp) {
			ps.setDouble(iii.value, tmp);
		} else if (value instanceof final Boolean tmp) {
			ps.setBoolean(iii.value, tmp);
		} else if (value instanceof final Boolean tmp) {
			ps.setBoolean(iii.value, tmp);
		} else if (value instanceof final Timestamp tmp) {
			ps.setTimestamp(iii.value, tmp);
		} else if (value instanceof final Date tmp) {
			ps.setTimestamp(iii.value, java.sql.Timestamp.from((tmp).toInstant()));
		} else if (value instanceof final LocalDate tmp) {
			ps.setDate(iii.value, java.sql.Date.valueOf(tmp));
		} else if (value instanceof final LocalTime tmp) {
			ps.setTime(iii.value, java.sql.Time.valueOf(tmp));
		} else if (value.getClass().isEnum()) {
			ps.setString(iii.value, value.toString());
		} else {
			throw new Exception("Not manage type ==> need to add it ...");
		}
	}

	public static void whereAppendQuery(final StringBuilder query, final String tableName, final QueryItem condition, final QueryOptions options, final String deletedFieldName)
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
				query.append(" WHERE ");
				query.append(tableName);
				query.append(".");
				query.append(deletedFieldName);
				query.append(" = false ");
			}
			return;
		}
		query.append(" WHERE (");
		condition.generateQuerry(query, tableName);

		query.append(") ");
		if (exclude_deleted && deletedFieldName != null) {
			query.append("AND ");
			query.append(tableName);
			query.append(".");
			query.append(deletedFieldName);
			query.append(" = false ");
		}
	}

	public static void whereInjectValue(final PreparedStatement ps, final QueryItem condition, final CountInOut iii) throws Exception {
		// Check if we have a condition to generate
		if (condition != null) {
			condition.injectQuerry(ps, iii);
		}
	}

	public static int executeSimpleQuerry(final String query, final boolean root) throws SQLException, IOException {
		final DBEntry entry = DBEntry.createInterface(GlobalConfiguration.dbConfig, root);
		final Statement stmt = entry.connection.createStatement();
		return stmt.executeUpdate(query);
	}

	public static int executeSimpleQuerry(final String query) throws SQLException, IOException {
		return executeSimpleQuerry(query, false);
	}

	public static boolean executeQuerry(final String query, final boolean root) throws SQLException, IOException {
		final DBEntry entry = DBEntry.createInterface(GlobalConfiguration.dbConfig, root);
		final Statement stmt = entry.connection.createStatement();
		return stmt.execute(query);
	}

	public static boolean executeQuerry(final String query) throws SQLException, IOException {
		return executeQuerry(query, false);
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

	public static void generateSelectField(final StringBuilder querySelect, final StringBuilder query, final Class<?> clazz, final QueryOptions options, final CountInOut count) throws Exception {
		final boolean readAllfields = QueryOptions.readAllFields(options);
		final String tableName = AnnotationTools.getTableName(clazz, options);
		boolean firstField = true;
		
		for (final Field elem : clazz.getFields()) {
			// static field is only for internal global declaration ==> remove it ..
			if (java.lang.reflect.Modifier.isStatic(elem.getModifiers())) {
				continue;
			}
			final DataAccessAddOn addOn = findAddOnforField(elem);
			if (addOn != null && !addOn.canRetrieve(elem)) {
				continue;
			}
			// TODO: Manage it with AddOn
			final boolean notRead = AnnotationTools.isdefaultNotRead(elem);
			if (!readAllfields && notRead) {
				continue;
			}
			final String name = AnnotationTools.getFieldName(elem);
			if (firstField) {
				firstField = false;
			} else {
				querySelect.append(",");
			}
			querySelect.append(" ");
			if (addOn != null) {
				addOn.generateQuerry(tableName, elem, querySelect, query, name, count, options);
			} else {
				querySelect.append(tableName);
				querySelect.append(".");
				querySelect.append(name);
				count.inc();
			}
		}
	}

	// TODO: set limit as an query Option...
	@SuppressWarnings("unchecked")
	public static <T> List<T> getsWhere(final Class<T> clazz, final QueryItem condition, final String orderBy, final QueryOptions options, final Integer linit) throws Exception {
		final List<LazyGetter> lazyCall = new ArrayList<>();
		final String deletedFieldName = AnnotationTools.getDeletedFieldName(clazz);
		DBEntry entry = DBEntry.createInterface(GlobalConfiguration.dbConfig);
		final List<T> outs = new ArrayList<>();
		// real add in the BDD:
		try {
			final CountInOut count = new CountInOut();
			final StringBuilder querySelect = new StringBuilder();
			StringBuilder query = new StringBuilder();
			final String tableName = AnnotationTools.getTableName(clazz, options);
			querySelect.append("SELECT ");
			query.append(" FROM `");
			query.append(tableName);
			query.append("` ");

			generateSelectField(querySelect, query, clazz, options, count);
			querySelect.append(query.toString());
			query = querySelect;
			whereAppendQuery(query, tableName, condition, options, deletedFieldName);
			if (orderBy != null && orderBy.length() >= 1) {
				query.append(" ORDER BY ");
				query.append(orderBy);
			}
			if (linit != null && linit >= 1) {
				query.append(" LIMIT " + linit);
			}
			LOGGER.debug("generate the query: '{}'", query.toString());
			// prepare the request:
			final PreparedStatement ps = entry.connection.prepareStatement(query.toString(), Statement.RETURN_GENERATED_KEYS);
			final CountInOut iii = new CountInOut(1);
			whereInjectValue(ps, condition, iii);
			// execute the request
			final ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				count.value = 1;
				final CountInOut countNotNull = new CountInOut(0);
				final Object data = createObjectFromSQLRequest(rs, clazz, count, countNotNull, options, lazyCall);
				final T out = (T) data;
				outs.add(out);
			}
			LOGGER.info("Async calls: {}", lazyCall.size());
			for (final LazyGetter elem : lazyCall) {
				elem.doRequest();
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

	public static Object createObjectFromSQLRequest(final ResultSet rs, final Class<?> clazz, final CountInOut count, final CountInOut countNotNull, final QueryOptions options,
			final List<LazyGetter> lazyCall) throws Exception {
		final boolean readAllfields = QueryOptions.readAllFields(options);
		// TODO: manage class that is defined inside a class ==> Not manage for now...
		final Object data = clazz.getConstructors()[0].newInstance();
		for (final Field elem : clazz.getFields()) {
			// static field is only for internal global declaration ==> remove it ..
			if (java.lang.reflect.Modifier.isStatic(elem.getModifiers())) {
				continue;
			}
			final DataAccessAddOn addOn = findAddOnforField(elem);
			if (addOn != null && !addOn.canRetrieve(elem)) {
				continue;
			}
			// TODO: Manage it with AddOn
			final boolean notRead = AnnotationTools.isdefaultNotRead(elem);
			if (!readAllfields && notRead) {
				continue;
			}
			if (addOn != null) {
				addOn.fillFromQuerry(rs, elem, data, count, options, lazyCall);
			} else {
				setValueFromDb(elem.getType(), data, count, elem, rs, countNotNull);
			}
		}
		return data;
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
		return delete(clazz, id, null);
	}

	public static int delete(final Class<?> clazz, final long id, final QueryOptions options) throws Exception {
		final String hasDeletedFieldName = AnnotationTools.getDeletedFieldName(clazz);
		if (hasDeletedFieldName != null) {
			return deleteSoft(clazz, id, options);
		} else {
			return deleteHard(clazz, id, options);
		}
	}

	public static int deleteWhere(final Class<?> clazz, final QueryItem condition, final QueryOptions options) throws Exception {
		final String hasDeletedFieldName = AnnotationTools.getDeletedFieldName(clazz);
		if (hasDeletedFieldName != null) {
			return deleteSoftWhere(clazz, condition, options);
		} else {
			return deleteHardWhere(clazz, condition, options);
		}
	}

	public static int deleteHard(final Class<?> clazz, final long id, final QueryOptions options) throws Exception {
		return deleteHardWhere(clazz, new QueryCondition("id", "=", id), options);
	}

	public static int deleteHardWhere(final Class<?> clazz, final QueryItem condition, final QueryOptions options) throws Exception {
		final String tableName = AnnotationTools.getTableName(clazz, options);
		final String deletedFieldName = AnnotationTools.getDeletedFieldName(clazz);
		// find the deleted field
		
		DBEntry entry = DBEntry.createInterface(GlobalConfiguration.dbConfig);
		final StringBuilder query = new StringBuilder();
		query.append("DELETE FROM `");
		query.append(tableName);
		query.append("` ");
		whereAppendQuery(query, tableName, condition, null, deletedFieldName);
		try {
			LOGGER.debug("APPLY: {}", query.toString());
			final PreparedStatement ps = entry.connection.prepareStatement(query.toString());
			final CountInOut iii = new CountInOut(1);
			whereInjectValue(ps, condition, iii);
			return ps.executeUpdate();
		} finally {
			entry.close();
			entry = null;
		}
	}

	private static int deleteSoft(final Class<?> clazz, final long id, final QueryOptions options) throws Exception {
		return deleteSoftWhere(clazz, new QueryCondition("id", "=", id), options);
	}

	public static String getDBNow() {
		if (!"sqlite".equals(ConfigBaseVariable.getDBType())) {
			return "now(3)";
		}
		return "DATE()";
	}

	public static int deleteSoftWhere(final Class<?> clazz, final QueryItem condition, final QueryOptions options) throws Exception {
		final String tableName = AnnotationTools.getTableName(clazz, options);
		final String deletedFieldName = AnnotationTools.getDeletedFieldName(clazz);
		/*
		String updateFieldName = null;
		if ("sqlite".equalsIgnoreCase(ConfigBaseVariable.getDBType())) {
			updateFieldName = AnnotationTools.getUpdatedFieldName(clazz);
		}
		*/
		// find the deleted field

		DBEntry entry = DBEntry.createInterface(GlobalConfiguration.dbConfig);
		final StringBuilder query = new StringBuilder();
		query.append("UPDATE `");
		query.append(tableName);
		query.append("` SET `");
		query.append(deletedFieldName);
		query.append("`=true ");
		/*
		 * The trigger work well, but the timestamp is store @ seconds...
		if (updateFieldName != null) {
			// done only in SQLite (the trigger does not work...
			query.append(", `");
			query.append(updateFieldName);
			query.append("`=DATE()");
		}
		*/
		whereAppendQuery(query, tableName, condition, null, deletedFieldName);
		try {
			LOGGER.debug("APPLY UPDATE: {}", query.toString());
			final PreparedStatement ps = entry.connection.prepareStatement(query.toString());
			final CountInOut iii = new CountInOut(1);
			whereInjectValue(ps, condition, iii);
			return ps.executeUpdate();
		} finally {
			entry.close();
			entry = null;
		}
	}
	
	public static int unsetDelete(final Class<?> clazz, final long id) throws Exception {
		return unsetDeleteWhere(clazz, new QueryCondition("id", "=", id), null);
	}
	
	public static int unsetDelete(final Class<?> clazz, final long id, final QueryOptions options) throws Exception {
		return unsetDeleteWhere(clazz, new QueryCondition("id", "=", id), options);
	}

	public static int unsetDeleteWhere(final Class<?> clazz, final QueryItem condition, final QueryOptions options) throws Exception {
		final String tableName = AnnotationTools.getTableName(clazz, options);
		final String deletedFieldName = AnnotationTools.getDeletedFieldName(clazz);
		if (deletedFieldName == null) {
			throw new Exception("The class " + clazz.getCanonicalName() + " has no deleted field");
		}
		DBEntry entry = DBEntry.createInterface(GlobalConfiguration.dbConfig);
		final StringBuilder query = new StringBuilder();
		query.append("UPDATE `");
		query.append(tableName);
		query.append("` SET `");
		query.append(deletedFieldName);
		query.append("`=false ");
		/*
		 * is is needed only for SQLite ???
		query.append("`modify_date`=");
		query.append(getDBNow());
		query.append(", ");
		*/
		// need to disable the deleted false because the model must be unselected to be updated.
		options.put(QueryOptions.SQL_DELETED_DISABLE, true);
		whereAppendQuery(query, tableName, condition, options, deletedFieldName);
		try {
			final PreparedStatement ps = entry.connection.prepareStatement(query.toString());
			final CountInOut iii = new CountInOut(1);
			whereInjectValue(ps, condition, iii);
			return ps.executeUpdate();
		} finally {
			entry.close();
			entry = null;
		}
	}
	
}