package org.kar.archidata.dataAccess.addOnSQL;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bson.types.ObjectId;
import org.kar.archidata.annotation.AnnotationTools;
import org.kar.archidata.annotation.AnnotationTools.FieldName;
import org.kar.archidata.annotation.DataJson;
import org.kar.archidata.dataAccess.CountInOut;
import org.kar.archidata.dataAccess.DBAccess;
import org.kar.archidata.dataAccess.DBAccessMorphia;
import org.kar.archidata.dataAccess.DBAccessSQL;
import org.kar.archidata.dataAccess.DataFactory;
import org.kar.archidata.dataAccess.LazyGetter;
import org.kar.archidata.dataAccess.QueryOptions;
import org.kar.archidata.dataAccess.addOnSQL.model.TableCoversGeneric;
import org.kar.archidata.dataAccess.options.OptionRenameColumn;
import org.kar.archidata.dataAccess.options.OptionSpecifyType;
import org.kar.archidata.dataAccess.options.OverrideTableName;
import org.kar.archidata.exception.DataAccessException;
import org.kar.archidata.tools.ContextGenericTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

import jakarta.validation.constraints.NotNull;

public class AddOnDataJson implements DataAccessAddOn {
	static final Logger LOGGER = LoggerFactory.getLogger(AddOnDataJson.class);

	@Override
	public Class<?> getAnnotationClass() {
		return DataJson.class;
	}

	@Override
	public String getSQLFieldType(final Field elem, final QueryOptions options) throws DataAccessException {
		final FieldName fieldName = AnnotationTools.getFieldName(elem, options);
		return DataFactory.convertTypeInSQL(String.class, fieldName.inTable());
	}

	@Override
	public boolean isCompatibleField(final Field elem) {
		final DataJson decorators = elem.getDeclaredAnnotation(DataJson.class);
		return decorators != null;
	}

	@Override
	public void insertData(
			final DBAccessSQL ioDb,
			final PreparedStatement ps,
			final Field field,
			final Object rootObject,
			final CountInOut iii)
			throws IllegalArgumentException, IllegalAccessException, SQLException, JsonProcessingException {
		final Object data = field.get(rootObject);
		if (data == null) {
			ps.setNull(iii.value, Types.VARCHAR);
		}
		final ObjectMapper objectMapper = ContextGenericTools.createObjectMapper();
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
			final DBAccessSQL ioDb,
			final ResultSet rs,
			final Field field,
			final Object data,
			final CountInOut count,
			final QueryOptions options,
			final List<LazyGetter> lazyCall) throws Exception {
		final List<OptionSpecifyType> specificTypes = options.get(OptionSpecifyType.class);
		final String jsonData = rs.getString(count.value);
		count.inc();
		if (!rs.wasNull()) {
			final ObjectMapper objectMapper = ContextGenericTools.createObjectMapper();
			if (field.getType() == List.class) {
				final ParameterizedType listType = (ParameterizedType) field.getGenericType();
				Class<?> listClass = (Class<?>) listType.getActualTypeArguments()[0];
				if (listClass == Object.class) {
					for (final OptionSpecifyType specify : specificTypes) {
						if (specify.name.equals(field.getName())) {
							listClass = specify.clazz;
							LOGGER.trace("Detect overwrite of typing var={} ... '{}' => '{}'", field.getName(),
									listClass.getCanonicalName(), specify.clazz.getCanonicalName());
							break;
						}
					}
				}
				if (listClass == Long.class) {
					final Object dataParsed = objectMapper.readValue(jsonData, new TypeReference<List<Long>>() {});
					field.set(data, dataParsed);
					return;
				}
				if (listClass == Float.class) {
					final Object dataParsed = objectMapper.readValue(jsonData, new TypeReference<List<Float>>() {});
					field.set(data, dataParsed);
					return;
				}
				if (listClass == Double.class) {
					final Object dataParsed = objectMapper.readValue(jsonData, new TypeReference<List<Double>>() {});
					field.set(data, dataParsed);
					return;
				}
				if (listClass == Integer.class) {
					final Object dataParsed = objectMapper.readValue(jsonData, new TypeReference<List<Integer>>() {});
					field.set(data, dataParsed);
					return;
				}
				if (listClass == Short.class) {
					final Object dataParsed = objectMapper.readValue(jsonData, new TypeReference<List<Short>>() {});
					field.set(data, dataParsed);
					return;
				}
				if (listClass == String.class) {
					final Object dataParsed = objectMapper.readValue(jsonData, new TypeReference<List<String>>() {});
					field.set(data, dataParsed);
					return;
				}
				if (listClass == UUID.class) {
					final Object dataParsed = objectMapper.readValue(jsonData, new TypeReference<List<UUID>>() {});
					field.set(data, dataParsed);
					return;
				}
				if (listClass == ObjectId.class) {
					final Object dataParsed = objectMapper.readValue(jsonData, new TypeReference<List<ObjectId>>() {});
					field.set(data, dataParsed);
					return;
				}
				LOGGER.warn("Maybe fail to translate Model in datajson list: List<{}>", listClass.getCanonicalName());
			}
			final TypeFactory typeFactory = objectMapper.getTypeFactory();
			Class<?> listClass = field.getType();
			if (listClass == Object.class) {
				for (final OptionSpecifyType specify : specificTypes) {
					if (specify.name.equals(field.getName())) {
						listClass = specify.clazz;
						LOGGER.trace("Detect overwrite of typing var={} ... '{}' => '{}'", field.getName(),
								listClass.getCanonicalName(), specify.clazz.getCanonicalName());
						break;
					}
				}
				final JavaType javaType = typeFactory.constructType(listClass);
				final Object dataParsed = objectMapper.readValue(jsonData, javaType);
				field.set(data, dataParsed);
			} else {
				final JavaType fieldType = typeFactory.constructType(field.getGenericType());
				final Object dataParsed = objectMapper.readValue(jsonData, fieldType);
				field.set(data, dataParsed);
			}
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
			final int fieldId,
			final QueryOptions options) throws Exception {
		DataFactory.createTablesSpecificType(tableName, primaryField, field, mainTableBuilder, preActionList,
				postActionList, createIfNotExist, createDrop, fieldId, JsonValue.class, options);
	}

	public static void addLink(
			final DBAccess ioDb,
			final Class<?> clazz,
			final String columnId,
			final Object id,
			final String columnList,
			final Object remoteKey) throws Exception {
		final String tableName = AnnotationTools.getTableName(clazz);
		final QueryOptions options = new QueryOptions(new OverrideTableName(tableName),
				new OptionSpecifyType("id", id.getClass()),
				new OptionSpecifyType("covers", remoteKey.getClass(), true));
		if (columnId != null && !columnId.equals("id")) {
			options.add(new OptionRenameColumn("id", columnId));
		}
		if (columnList != null && !columnList.equals("covers")) {
			options.add(new OptionRenameColumn("covers", columnList));
		}
		final TableCoversGeneric data = ioDb.get(TableCoversGeneric.class, id, options.getAllArray());
		if (data.covers == null) {
			data.covers = new ArrayList<>();
		}
		for (final Object elem : data.covers) {
			if (elem.equals(remoteKey)) {
				return;
			}
		}
		data.covers.add(remoteKey);
		ioDb.update(data, data.id, List.of("covers"), options.getAllArray());
	}

	public static void removeLink(
			final DBAccess ioDb,
			final Class<?> clazz,
			final String columnId,
			final Object id,
			final String columnList,
			final Object remoteKey) throws Exception {
		if (ioDb instanceof final DBAccessSQL daSQL) {
			final String tableName = AnnotationTools.getTableName(clazz);
			final QueryOptions options = new QueryOptions(new OverrideTableName(tableName),
					new OptionSpecifyType("id", id.getClass()),
					new OptionSpecifyType("covers", remoteKey.getClass(), true));
			if (columnId != null && !columnId.equals("id")) {
				options.add(new OptionRenameColumn("id", columnId));
			}
			if (columnList != null && !columnList.equals("covers")) {
				options.add(new OptionRenameColumn("covers", columnList));
			}
			final TableCoversGeneric data = ioDb.get(TableCoversGeneric.class, id, options.getAllArray());
			if (data.covers == null) {
				return;
			}
			final List<Object> newList = new ArrayList<>();
			for (final Object elem : data.covers) {
				if (elem.equals(remoteKey)) {
					continue;
				}
				newList.add(elem);
			}
			data.covers = newList;
			ioDb.update(data, data.id, List.of("covers"), options.getAllArray());
		} else if (ioDb instanceof final DBAccessMorphia dam) {

		} else {
			throw new DataAccessException("DataAccess Not managed");
		}
	}
}
