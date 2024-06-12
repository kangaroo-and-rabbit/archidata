package org.kar.archidata.dataAccess.addOn;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.kar.archidata.annotation.AnnotationTools;
import org.kar.archidata.annotation.DataJson;
import org.kar.archidata.dataAccess.CountInOut;
import org.kar.archidata.dataAccess.DataAccess;
import org.kar.archidata.dataAccess.DataAccessAddOn;
import org.kar.archidata.dataAccess.DataFactory;
import org.kar.archidata.dataAccess.LazyGetter;
import org.kar.archidata.dataAccess.QueryOptions;
import org.kar.archidata.dataAccess.addOn.model.TableCoversLongLong;
import org.kar.archidata.dataAccess.addOn.model.TableCoversLongUUID;
import org.kar.archidata.dataAccess.addOn.model.TableCoversUUIDLong;
import org.kar.archidata.dataAccess.addOn.model.TableCoversUUIDUUID;
import org.kar.archidata.dataAccess.options.OverrideTableName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.constraints.NotNull;

public class AddOnDataJson implements DataAccessAddOn {
	static final Logger LOGGER = LoggerFactory.getLogger(AddOnDataJson.class);

	@Override
	public Class<?> getAnnotationClass() {
		return DataJson.class;
	}

	@Override
	public String getSQLFieldType(final Field elem) throws Exception {
		final String fieldName = AnnotationTools.getFieldName(elem);
		return DataFactory.convertTypeInSQL(String.class, fieldName);
	}

	@Override
	public boolean isCompatibleField(final Field elem) {
		final DataJson decorators = elem.getDeclaredAnnotation(DataJson.class);
		return decorators != null;
	}

	@Override
	public void insertData(final PreparedStatement ps, final Field field, final Object rootObject, final CountInOut iii)
			throws IllegalArgumentException, IllegalAccessException, SQLException, JsonProcessingException {
		final Object data = field.get(rootObject);
		if (data == null) {
			ps.setNull(iii.value, Types.VARCHAR);
		}
		final ObjectMapper objectMapper = new ObjectMapper();
		final String dataString = objectMapper.writeValueAsString(data);
		ps.setString(iii.value, dataString);
		iii.inc();
	}

	@Override
	public boolean canInsert(final Field field) {
		return true;
	}

	@Override
	public boolean isInsertAsync(final Field field) {
		return false;
	}

	@Override
	public boolean canRetrieve(final Field field) {
		return true;
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
		querySelect.append(" ");
		querySelect.append(tableName);
		querySelect.append(".");
		querySelect.append(name);
		count.inc();
		return;
	}

	@Override
	public void fillFromQuery(
			final ResultSet rs,
			final Field field,
			final Object data,
			final CountInOut count,
			final QueryOptions options,
			final List<LazyGetter> lazyCall) throws Exception {
		final String jsonData = rs.getString(count.value);
		count.inc();
		if (!rs.wasNull()) {
			final ObjectMapper objectMapper = new ObjectMapper();
			if (field.getType() == List.class) {
				final ParameterizedType listType = (ParameterizedType) field.getGenericType();
				final Class<?> listClass = (Class<?>) listType.getActualTypeArguments()[0];
				if (listClass == Long.class) {
					final Object dataParsed = objectMapper.readValue(jsonData, new TypeReference<List<Long>>() {});// field.getType());
					field.set(data, dataParsed);
					return;
				}
				if (listClass == Float.class) {
					final Object dataParsed = objectMapper.readValue(jsonData, new TypeReference<List<Float>>() {});// field.getType());
					field.set(data, dataParsed);
					return;
				}
				if (listClass == Double.class) {
					final Object dataParsed = objectMapper.readValue(jsonData, new TypeReference<List<Double>>() {});// field.getType());
					field.set(data, dataParsed);
					return;
				}
				if (listClass == Integer.class) {
					final Object dataParsed = objectMapper.readValue(jsonData, new TypeReference<List<Integer>>() {});// field.getType());
					field.set(data, dataParsed);
					return;
				}
				if (listClass == Short.class) {
					final Object dataParsed = objectMapper.readValue(jsonData, new TypeReference<List<Short>>() {});// field.getType());
					field.set(data, dataParsed);
					return;
				}
				if (listClass == String.class) {
					final Object dataParsed = objectMapper.readValue(jsonData, new TypeReference<List<String>>() {});// field.getType());
					field.set(data, dataParsed);
					return;
				}
				if (listClass == UUID.class) {
					final Object dataParsed = objectMapper.readValue(jsonData, new TypeReference<List<UUID>>() {});// field.getType());
					field.set(data, dataParsed);
					return;
				}
				LOGGER.warn("Maybe fail to translate Model in datajson list: List<{}>", listClass.getCanonicalName());
			}
			final Object dataParsed = objectMapper.readValue(jsonData, field.getType());
			field.set(data, dataParsed);
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
			final int fieldId) throws Exception {
		DataFactory.createTablesSpecificType(tableName, primaryField, field, mainTableBuilder, preActionList,
				postActionList, createIfNotExist, createDrop, fieldId, JsonValue.class);
	}

	public static void addLink(final Class<?> clazz, final Long id, final String column, final Long remoteKey)
			throws Exception {
		final String tableName = AnnotationTools.getTableName(clazz);
		final TableCoversLongLong data = DataAccess.get(TableCoversLongLong.class, id,
				new OverrideTableName(tableName));
		if (data.covers == null) {
			data.covers = new ArrayList<>();
		}
		for (final Long elem : data.covers) {
			if (elem.equals(remoteKey)) {
				return;
			}
		}
		data.covers.add(remoteKey);
		DataAccess.update(data, data.id, List.of("covers"), new OverrideTableName(tableName));
	}

	/**
	 * Adds a remoteKey to the covers list of a data entry identified by the given class type and ID.
	 * If the covers list is null, it initializes it. If the remoteKey already exists in the list,
	 * the method returns without making any changes.
	 *
	 * @param clazz     The class type to retrieve the table name from.
	 * @param id        The ID of the data object to fetch.
	 * @param column    The name of the column (currently not used, but may be used for specifying a field name).
	 * @param remoteKey The UUID to add to the covers list.
	 * @throws Exception If an error occurs during data retrieval or update.
	 */
	public static void addLink(final Class<?> clazz, final Long id, final String column, final UUID remoteKey)
			throws Exception {
		final String tableName = AnnotationTools.getTableName(clazz);
		// TODO: Get primary key name
		final TableCoversLongUUID data = DataAccess.get(TableCoversLongUUID.class, id,
				new OverrideTableName(tableName));
		if (data.covers == null) {
			data.covers = new ArrayList<>();
		}
		for (final UUID elem : data.covers) {
			if (elem.equals(remoteKey)) {
				return;
			}
		}
		data.covers.add(remoteKey);
		DataAccess.update(data, data.id, List.of("covers"), new OverrideTableName(tableName));// TODO:  ,new OverrideFieldName("covers", column));
	}

	/**
	 * Adds a remoteKey to the covers list of a data entry identified by the given class type and ID.
	 * If the covers list is null, it initializes it. If the remoteKey already exists in the list,
	 * the method returns without making any changes.
	 *
	 * @param clazz     The class type to retrieve the table name from.
	 * @param id        The ID of the data object to fetch.
	 * @param column    The name of the column (currently not used, but may be used for specifying a field name).
	 * @param remoteKey The UUID to add to the covers list.
	 * @throws Exception If an error occurs during data retrieval or update.
	 */
	public static void addLink(final Class<?> clazz, final UUID uuid, final String column, final UUID remoteKey)
			throws Exception {
		final String tableName = AnnotationTools.getTableName(clazz);
		final TableCoversUUIDUUID data = DataAccess.get(TableCoversUUIDUUID.class, uuid,
				new OverrideTableName(tableName));
		if (data.covers == null) {
			data.covers = new ArrayList<>();
		}
		for (final UUID elem : data.covers) {
			if (elem.equals(remoteKey)) {
				return;
			}
		}
		data.covers.add(remoteKey);
		DataAccess.update(data, data.id, List.of("covers"), new OverrideTableName(tableName));
	}

	public static void addLink(final Class<?> clazz, final UUID uuid, final String column, final Long remoteKey)
			throws Exception {
		final String tableName = AnnotationTools.getTableName(clazz);
		final TableCoversUUIDLong data = DataAccess.get(TableCoversUUIDLong.class, uuid,
				new OverrideTableName(tableName));
		if (data.covers == null) {
			data.covers = new ArrayList<>();
		}
		for (final Long elem : data.covers) {
			if (elem.equals(remoteKey)) {
				return;
			}
		}
		data.covers.add(remoteKey);
		DataAccess.update(data, data.uuid, List.of("covers"), new OverrideTableName(tableName));
	}

	public static void removeLink(final Class<?> clazz, final UUID uuid, final String column, final Long remoteKey)
			throws Exception {
		final String tableName = AnnotationTools.getTableName(clazz);
		final TableCoversUUIDLong data = DataAccess.get(TableCoversUUIDLong.class, uuid,
				new OverrideTableName(tableName));
		if (data.covers == null) {
			return;
		}
		final List<Long> newList = new ArrayList<>();
		for (final Long elem : data.covers) {
			if (elem.equals(remoteKey)) {
				continue;
			}
			newList.add(elem);
		}
		data.covers = newList;
		DataAccess.update(data, data.uuid, List.of("covers"), new OverrideTableName(tableName));
	}

	public static void removeLink(final Class<?> clazz, final UUID uuid, final String column, final UUID remoteKey)
			throws Exception {
		final String tableName = AnnotationTools.getTableName(clazz);
		final TableCoversUUIDUUID data = DataAccess.get(TableCoversUUIDUUID.class, uuid,
				new OverrideTableName(tableName));
		if (data.covers == null) {
			return;
		}
		final List<UUID> newList = new ArrayList<>();
		for (final UUID elem : data.covers) {
			if (elem.equals(remoteKey)) {
				continue;
			}
			newList.add(elem);
		}
		data.covers = newList;
		DataAccess.update(data, data.id, List.of("covers"), new OverrideTableName(tableName));
	}

	public static void removeLink(final Class<?> clazz, final Long id, final String column, final Long remoteKey)
			throws Exception {
		final String tableName = AnnotationTools.getTableName(clazz);
		final TableCoversLongLong data = DataAccess.get(TableCoversLongLong.class, id,
				new OverrideTableName(tableName));
		if (data.covers == null) {
			return;
		}
		final List<Long> newList = new ArrayList<>();
		for (final Long elem : data.covers) {
			if (elem.equals(remoteKey)) {
				continue;
			}
			newList.add(elem);
		}
		data.covers = newList;
		DataAccess.update(data, data.id, List.of("covers"), new OverrideTableName(tableName));
	}

	public static void removeLink(final Class<?> clazz, final Long id, final String column, final UUID remoteKey)
			throws Exception {
		final String tableName = AnnotationTools.getTableName(clazz);
		final TableCoversLongUUID data = DataAccess.get(TableCoversLongUUID.class, id,
				new OverrideTableName(tableName));
		if (data.covers == null) {
			return;
		}
		final List<UUID> newList = new ArrayList<>();
		for (final UUID elem : data.covers) {
			if (elem.equals(remoteKey)) {
				continue;
			}
			newList.add(elem);
		}
		data.covers = newList;
		DataAccess.update(data, data.id, List.of("covers"), new OverrideTableName(tableName));
	}
}
