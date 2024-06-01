package org.kar.archidata.dataAccess;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.kar.archidata.annotation.AnnotationTools;
import org.kar.archidata.annotation.CreationTimestamp;
import org.kar.archidata.annotation.UpdateTimestamp;
import org.kar.archidata.dataAccess.addOn.AddOnDataJson;
import org.kar.archidata.dataAccess.addOn.AddOnManyToMany;
import org.kar.archidata.dataAccess.addOn.AddOnManyToOne;
import org.kar.archidata.dataAccess.addOn.AddOnOneToMany;
import org.kar.archidata.dataAccess.options.CheckFunction;
import org.kar.archidata.dataAccess.options.Condition;
import org.kar.archidata.dataAccess.options.DBInterfaceOption;
import org.kar.archidata.dataAccess.options.DBInterfaceRoot;
import org.kar.archidata.dataAccess.options.FilterValue;
import org.kar.archidata.dataAccess.options.GroupBy;
import org.kar.archidata.dataAccess.options.Limit;
import org.kar.archidata.dataAccess.options.OrderBy;
import org.kar.archidata.dataAccess.options.QueryOption;
import org.kar.archidata.dataAccess.options.TransmitKey;
import org.kar.archidata.db.DBEntry;
import org.kar.archidata.exception.DataAccessException;
import org.kar.archidata.tools.ConfigBaseVariable;
import org.kar.archidata.tools.DateTools;
import org.kar.archidata.tools.UuidUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.InternalServerErrorException;

/* TODO list:
   - Manage to group of SQL action to permit to commit only at the end.
 */

/** Data access is an abstraction class that permit to access on the DB with a function wrapping that permit to minimize the SQL writing of SQL code. This interface support the SQL and SQLite
 * back-end. */
public class DataAccess {
	static final Logger LOGGER = LoggerFactory.getLogger(DataAccess.class);
	// by default we manage some add-on that permit to manage non-native model (like json serialization, List of external key as String list...)
	static final List<DataAccessAddOn> addOn = new ArrayList<>();

	static {
		addOn.add(new AddOnManyToMany());
		addOn.add(new AddOnManyToOne());
		addOn.add(new AddOnOneToMany());
		addOn.add(new AddOnDataJson());
	}

	/** Add a new add-on on the current management.
	 * @param addOn instantiate object on the Add-on */
	public static void addAddOn(final DataAccessAddOn addOn) {
		DataAccess.addOn.add(addOn);
	}

	public DataAccess() {

	}

	public static boolean isDBExist(final String name, final QueryOption... option)
			throws InternalServerErrorException {
		final QueryOptions options = new QueryOptions(option);
		if ("sqlite".equals(ConfigBaseVariable.getDBType())) {
			// no base manage in sqLite ...
			// TODO: check if the file exist or not ...
			return true;
		}
		DBEntry entry;
		try {
			entry = DBInterfaceOption.getAutoEntry(options);
		} catch (final IOException ex) {
			ex.printStackTrace();
			LOGGER.error("Can not check if the DB exist!!! {}", ex.getMessage());

			// TODO: TO test

			return false;
		}
		try {
			// TODO : Maybe connect with a temporary not specified connection interface to a db ...
			final PreparedStatement ps = entry.connection.prepareStatement("show databases");
			final ResultSet rs = ps.executeQuery();
			// LOGGER.info("List all tables: equals? '{}'", name);
			while (rs.next()) {
				final String data = rs.getString(1);
				// LOGGER.info(" - '{}'", data);
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
			return 1 == DataAccess.executeSimpleQuery("CREATE DATABASE `" + name + "`;", new DBInterfaceRoot(true));
		} catch (final SQLException | IOException ex) {
			ex.printStackTrace();
			LOGGER.error("Can not check if the DB exist!!! {}", ex.getMessage());
			return false;
		}
	}

	public static boolean isTableExist(final String name, final QueryOption... option)
			throws InternalServerErrorException {
		final QueryOptions options = new QueryOptions(option);
		try {
			String request = "";
			if ("sqlite".equals(ConfigBaseVariable.getDBType())) {
				request = """
						SELECT COUNT(*) AS total
						FROM sqlite_master
						WHERE type = 'table'
						AND name = ?;
						""";
				// PreparedStatement ps = entry.connection.prepareStatement("show tables");
				final DBEntry entry = DBInterfaceOption.getAutoEntry(options);
				final PreparedStatement ps = entry.connection.prepareStatement(request);
				ps.setString(1, name);
				final ResultSet ret = ps.executeQuery();
				final int count = ret.getInt("total");
				return count == 1;
			} else {
				final DBEntry entry = DBInterfaceOption.getAutoEntry(options);
				// TODO : Maybe connect with a temporary not specified connection interface to a db ...
				final PreparedStatement ps = entry.connection.prepareStatement("show tables");
				final ResultSet rs = ps.executeQuery();
				// LOGGER.info("List all tables: equals? '{}'", name);
				while (rs.next()) {
					final String data = rs.getString(1);
					// LOGGER.info(" - '{}'", data);
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

	/** Extract a list of Long with "-" separated element from a SQL input data.
	 * @param rs Result Set of the BDD
	 * @param iii Id in the result set
	 * @return The list of Long value
	 * @throws SQLException if an error is generated in the SQL request. */
	public static List<Long> getListOfIds(final ResultSet rs, final int iii, final String separator)
			throws SQLException {
		final String trackString = rs.getString(iii);
		if (rs.wasNull()) {
			return null;
		}
		final List<Long> out = new ArrayList<>();
		final String[] elements = trackString.split(separator);
		for (final String elem : elements) {
			final Long tmp = Long.parseLong(elem);
			out.add(tmp);
		}
		return out;
	}

	/** Extract a list of UUID with "-" separated element from a SQL input data.
	 * @param rs Result Set of the BDD
	 * @param iii Id in the result set
	 * @return The list of Long value
	 * @throws SQLException if an error is generated in the SQL request. */
	public static List<UUID> getListOfUUIDs(final ResultSet rs, final int iii, final String separator)
			throws SQLException {
		final String trackString = rs.getString(iii);
		if (rs.wasNull()) {
			return null;
		}
		final List<UUID> out = new ArrayList<>();
		final String[] elements = trackString.split(separator);
		for (final String elem : elements) {
			final UUID tmp = UUID.fromString(elem);
			out.add(tmp);
		}
		return out;
	}

	public static byte[][] splitIntoGroupsOf16Bytes(final byte[] input) {
		final int inputLength = input.length;
		final int numOfGroups = (inputLength + 15) / 16; // Calculate the number of groups needed
		final byte[][] groups = new byte[numOfGroups][16];

		for (int i = 0; i < numOfGroups; i++) {
			final int startIndex = i * 16;
			final int endIndex = Math.min(startIndex + 16, inputLength);
			groups[i] = Arrays.copyOfRange(input, startIndex, endIndex);
		}

		return groups;
	}

	public static List<UUID> getListOfRawUUIDs(final ResultSet rs, final int iii)
			throws SQLException, DataAccessException {
		final byte[] trackString = rs.getBytes(iii);
		if (rs.wasNull()) {
			return null;
		}
		final byte[][] elements = splitIntoGroupsOf16Bytes(trackString);
		final List<UUID> out = new ArrayList<>();
		for (final byte[] elem : elements) {
			final UUID tmp = UuidUtils.asUuid(elem);
			out.add(tmp);
		}
		return out;
	}

	public static UUID getListOfRawUUID(final ResultSet rs, final int iii) throws SQLException, DataAccessException {
		final byte[] elem = rs.getBytes(iii);
		if (rs.wasNull()) {
			return null;
		}
		return UuidUtils.asUuid(elem);
	}

	protected static <T> void setValuedb(
			final Class<?> type,
			final T data,
			final CountInOut iii,
			final Field field,
			final PreparedStatement ps) throws Exception {
		if (type == UUID.class) {
			final Object tmp = field.get(data);
			if (tmp == null) {
				ps.setNull(iii.value, Types.BINARY);
			} else {
				final byte[] dataByte = UuidUtils.asBytes((UUID) tmp);
				ps.setBytes(iii.value, dataByte);
			}
		} else if (type == Long.class) {
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
		} else if (type == Instant.class) {
			final Object tmp = field.get(data);
			if (tmp == null) {
				ps.setNull(iii.value, Types.INTEGER);
			} else {
				final String sqlDate = ((Instant) tmp).toString();
				ps.setString(iii.value, sqlDate);
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
			throw new DataAccessException("Unknown Field Type");
		}
		iii.inc();
	}

	protected static <T> void setValueFromDb(
			final Class<?> type,
			final Object data,
			final CountInOut count,
			final Field field,
			final ResultSet rs,
			final CountInOut countNotNull) throws Exception {
		if (type == UUID.class) {
			final byte[] tmp = rs.getBytes(count.value);
			// final UUID tmp = rs.getObject(count.value, UUID.class);
			if (rs.wasNull()) {
				field.set(data, null);
			} else {
				// field.set(data, tmp);
				final UUID uuid = UuidUtils.asUuid(tmp);
				field.set(data, uuid);
				countNotNull.inc();
			}
		} else if (type == Long.class) {
			final Long tmp = rs.getLong(count.value);
			if (rs.wasNull()) {
				field.set(data, null);
			} else {
				field.set(data, tmp);
				countNotNull.inc();
			}
		} else if (type == long.class) {
			final Long tmp = rs.getLong(count.value);
			if (rs.wasNull()) {
				// field.set(data, null);
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
				// field.set(data, null);
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
				// field.set(data, null);
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
				// field.set(data, null);
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
				// field.set(data, null);
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
				if (rs.wasNull()) {
					field.set(data, null);
				} else {
					final Date date = DateTools.parseDate(tmp);
					LOGGER.error("Fail to parse the SQL time !!! {}", date);
					field.set(data, date);
					countNotNull.inc();
				}
			}
		} else if (type == Instant.class) {
			final String tmp = rs.getString(count.value);
			if (rs.wasNull()) {
				field.set(data, null);
			} else {
				field.set(data, Instant.parse(tmp));
				countNotNull.inc();
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
				boolean find = false;
				final Object[] arr = type.getEnumConstants();
				for (final Object elem : arr) {
					if (elem.toString().equals(tmp)) {
						field.set(data, elem);
						countNotNull.inc();
						find = true;
						break;
					}
				}
				if (!find) {
					throw new DataAccessException("Enum value does not exist in the Model: '" + tmp + "'");
				}
			}
		} else {
			throw new DataAccessException("Unknown Field Type");
		}
		count.inc();
	}

	// TODO: this function will replace the previous one !!!
	protected static RetreiveFromDB createSetValueFromDbCallback(final int count, final Field field) throws Exception {
		final Class<?> type = field.getType();
		if (type == UUID.class) {
			return (final ResultSet rs, final Object obj) -> {

				final byte[] tmp = rs.getBytes(count);
				// final UUID tmp = rs.getObject(count, UUID.class);
				if (rs.wasNull()) {
					field.set(obj, null);
				} else {
					// field.set(obj, tmp);
					final UUID uuid = UuidUtils.asUuid(tmp);
					field.set(obj, uuid);
				}
			};
		}
		if (type == Long.class) {
			return (final ResultSet rs, final Object obj) -> {
				final Long tmp = rs.getLong(count);
				if (rs.wasNull()) {
					field.set(obj, null);
				} else {
					field.set(obj, tmp);
				}
			};
		}
		if (type == long.class) {
			return (final ResultSet rs, final Object obj) -> {
				final Long tmp = rs.getLong(count);
				if (rs.wasNull()) {
					// field.set(data, null);
				} else {
					field.setLong(obj, tmp);
				}
			};
		}
		if (type == Integer.class) {
			return (final ResultSet rs, final Object obj) -> {
				final Integer tmp = rs.getInt(count);
				if (rs.wasNull()) {
					field.set(obj, null);
				} else {
					field.set(obj, tmp);
				}
			};
		}
		if (type == int.class) {
			return (final ResultSet rs, final Object obj) -> {
				final Integer tmp = rs.getInt(count);
				if (rs.wasNull()) {
					// field.set(obj, null);
				} else {
					field.setInt(obj, tmp);
				}
			};
		}
		if (type == Float.class) {
			return (final ResultSet rs, final Object obj) -> {
				final Float tmp = rs.getFloat(count);
				if (rs.wasNull()) {
					field.set(obj, null);
				} else {
					field.set(obj, tmp);
				}
			};
		}
		if (type == float.class) {
			return (final ResultSet rs, final Object obj) -> {
				final Float tmp = rs.getFloat(count);
				if (rs.wasNull()) {
					// field.set(obj, null);
				} else {
					field.setFloat(obj, tmp);
				}
			};
		}
		if (type == Double.class) {
			return (final ResultSet rs, final Object obj) -> {
				final Double tmp = rs.getDouble(count);
				if (rs.wasNull()) {
					field.set(obj, null);
				} else {
					field.set(obj, tmp);
				}
			};
		}
		if (type == double.class) {
			return (final ResultSet rs, final Object obj) -> {
				final Double tmp = rs.getDouble(count);
				if (rs.wasNull()) {
					// field.set(obj, null);
				} else {
					field.setDouble(obj, tmp);
				}
			};
		}
		if (type == Boolean.class) {
			return (final ResultSet rs, final Object obj) -> {
				final Boolean tmp = rs.getBoolean(count);
				if (rs.wasNull()) {
					field.set(obj, null);
				} else {
					field.set(obj, tmp);
				}
			};
		}
		if (type == boolean.class) {
			return (final ResultSet rs, final Object obj) -> {
				final Boolean tmp = rs.getBoolean(count);
				if (rs.wasNull()) {
					// field.set(obj, null);
				} else {
					field.setBoolean(obj, tmp);
				}
			};
		}
		if (type == Timestamp.class) {
			return (final ResultSet rs, final Object obj) -> {
				final Timestamp tmp = rs.getTimestamp(count);
				if (rs.wasNull()) {
					field.set(obj, null);
				} else {
					field.set(obj, tmp);
				}
			};
		}
		if (type == Date.class) {
			return (final ResultSet rs, final Object obj) -> {
				try {
					final Timestamp tmp = rs.getTimestamp(count);
					if (rs.wasNull()) {
						field.set(obj, null);
					} else {
						field.set(obj, Date.from(tmp.toInstant()));
					}
				} catch (final SQLException ex) {
					final String tmp = rs.getString(count);
					LOGGER.error("Fail to parse the SQL time !!! {}", tmp);
					if (rs.wasNull()) {
						field.set(obj, null);
					} else {
						final Date date = DateTools.parseDate(tmp);
						LOGGER.error("Fail to parse the SQL time !!! {}", date);
						field.set(obj, date);
					}
				}
			};
		}
		if (type == Instant.class) {
			return (final ResultSet rs, final Object obj) -> {
				final String tmp = rs.getString(count);
				if (rs.wasNull()) {
					field.set(obj, null);
				} else {
					field.set(obj, Instant.parse(tmp));
				}
			};
		}
		if (type == LocalDate.class) {
			return (final ResultSet rs, final Object obj) -> {
				final java.sql.Date tmp = rs.getDate(count);
				if (rs.wasNull()) {
					field.set(obj, null);
				} else {
					field.set(obj, tmp.toLocalDate());
				}
			};
		}
		if (type == LocalTime.class) {
			return (final ResultSet rs, final Object obj) -> {
				final java.sql.Time tmp = rs.getTime(count);
				if (rs.wasNull()) {
					field.set(obj, null);
				} else {
					field.set(obj, tmp.toLocalTime());
				}
			};
		}
		if (type == String.class) {
			return (final ResultSet rs, final Object obj) -> {
				final String tmp = rs.getString(count);
				if (rs.wasNull()) {
					field.set(obj, null);
				} else {
					field.set(obj, tmp);
				}
			};
		}
		if (type.isEnum()) {
			return (final ResultSet rs, final Object obj) -> {
				final String tmp = rs.getString(count);
				if (rs.wasNull()) {
					field.set(obj, null);
				} else {
					boolean find = false;
					final Object[] arr = type.getEnumConstants();
					for (final Object elem : arr) {
						if (elem.toString().equals(tmp)) {
							field.set(obj, elem);
							find = true;
							break;
						}
					}
					if (!find) {
						throw new DataAccessException("Enum value does not exist in the Model: '" + tmp + "'");
					}
				}
			};
		}
		throw new DataAccessException("Unknown Field Type");

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

	// TODO: manage insert batch...
	public static <T> List<T> insertMultiple(final List<T> data, final QueryOption... options) throws Exception {
		final List<T> out = new ArrayList<>();
		for (final T elem : data) {
			final T tmp = insert(elem, options);
			out.add(tmp);
		}
		return out;
	}

	public static <T> T insert(final T data, final QueryOption... option) throws Exception {
		final Class<?> clazz = data.getClass();
		final QueryOptions options = new QueryOptions(option);

		// External checker of data:
		final List<CheckFunction> checks = options.get(CheckFunction.class);
		for (final CheckFunction check : checks) {
			check.getChecker().check("", data, AnnotationTools.getFieldsNames(clazz), options);
		}

		final DBEntry entry = DBInterfaceOption.getAutoEntry(options);
		final List<Field> asyncFieldUpdate = new ArrayList<>();
		Long uniqueSQLID = null;
		UUID uniqueSQLUUID = null;
		final String tableName = AnnotationTools.getTableName(clazz, options);
		Field primaryKeyField = null;
		boolean generateUUID = false;
		// real add in the BDD:
		try {
			// boolean createIfNotExist = clazz.getDeclaredAnnotationsByType(SQLIfNotExists.class).length != 0;
			final StringBuilder query = new StringBuilder();
			query.append("INSERT INTO `");
			query.append(tableName);
			query.append("` (");

			boolean firstField = true;
			int count = 0;
			for (final Field field : clazz.getFields()) {
				// static field is only for internal global declaration ==> remove it ..
				if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
					continue;
				}
				if (AnnotationTools.isPrimaryKey(field)) {
					primaryKeyField = field;
					if (primaryKeyField.getType() != UUID.class) {
						break;
					}
					generateUUID = true;
					count++;
					final String name = AnnotationTools.getFieldName(field);
					if (firstField) {
						firstField = false;
					} else {
						query.append(",");
					}
					query.append(" `");
					query.append(name);
					query.append("`");
					break;
				}
			}
			for (final Field field : clazz.getFields()) {
				// static field is only for internal global declaration ==> remove it ..
				if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
					continue;
				}
				if (AnnotationTools.isPrimaryKey(field)) {
					continue;
				}
				final DataAccessAddOn addOn = findAddOnforField(field);
				if (addOn != null && !addOn.canInsert(field)) {
					if (addOn.isInsertAsync(field)) {
						asyncFieldUpdate.add(field);
					}
					continue;
				}
				final boolean createTime = field.getDeclaredAnnotationsByType(CreationTimestamp.class).length != 0;
				if (createTime) {
					continue;
				}
				final boolean updateTime = field.getDeclaredAnnotationsByType(UpdateTimestamp.class).length != 0;
				if (updateTime) {
					continue;
				}
				if (!field.getClass().isPrimitive()) {
					final Object tmp = field.get(data);
					if (tmp == null && field.getDeclaredAnnotationsByType(DefaultValue.class).length != 0) {
						continue;
					}
				}
				count++;
				final String name = AnnotationTools.getFieldName(field);
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
			final List<OrderBy> orders = options.get(OrderBy.class);
			for (final OrderBy order : orders) {
				order.generateQuery(query, tableName);
			}
			LOGGER.warn("generate the query: '{}'", query.toString());
			// prepare the request:
			final PreparedStatement ps = entry.connection.prepareStatement(query.toString(),
					Statement.RETURN_GENERATED_KEYS);

			final CountInOut iii = new CountInOut(1);
			UUID uuid = null;
			if (generateUUID) {
				firstField = false;
				// uuid = UUID.randomUUID();
				uuid = UuidUtils.nextUUID();
				addElement(ps, uuid, iii);
				iii.inc();
			}
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
				if (addOn != null) {
					// Add-on specific insertion.
					addOn.insertData(ps, elem, data, iii);
				} else {
					// Generic class insertion...
					final Class<?> type = elem.getType();
					if (!type.isPrimitive()) {
						final Object tmp = elem.get(data);
						if (tmp == null && elem.getDeclaredAnnotationsByType(DefaultValue.class).length != 0) {
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
			// Retrieve uid inserted
			if (generateUUID) {
				// we generate the UUID, otherwise we can not retrieve it
				uniqueSQLUUID = uuid;
			} else {
				try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
					if (generatedKeys.next()) {
						if (primaryKeyField.getType() == UUID.class) {
							// uniqueSQLUUID = generatedKeys.getObject(1, UUID.class);
							/* final Object obj = generatedKeys.getObject(1); final BigInteger bigint = (BigInteger) generatedKeys.getObject(1); uniqueSQLUUID = UuidUtils.asUuid(bigint); final UUID
							 * generatedUUID = (UUID) generatedKeys.getObject(1); System.out.println("UUID généré: " + generatedUUID); */
							final Object obj = generatedKeys.getObject(1);
							final byte[] tmpid = generatedKeys.getBytes(1);
							uniqueSQLUUID = UuidUtils.asUuid(tmpid);
						} else {
							uniqueSQLID = generatedKeys.getLong(1);
						}

					} else {
						throw new SQLException("Creating node failed, no ID obtained (1).");
					}
				} catch (final Exception ex) {
					LOGGER.error("Can not get the UID key inserted ... ");
					ex.printStackTrace();
					throw new SQLException("Creating node failed, no ID obtained (2).");
				}
			}
			if (primaryKeyField != null) {
				if (primaryKeyField.getType() == Long.class) {
					primaryKeyField.set(data, uniqueSQLID);
				} else if (primaryKeyField.getType() == long.class) {
					primaryKeyField.setLong(data, uniqueSQLID);
				} else if (primaryKeyField.getType() == UUID.class) {
					primaryKeyField.set(data, uniqueSQLUUID);
				} else {
					LOGGER.error("Can not manage the primary filed !!!");
				}
			}
			// ps.execute();
		} catch (final SQLException ex) {
			LOGGER.error("Fail SQL request: {}", ex.getMessage());
			ex.printStackTrace();
			throw new DataAccessException("Fail to Insert data in DB : " + ex.getMessage());
		} finally {
			entry.close();
		}
		final List<LazyGetter> asyncActions = new ArrayList<>();
		for (final Field field : asyncFieldUpdate) {
			final DataAccessAddOn addOn = findAddOnforField(field);
			if (uniqueSQLID != null) {
				addOn.asyncInsert(tableName, uniqueSQLID, field, field.get(data), asyncActions);
			} else if (uniqueSQLUUID != null) {
				addOn.asyncInsert(tableName, uniqueSQLUUID, field, field.get(data), asyncActions);
			}
		}
		for (final LazyGetter action : asyncActions) {
			action.doRequest();
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

	public static <ID_TYPE> QueryCondition getTableIdCondition(final Class<?> clazz, final ID_TYPE idKey)
			throws Exception {
		// Find the ID field type ....
		final Field idField = AnnotationTools.getIdField(clazz);
		if (idField == null) {
			throw new DataAccessException(
					"The class have no annotation @Id ==> can not determine the default type searching");
		}
		// check the compatibility of the id and the declared ID
		final Class<?> typeClass = idField.getType();
		if (idKey == null) {
			throw new DataAccessException("Try to identify the ID type and object wa null.");
		}
		if (idKey.getClass() != typeClass) {
			if (idKey.getClass() == Condition.class) {
				throw new DataAccessException(
						"Try to identify the ID type on a condition 'close' internal API error use xxxWhere(...) instead.");
			}
			throw new DataAccessException("Request update with the wrong type ...");
		}
		return new QueryCondition(AnnotationTools.getFieldName(idField), "=", idKey);
	}

	/** Update an object with the inserted json data
	 *
	 * @param <T> Type of the object to insert
	 * @param <ID_TYPE> Master key on the object manage with @Id
	 * @param clazz Class reference of the insertion model
	 * @param id Key to insert data
	 * @param jsonData Json data (partial) values to update
	 * @return the number of object updated
	 * @throws Exception */
	public static <T, ID_TYPE> int updateWithJson(
			final Class<T> clazz,
			final ID_TYPE id,
			final String jsonData,
			final QueryOption... option) throws Exception {
		final QueryOptions options = new QueryOptions(option);
		options.add(new Condition(getTableIdCondition(clazz, id)));
		options.add(new TransmitKey(id));
		return updateWhereWithJson(clazz, jsonData, options.getAllArray());
	}

	public static <T> int updateWhereWithJson(final Class<T> clazz, final String jsonData, final QueryOption... option)
			throws Exception {
		final QueryOptions options = new QueryOptions(option);
		if (options.get(Condition.class).size() == 0) {
			throw new DataAccessException("request a updateWhereWithJson without any condition");
		}
		final ObjectMapper mapper = new ObjectMapper();
		// parse the object to be sure the data are valid:
		final T data = mapper.readValue(jsonData, clazz);
		// Read the tree to filter injection of data:
		final JsonNode root = mapper.readTree(jsonData);
		final List<String> keys = new ArrayList<>();
		final var iterator = root.fieldNames();
		iterator.forEachRemaining(e -> keys.add(e));
		options.add(new FilterValue(keys));
		return updateWhere(data, options.getAllArray());
	}

	public static <T, ID_TYPE> int update(final T data, final ID_TYPE id) throws Exception {
		return update(data, id, AnnotationTools.getFieldsNames(data.getClass()));
	}

	/** @param <T>
	 * @param data
	 * @param id
	 * @param filterValue
	 * @return the affected rows.
	 * @throws Exception */
	public static <T, ID_TYPE> int update(
			final T data,
			final ID_TYPE id,
			final List<String> updateColomn,
			final QueryOption... option) throws Exception {
		final QueryOptions options = new QueryOptions(option);
		options.add(new Condition(getTableIdCondition(data.getClass(), id)));
		options.add(new FilterValue(updateColomn));
		options.add(new TransmitKey(id));
		return updateWhere(data, options);
	}

	public static <T> int updateWhere(final T data, final QueryOption... option) throws Exception {
		final QueryOptions options = new QueryOptions(option);
		return updateWhere(data, options);
	}

	public static <T> int updateWhere(final T data, final QueryOptions options) throws Exception {
		final Class<?> clazz = data.getClass();
		final Condition condition = conditionFusionOrEmpty(options, true);
		final List<FilterValue> filters = options.get(FilterValue.class);
		if (filters.size() != 1) {
			throw new DataAccessException("request a gets without/or with more 1 filter of values");
		}
		final FilterValue filter = filters.get(0);
		// External checker of data:
		if (options != null) {
			final List<CheckFunction> checks = options.get(CheckFunction.class);
			for (final CheckFunction check : checks) {
				check.getChecker().check("", data, filter.getValues(), options);
			}
		}
		final List<LazyGetter> asyncActions = new ArrayList<>();
		final DBEntry entry = DBInterfaceOption.getAutoEntry(options);
		// real add in the BDD:
		try (entry) {
			final String tableName = AnnotationTools.getTableName(clazz, options);
			// boolean createIfNotExist = clazz.getDeclaredAnnotationsByType(SQLIfNotExists.class).length != 0;
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
				if (!filter.getValues().contains(name)) {
					continue;
				} else if (AnnotationTools.isGenericField(field)) {
					continue;
				}
				final DataAccessAddOn addOn = findAddOnforField(field);
				if (addOn != null && !addOn.canInsert(field)) {
					if (addOn.isInsertAsync(field)) {
						final List<TransmitKey> transmitKey = options.get(TransmitKey.class);
						if (transmitKey.size() != 1) {
							throw new DataAccessException(
									"Fail to transmit Key to update the async update... (must have only 1)");
						}
						addOn.asyncUpdate(tableName, transmitKey.get(0).getKey(), field, field.get(data), asyncActions);
					}
					continue;
				}
				if (!field.getClass().isPrimitive()) {
					final Object tmp = field.get(data);
					if (tmp == null && field.getDeclaredAnnotationsByType(DefaultValue.class).length != 0) {
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
			final List<OrderBy> orders = options.get(OrderBy.class);
			for (final OrderBy order : orders) {
				order.generateQuery(query, tableName);
			}
			query.append(" ");
			final String deletedFieldName = AnnotationTools.getDeletedFieldName(clazz);
			condition.whereAppendQuery(query, tableName, null, deletedFieldName);

			// If the first field is not set, then nothing to update n the main base:
			if (!firstField) {
				LOGGER.debug("generate update query: '{}'", query.toString());
				// prepare the request:
				final PreparedStatement ps = entry.connection.prepareStatement(query.toString(),
						Statement.RETURN_GENERATED_KEYS);
				final CountInOut iii = new CountInOut(1);
				for (final Field field : clazz.getFields()) {
					// static field is only for internal global declaration ==> remove it ..
					if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
						continue;
					}
					final String name = AnnotationTools.getFieldName(field);
					if (!filter.getValues().contains(name)) {
						continue;
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
							if (tmp == null && field.getDeclaredAnnotationsByType(DefaultValue.class).length != 0) {
								continue;
							}
						}
						setValuedb(type, data, iii, field, ps);
					} else {
						addOn.insertData(ps, field, data, iii);
					}
				}
				condition.injectQuery(ps, iii);
				return ps.executeUpdate();
			}
		} catch (final SQLException ex) {
			ex.printStackTrace();
		}
		for (final LazyGetter action : asyncActions) {
			action.doRequest();
		}
		return 0;
	}

	public static void addElement(final PreparedStatement ps, final Object value, final CountInOut iii)
			throws Exception {
		if (value instanceof final UUID tmp) {
			final byte[] dataByte = UuidUtils.asBytes(tmp);
			ps.setBytes(iii.value, dataByte);
		} else if (value instanceof final Long tmp) {
			LOGGER.debug("Inject Long => {}", tmp);
			ps.setLong(iii.value, tmp);
		} else if (value instanceof final Integer tmp) {
			LOGGER.debug("Inject Integer => {}", tmp);
			ps.setInt(iii.value, tmp);
		} else if (value instanceof final String tmp) {
			LOGGER.debug("Inject String => {}", tmp);
			ps.setString(iii.value, tmp);
		} else if (value instanceof final Short tmp) {
			LOGGER.debug("Inject Short => {}", tmp);
			ps.setShort(iii.value, tmp);
		} else if (value instanceof final Byte tmp) {
			LOGGER.debug("Inject Byte => {}", tmp);
			ps.setByte(iii.value, tmp);
		} else if (value instanceof final Float tmp) {
			LOGGER.debug("Inject Float => {}", tmp);
			ps.setFloat(iii.value, tmp);
		} else if (value instanceof final Double tmp) {
			LOGGER.debug("Inject Double => {}", tmp);
			ps.setDouble(iii.value, tmp);
		} else if (value instanceof final Boolean tmp) {
			LOGGER.debug("Inject Boolean => {}", tmp);
			ps.setBoolean(iii.value, tmp);
		} else if (value instanceof final Timestamp tmp) {
			LOGGER.debug("Inject Timestamp => {}", tmp);
			ps.setTimestamp(iii.value, tmp);
		} else if (value instanceof final Date tmp) {
			LOGGER.debug("Inject Date => {}", tmp);
			ps.setTimestamp(iii.value, java.sql.Timestamp.from((tmp).toInstant()));
		} else if (value instanceof final LocalDate tmp) {
			LOGGER.debug("Inject LocalDate => {}", tmp);
			ps.setDate(iii.value, java.sql.Date.valueOf(tmp));
		} else if (value instanceof final LocalTime tmp) {
			LOGGER.debug("Inject LocalTime => {}", tmp);
			ps.setTime(iii.value, java.sql.Time.valueOf(tmp));
		} else if (value.getClass().isEnum()) {
			LOGGER.debug("Inject ENUM => {}", value.toString());
			ps.setString(iii.value, value.toString());
		} else {
			throw new DataAccessException("Not manage type ==> need to add it ...");
		}
	}

	public static int executeSimpleQuery(final String query, final QueryOption... option)
			throws SQLException, IOException {
		final QueryOptions options = new QueryOptions(option);
		final DBEntry entry = DBInterfaceOption.getAutoEntry(options);
		final Statement stmt = entry.connection.createStatement();
		return stmt.executeUpdate(query);
	}

	public static boolean executeQuery(final String query, final QueryOption... option)
			throws SQLException, IOException {
		final QueryOptions options = new QueryOptions(option);
		final DBEntry entry = DBInterfaceOption.getAutoEntry(options);
		final Statement stmt = entry.connection.createStatement();
		return stmt.execute(query);
	}

	public static <T> T getWhere(final Class<T> clazz, final QueryOptions options) throws Exception {
		options.add(new Limit(1));
		final List<T> values = getsWhere(clazz, options);
		if (values.size() == 0) {
			return null;
		}
		return values.get(0);
	}

	public static <T> T getWhere(final Class<T> clazz, final QueryOption... option) throws Exception {
		final QueryOptions options = new QueryOptions(option);
		return getWhere(clazz, options);
	}

	public static void generateSelectField(//
			final StringBuilder querySelect, //
			final StringBuilder query, //
			final Class<?> clazz, //
			final QueryOptions options, //
			final CountInOut count//
	) throws Exception {
		final boolean readAllfields = QueryOptions.readAllColomn(options);
		final String tableName = AnnotationTools.getTableName(clazz, options);
		final String primaryKey = AnnotationTools.getPrimaryKeyField(clazz).getName();
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
				addOn.generateQuery(tableName, primaryKey, elem, querySelect, query, name, count, options);
			} else {
				querySelect.append(tableName);
				querySelect.append(".");
				querySelect.append(name);
				count.inc();
			}
		}
	}

	public static <T> List<T> getsWhere(final Class<T> clazz, final QueryOption... option) throws Exception {
		final QueryOptions options = new QueryOptions(option);
		return getsWhere(clazz, options);
	}

	public static Condition conditionFusionOrEmpty(final QueryOptions options, final boolean throwIfEmpty)
			throws DataAccessException {
		final List<Condition> conditions = options.get(Condition.class);
		if (conditions.size() == 0) {
			if (throwIfEmpty) {
				throw new DataAccessException("request a gets without any condition");
			} else {
				return new Condition();
			}
		}
		Condition condition = null;
		if (conditions.size() == 1) {
			condition = conditions.get(0);
		} else {
			final QueryAnd andCondition = new QueryAnd();
			for (final Condition cond : conditions) {
				andCondition.add(cond.condition);
			}
			condition = new Condition(andCondition);
		}
		return condition;
	}

	@SuppressWarnings("unchecked")
	public static <T> List<T> getsWhere(final Class<T> clazz, final QueryOptions options) throws Exception {
		final Condition condition = conditionFusionOrEmpty(options, false);
		final List<LazyGetter> lazyCall = new ArrayList<>();
		final String deletedFieldName = AnnotationTools.getDeletedFieldName(clazz);
		final DBEntry entry = DBInterfaceOption.getAutoEntry(options);
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
			condition.whereAppendQuery(query, tableName, options, deletedFieldName);
			final List<GroupBy> groups = options.get(GroupBy.class);
			for (final GroupBy group : groups) {
				group.generateQuery(query, tableName);
			}
			final List<OrderBy> orders = options.get(OrderBy.class);
			for (final OrderBy order : orders) {
				order.generateQuery(query, tableName);
			}
			final List<Limit> limits = options.get(Limit.class);
			if (limits.size() == 1) {
				limits.get(0).generateQuery(query, tableName);
			} else if (limits.size() > 1) {
				throw new DataAccessException("Request with multiple 'limit'...");
			}
			LOGGER.warn("generate the query: '{}'", query.toString());
			// prepare the request:
			final PreparedStatement ps = entry.connection.prepareStatement(query.toString(),
					Statement.RETURN_GENERATED_KEYS);
			final CountInOut iii = new CountInOut(1);
			condition.injectQuery(ps, iii);
			if (limits.size() == 1) {
				limits.get(0).injectQuery(ps, iii);
			}
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
		}
		return outs;
	}

	public static Object createObjectFromSQLRequest(
			final ResultSet rs,
			final Class<?> clazz,
			final CountInOut count,
			final CountInOut countNotNull,
			final QueryOptions options,
			final List<LazyGetter> lazyCall) throws Exception {
		final boolean readAllfields = QueryOptions.readAllColomn(options);
		// TODO: manage class that is defined inside a class ==> Not manage for now...
		Object data = null;
		for (final Constructor<?> contructor : clazz.getConstructors()) {
			if (contructor.getParameterCount() == 0) {
				data = contructor.newInstance();
			}
		}
		if (data == null) {
			throw new DataAccessException(
					"Can not find the default constructor for the class: " + clazz.getCanonicalName());
		}
		for (final Field elem : clazz.getFields()) {
			// static field is only for internal global declaration ==> remove it ..
			if (java.lang.reflect.Modifier.isStatic(elem.getModifiers())) {
				continue;
			}
			final DataAccessAddOn addOn = findAddOnforField(elem);
			if (addOn != null && !addOn.canRetrieve(elem)) {
				continue;
			}
			final boolean notRead = AnnotationTools.isdefaultNotRead(elem);
			if (!readAllfields && notRead) {
				continue;
			}
			if (addOn != null) {
				addOn.fillFromQuery(rs, elem, data, count, options, lazyCall);
			} else {
				setValueFromDb(elem.getType(), data, count, elem, rs, countNotNull);
			}
		}
		return data;
	}

	public static <ID_TYPE> long count(final Class<?> clazz, final ID_TYPE id, final QueryOption... option)
			throws Exception {
		final QueryOptions options = new QueryOptions(option);
		options.add(new Condition(getTableIdCondition(clazz, id)));
		return DataAccess.countWhere(clazz, options);
	}

	public static long countWhere(final Class<?> clazz, final QueryOption... option) throws Exception {
		final QueryOptions options = new QueryOptions(option);
		return countWhere(clazz, options);
	}

	public static long countWhere(final Class<?> clazz, final QueryOptions options) throws Exception {
		final Condition condition = conditionFusionOrEmpty(options, false);
		final String deletedFieldName = AnnotationTools.getDeletedFieldName(clazz);
		DBEntry entry = DBInterfaceOption.getAutoEntry(options);
		long count = 0;
		// real add in the BDD:
		try {
			final StringBuilder query = new StringBuilder();
			final String tableName = AnnotationTools.getTableName(clazz, options);
			query.append("SELECT COUNT(*) AS count FROM `");
			query.append(tableName);
			query.append("` ");
			condition.whereAppendQuery(query, tableName, options, deletedFieldName);
			final List<Limit> limits = options.get(Limit.class);
			if (limits.size() == 1) {
				limits.get(0).generateQuery(query, tableName);
			} else if (limits.size() > 1) {
				throw new DataAccessException("Request with multiple 'limit'...");
			}
			LOGGER.warn("generate the query: '{}'", query.toString());
			// prepare the request:
			final PreparedStatement ps = entry.connection.prepareStatement(query.toString(),
					Statement.RETURN_GENERATED_KEYS);
			final CountInOut iii = new CountInOut(1);
			condition.injectQuery(ps, iii);
			if (limits.size() == 1) {
				limits.get(0).injectQuery(ps, iii);
			}
			// execute the request
			final ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				count = rs.getLong("count");
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
		return count;
	}

	public static <T, ID_TYPE> T get(final Class<T> clazz, final ID_TYPE id, final QueryOption... option)
			throws Exception {
		final QueryOptions options = new QueryOptions(option);
		options.add(new Condition(getTableIdCondition(clazz, id)));
		return DataAccess.getWhere(clazz, options.getAllArray());
	}

	public static <T> List<T> gets(final Class<T> clazz) throws Exception {
		return getsWhere(clazz);
	}

	public static <T> List<T> gets(final Class<T> clazz, final QueryOption... option) throws Exception {
		return getsWhere(clazz, option);
	}

	public static <ID_TYPE> int delete(final Class<?> clazz, final ID_TYPE id) throws Exception {
		return delete(clazz, id, null);
	}

	/** Delete items with the specific Id (cf @Id) and some options. If the Entity is manage as a softDeleted model, then it is flag as removed (if not already done before).
	 * @param <ID_TYPE> Type of the reference @Id
	 * @param clazz Data model that might remove element
	 * @param id Unique Id of the model
	 * @param options (Optional) Options of the request
	 * @return Number of element that is removed. */
	public static <ID_TYPE> int delete(final Class<?> clazz, final ID_TYPE id, final QueryOption... options)
			throws Exception {
		final String hasDeletedFieldName = AnnotationTools.getDeletedFieldName(clazz);
		if (hasDeletedFieldName != null) {
			return deleteSoft(clazz, id, options);
		} else {
			return deleteHard(clazz, id, options);
		}
	}

	/** Delete items with the specific condition and some options. If the Entity is manage as a softDeleted model, then it is flag as removed (if not already done before).
	 * @param clazz Data model that might remove element.
	 * @param condition Condition to remove elements.
	 * @param options (Optional) Options of the request.
	 * @return Number of element that is removed. */
	public static int deleteWhere(final Class<?> clazz, final QueryOption... option) throws Exception {

		final String hasDeletedFieldName = AnnotationTools.getDeletedFieldName(clazz);
		if (hasDeletedFieldName != null) {
			return deleteSoftWhere(clazz, option);
		} else {
			return deleteHardWhere(clazz, option);
		}
	}

	public static <ID_TYPE> int deleteHard(final Class<?> clazz, final ID_TYPE id, final QueryOption... option)
			throws Exception {
		final QueryOptions options = new QueryOptions(option);
		options.add(new Condition(getTableIdCondition(clazz, id)));
		return deleteHardWhere(clazz, options.getAllArray());
	}

	public static int deleteHardWhere(final Class<?> clazz, final QueryOption... option) throws Exception {
		final QueryOptions options = new QueryOptions(option);
		final Condition condition = conditionFusionOrEmpty(options, true);
		final String tableName = AnnotationTools.getTableName(clazz, options);
		final String deletedFieldName = AnnotationTools.getDeletedFieldName(clazz);
		// find the deleted field
		final DBEntry entry = DBInterfaceOption.getAutoEntry(options);
		final StringBuilder query = new StringBuilder();
		query.append("DELETE FROM `");
		query.append(tableName);
		query.append("` ");
		condition.whereAppendQuery(query, tableName, null, deletedFieldName);
		try {
			LOGGER.debug("APPLY: {}", query.toString());
			final PreparedStatement ps = entry.connection.prepareStatement(query.toString());
			final CountInOut iii = new CountInOut(1);
			condition.injectQuery(ps, iii);
			return ps.executeUpdate();
		} finally {
			entry.close();
		}
	}

	private static <ID_TYPE> int deleteSoft(final Class<?> clazz, final ID_TYPE id, final QueryOption... option)
			throws Exception {
		final QueryOptions options = new QueryOptions(option);
		options.add(new Condition(getTableIdCondition(clazz, id)));
		return deleteSoftWhere(clazz, options.getAllArray());
	}

	public static int deleteSoftWhere(final Class<?> clazz, final QueryOption... option) throws Exception {
		final QueryOptions options = new QueryOptions(option);
		final Condition condition = conditionFusionOrEmpty(options, true);
		final String tableName = AnnotationTools.getTableName(clazz, options);
		final String deletedFieldName = AnnotationTools.getDeletedFieldName(clazz);
		/* String updateFieldName = null; if ("sqlite".equalsIgnoreCase(ConfigBaseVariable.getDBType())) { updateFieldName = AnnotationTools.getUpdatedFieldName(clazz); } */
		// find the deleted field
		final DBEntry entry = DBInterfaceOption.getAutoEntry(options);
		final StringBuilder query = new StringBuilder();
		query.append("UPDATE `");
		query.append(tableName);
		query.append("` SET `");
		query.append(deletedFieldName);
		query.append("`=true ");
		/* The trigger work well, but the timestamp is store @ seconds... if (updateFieldName != null) { // done only in SQLite (the trigger does not work... query.append(", `");
		 * query.append(updateFieldName); query.append("`=DATE()"); } */
		condition.whereAppendQuery(query, tableName, null, deletedFieldName);
		try {
			LOGGER.debug("APPLY UPDATE: {}", query.toString());
			final PreparedStatement ps = entry.connection.prepareStatement(query.toString());
			final CountInOut iii = new CountInOut(1);
			condition.injectQuery(ps, iii);
			return ps.executeUpdate();
		} finally {
			entry.close();
		}
	}

	public static <ID_TYPE> int unsetDelete(final Class<?> clazz, final ID_TYPE id) throws Exception {
		return unsetDeleteWhere(clazz, new Condition(getTableIdCondition(clazz, id)));
	}

	public static <ID_TYPE> int unsetDelete(final Class<?> clazz, final ID_TYPE id, final QueryOption... option)
			throws Exception {
		final QueryOptions options = new QueryOptions(option);
		options.add(new Condition(getTableIdCondition(clazz, id)));
		return unsetDeleteWhere(clazz, options.getAllArray());
	}

	public static int unsetDeleteWhere(final Class<?> clazz, final QueryOption... option) throws Exception {
		final QueryOptions options = new QueryOptions(option);
		final Condition condition = conditionFusionOrEmpty(options, true);
		final String tableName = AnnotationTools.getTableName(clazz, options);
		final String deletedFieldName = AnnotationTools.getDeletedFieldName(clazz);
		if (deletedFieldName == null) {
			throw new DataAccessException("The class " + clazz.getCanonicalName() + " has no deleted field");
		}
		final DBEntry entry = DBInterfaceOption.getAutoEntry(options);
		final StringBuilder query = new StringBuilder();
		query.append("UPDATE `");
		query.append(tableName);
		query.append("` SET `");
		query.append(deletedFieldName);
		query.append("`=false ");
		// need to disable the deleted false because the model must be unselected to be updated.
		options.add(QueryOptions.ACCESS_DELETED_ITEMS);
		condition.whereAppendQuery(query, tableName, options, deletedFieldName);
		try {
			final PreparedStatement ps = entry.connection.prepareStatement(query.toString());
			final CountInOut iii = new CountInOut(1);
			condition.injectQuery(ps, iii);
			return ps.executeUpdate();
		} finally {
			entry.close();
		}
	}

	public static void drop(final Class<?> clazz, final QueryOption... option) throws Exception {
		final QueryOptions options = new QueryOptions(option);
		final String tableName = AnnotationTools.getTableName(clazz, options);
		final DBEntry entry = DBInterfaceOption.getAutoEntry(options);
		final StringBuilder query = new StringBuilder();
		query.append("DROP TABLE IF EXISTS `");
		query.append(tableName);
		query.append("`");
		try {
			LOGGER.trace("Execute Query: {}", query.toString());
			// Remove main table
			final PreparedStatement ps = entry.connection.prepareStatement(query.toString());
			ps.executeUpdate();
			// search subTable:
			for (final Field field : clazz.getFields()) {
				// static field is only for internal global declaration ==> remove it ..
				if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
					continue;
				}
				if (AnnotationTools.isGenericField(field)) {
					continue;
				}
				final DataAccessAddOn addOn = findAddOnforField(field);
				if (addOn != null && !addOn.canInsert(field)) {
					addOn.drop(tableName, field);
				}
			}
		} finally {
			entry.close();
		}
	}

	public static void cleanAll(final Class<?> clazz, final QueryOption... option) throws Exception {
		final QueryOptions options = new QueryOptions(option);
		final String tableName = AnnotationTools.getTableName(clazz, options);
		DBEntry entry = DBInterfaceOption.getAutoEntry(options);
		final StringBuilder query = new StringBuilder();
		query.append("DELETE FROM `");
		query.append(tableName);
		query.append("`");
		try {
			LOGGER.trace("Execute Query: {}", query.toString());
			// Remove main table
			final PreparedStatement ps = entry.connection.prepareStatement(query.toString());
			ps.executeUpdate();
			// search subTable:
			for (final Field field : clazz.getFields()) {
				// static field is only for internal global declaration ==> remove it ..
				if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
					continue;
				}
				if (AnnotationTools.isGenericField(field)) {
					continue;
				}
				final DataAccessAddOn addOn = findAddOnforField(field);
				if (addOn != null && !addOn.canInsert(field)) {
					addOn.cleanAll(tableName, field);
				}
			}
		} finally {
			entry.close();
			entry = null;
		}
	}

	/** Execute a simple query with external property.
	 * @param <TYPE> Type of the data generate.
	 * @param clazz Class that might be analyze.
	 * @param query Base of the query.
	 * @param parameters "?" parameter of the query.
	 * @param option Optional parameters
	 * @return The list of element requested
	 * @throws Exception */
	public static <TYPE> List<TYPE> query(
			final Class<TYPE> clazz,
			final String query,
			final List<Object> parameters,
			final QueryOption... option) throws Exception {
		final QueryOptions options = new QueryOptions(option);
		return query(clazz, query, parameters, options);
	}

	public static <TYPE> List<TYPE> query(
			final Class<TYPE> clazz,
			final String queryBase,
			final List<Object> parameters,
			final QueryOptions options) throws Exception {
		final List<LazyGetter> lazyCall = new ArrayList<>();
		// TODO ... final String deletedFieldName = AnnotationTools.getDeletedFieldName(clazz);
		final DBEntry entry = DBInterfaceOption.getAutoEntry(options);

		final Condition condition = conditionFusionOrEmpty(options, false);
		final StringBuilder query = new StringBuilder(queryBase);
		final List<TYPE> outs = new ArrayList<>();
		// real add in the BDD:
		try {
			final CountInOut count = new CountInOut();
			condition.whereAppendQuery(query, null, options, null);

			final List<GroupBy> groups = options.get(GroupBy.class);
			for (final GroupBy group : groups) {
				group.generateQuery(query, null);
			}
			final List<OrderBy> orders = options.get(OrderBy.class);
			for (final OrderBy order : orders) {
				order.generateQuery(query, null);
			}
			final List<Limit> limits = options.get(Limit.class);
			if (limits.size() == 1) {
				limits.get(0).generateQuery(query, null);
			} else if (limits.size() > 1) {
				throw new DataAccessException("Request with multiple 'limit'...");
			}
			LOGGER.warn("generate the query: '{}'", query.toString());
			// prepare the request:
			final PreparedStatement ps = entry.connection.prepareStatement(query.toString(),
					Statement.RETURN_GENERATED_KEYS);
			final CountInOut iii = new CountInOut(1);
			if (parameters != null) {
				for (final Object elem : parameters) {
					DataAccess.addElement(ps, elem, iii);
				}
				iii.inc();
			}
			condition.injectQuery(ps, iii);
			if (limits.size() == 1) {
				limits.get(0).injectQuery(ps, iii);
			}
			// execute the request
			final ResultSet rs = ps.executeQuery();
			final ResultSetMetaData rsmd = rs.getMetaData();
			final List<RetreiveFromDB> actionToRetreive = new ArrayList<>();
			LOGGER.info("Field:");
			for (int jjj = 0; jjj < rsmd.getColumnCount(); jjj++) {
				final String label = rsmd.getColumnLabel(jjj + 1);
				LOGGER.info("    - {}:{}", jjj, label);
				// find field name ...
				final Field field = AnnotationTools.getFieldNamed(clazz, label);
				if (field == null) {
					throw new DataAccessException("Query with unknown field: '" + label + "'");
				}
				// create the callback...
				final RetreiveFromDB element = createSetValueFromDbCallback(jjj + 1, field);
				actionToRetreive.add(element);
			}

			while (rs.next()) {
				count.value = 1;
				Object data = null;
				for (final Constructor<?> contructor : clazz.getConstructors()) {
					if (contructor.getParameterCount() == 0) {
						data = contructor.newInstance();
					}
				}
				if (data == null) {
					// TODO...
				} else {
					for (final RetreiveFromDB action : actionToRetreive) {
						action.doRequest(rs, data);
					}
				}
				@SuppressWarnings("unchecked")
				final TYPE out = (TYPE) data;
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
		}
		return outs;
	}
}
