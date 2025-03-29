package org.kar.archidata.dataAccess.addOnMongo;

import java.lang.reflect.Field;
import java.util.List;

import org.bson.Document;
import org.kar.archidata.annotation.AnnotationTools;
import org.kar.archidata.annotation.DataJson;
import org.kar.archidata.dataAccess.DBAccessMorphia;
import org.kar.archidata.dataAccess.DataFactory;
import org.kar.archidata.dataAccess.LazyGetter;
import org.kar.archidata.dataAccess.QueryOptions;
import org.kar.archidata.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonValue;

public class AddOnDataJson implements DataAccessAddOn {
	static final Logger LOGGER = LoggerFactory.getLogger(AddOnDataJson.class);

	@Override
	public Class<?> getAnnotationClass() {
		return DataJson.class;
	}

	@Override
	public String getSQLFieldType(final Field elem, final QueryOptions options) throws DataAccessException {
		final String fieldName = AnnotationTools.getFieldName(elem, options).inTable();
		return DataFactory.convertTypeInSQL(String.class, fieldName);
	}

	@Override
	public boolean isCompatibleField(final Field elem) {
		final DataJson decorators = elem.getDeclaredAnnotation(DataJson.class);
		return decorators != null;
	}

	@Override
	public void insertData(
			final DBAccessMorphia ioDb,
			final Field field,
			final Object rootObject,
			final QueryOptions options,
			final Document docSet,
			final Document docUnSet) throws Exception {
		/*
		final Object data = field.get(rootObject);
		if (data == null) {
			ps.setNull(iii.value, Types.VARCHAR);
		}
		final ObjectMapper objectMapper = ContextGenericTools.createObjectMapper();
		final String dataString = objectMapper.writeValueAsString(data);
		ps.setString(iii.value, dataString);
		iii.inc();
		*/
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
	public void fillFromDoc(
			final DBAccessMorphia ioDb,
			final Document doc,
			final Field field,
			final Object data,
			final QueryOptions options,
			final List<LazyGetter> lazyCall) throws Exception {
		/*
		final String fieldName = AnnotationTools.getFieldName(field);
		if (!doc.containsKey(fieldName)) {
			field.set(data, null);
			return;
		}
		final String jsonData = rs.getString(count.value);
		if (!rs.wasNull()) {
			final ObjectMapper objectMapper = ContextGenericTools.createObjectMapper();
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
		*/
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

}
