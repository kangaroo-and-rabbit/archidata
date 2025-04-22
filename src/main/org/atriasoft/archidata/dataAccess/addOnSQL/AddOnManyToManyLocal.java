package org.atriasoft.archidata.dataAccess.addOnSQL;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.atriasoft.archidata.annotation.AnnotationTools;
import org.atriasoft.archidata.annotation.AnnotationTools.FieldName;
import org.atriasoft.archidata.annotation.ManyToManyLocal;
import org.atriasoft.archidata.dataAccess.CountInOut;
import org.atriasoft.archidata.dataAccess.DBAccessSQL;
import org.atriasoft.archidata.dataAccess.DataFactory;
import org.atriasoft.archidata.dataAccess.LazyGetter;
import org.atriasoft.archidata.dataAccess.QueryInList;
import org.atriasoft.archidata.dataAccess.QueryOptions;
import org.atriasoft.archidata.dataAccess.commonTools.ManyToManyLocalTools;
import org.atriasoft.archidata.dataAccess.options.Condition;
import org.atriasoft.archidata.exception.SystemException;
import org.atriasoft.archidata.tools.ContextGenericTools;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.validation.constraints.NotNull;

public class AddOnManyToManyLocal implements DataAccessAddOn {
	static final Logger LOGGER = LoggerFactory.getLogger(AddOnManyToManyLocal.class);
	static final String SEPARATOR_LONG = "-";
	static final String SEPARATOR_UUID = "_";

	@Override
	public Class<?> getAnnotationClass() {
		return ManyToManyLocal.class;
	}

	@Override
	public boolean isCompatibleField(final Field field) {
		if (field.getType() != List.class) {
			return false;
		}
		final Class<?> objectClass = (Class<?>) ((ParameterizedType) field.getGenericType())
				.getActualTypeArguments()[0];
		if (objectClass == Long.class || objectClass == UUID.class || objectClass == ObjectId.class) {
			return true;
		}
		final ManyToManyLocal decorators = field.getDeclaredAnnotation(ManyToManyLocal.class);
		if (decorators == null) {
			return false;
		}
		if (decorators.targetEntity() == objectClass) {
			return true;
		}
		return false;
	}

	@Override
	public void insertData(
			final DBAccessSQL ioDb,
			final PreparedStatement ps,
			final Field field,
			final Object rootObject,
			final CountInOut iii)
			throws SQLException, IllegalArgumentException, IllegalAccessException, JsonProcessingException {
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
	public boolean isUpdateAsync(final Field field) {
		return true;
	}

	@Override
	public void asyncUpdate(
			final DBAccessSQL ioDb,
			final Object previousData,
			final String tableName,
			final Object primaryKeyValue,
			final Field field,
			final Object insertedData,
			final List<LazyGetter> actions,
			final QueryOptions options) throws Exception {
		final Object previousDataValue = field.get(previousData);
		Collection<?> previousDataCollection = new ArrayList<>();
		if (previousDataValue instanceof final Collection<?> tmpCollection) {
			previousDataCollection = tmpCollection;
		}
		final Object insertedDataValue = insertedData;
		Collection<?> insertedDataCollection = new ArrayList<>();
		if (insertedDataValue instanceof final Collection<?> tmpCollection) {
			insertedDataCollection = tmpCollection;
		}
		// add new Values
		for (final Object value : insertedDataCollection) {
			if (previousDataCollection.contains(value)) {
				continue;
			}
			actions.add(() -> {
				ManyToManyLocalTools.addLinkRemote(ioDb, field, primaryKeyValue, value);
			});
		}
		// remove old values:
		for (final Object value : previousDataCollection) {
			if (insertedDataCollection.contains(value)) {
				continue;
			}
			actions.add(() -> {
				ManyToManyLocalTools.removeLinkRemote(ioDb, field, primaryKeyValue, value);
			});
		}

	}

	/** Some action must be done asynchronously for update or remove element
	 * @param field
	 * @return */
	@Override
	public boolean isInsertAsync(final Field field) throws Exception {
		return true;
	}

	/** When insert is mark async, this function permit to create or update the data
	 * @param tableName Name of the Table.
	 * @param primaryKeyValue Local ID of the current table
	 * @param field Field that is updated.
	 * @param data Data that might be inserted.
	 * @param actions Asynchronous action to do after main request. */
	@Override
	public void asyncInsert(
			final DBAccessSQL ioDb,
			final String tableName,
			final Object primaryKeyValue,
			final Field field,
			final Object data,
			final List<LazyGetter> actions,
			final QueryOptions options) throws Exception {
		final Object insertedData = data;
		if (insertedData == null) {
			return;
		}
		if (insertedData instanceof final Collection<?> insertedDataCollection) {
			for (final Object value : insertedDataCollection) {
				actions.add(() -> {
					ManyToManyLocalTools.addLinkRemote(ioDb, field, primaryKeyValue, value);
				});
			}
		}
	}

	@Override
	public boolean isPreviousDataNeeded(final Field field) {
		return true;
	}

	@Override
	public boolean canInsert(final Field field) {
		return isCompatibleField(field);
	}

	@Override
	public boolean canRetrieve(final Field field) {
		return isCompatibleField(field);
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
		if (field.getType() != List.class) {
			throw new SystemException("@ManyToManyLocal must contain a List");
		}
		final String jsonData = rs.getString(count.value);
		count.inc();
		if (rs.wasNull()) {
			return;
		}
		final ObjectMapper objectMapper = ContextGenericTools.createObjectMapper();
		final ParameterizedType listType = (ParameterizedType) field.getGenericType();
		final Class<?> objectClass = (Class<?>) listType.getActualTypeArguments()[0];
		if (objectClass == Long.class) {
			final List<Long> dataParsed = objectMapper.readValue(jsonData, new TypeReference<List<Long>>() {});
			field.set(data, dataParsed);
			return;
		}
		if (objectClass == String.class) {
			final List<String> dataParsed = objectMapper.readValue(jsonData, new TypeReference<List<String>>() {});
			field.set(data, dataParsed);
			return;
		}
		if (objectClass == UUID.class) {
			final List<UUID> dataParsed = objectMapper.readValue(jsonData, new TypeReference<List<UUID>>() {});
			field.set(data, dataParsed);
			return;
		}
		if (objectClass == ObjectId.class) {
			final List<ObjectId> dataParsed = objectMapper.readValue(jsonData, new TypeReference<List<ObjectId>>() {});
			field.set(data, dataParsed);
			return;
		}
		final ManyToManyLocal decorators = field.getDeclaredAnnotation(ManyToManyLocal.class);
		if (decorators == null) {
			return;
		}
		if (objectClass == decorators.targetEntity()) {
			final Class<?> foreignKeyType = AnnotationTools.getPrimaryKeyField(objectClass).getType();
			if (foreignKeyType == Long.class) {
				final List<Long> idList = objectMapper.readValue(jsonData, new TypeReference<List<Long>>() {});
				if (idList != null && idList.size() > 0) {
					final FieldName idField = AnnotationTools.getFieldName(AnnotationTools.getIdField(objectClass),
							options);
					// In the lazy mode, the request is done in asynchronous mode, they will be done after...
					final LazyGetter lambda = () -> {
						// TODO: update to have get with abstract types ....
						final Object foreignData = ioDb.getsWhere(decorators.targetEntity(),
								new Condition(new QueryInList<>(idField.inTable(), idList)));
						if (foreignData == null) {
							return;
						}
						field.set(data, foreignData);
					};
					lazyCall.add(lambda);
				}
			} else if (foreignKeyType == UUID.class) {
				final List<UUID> idList = objectMapper.readValue(jsonData, new TypeReference<List<UUID>>() {});
				if (idList != null && idList.size() > 0) {
					final FieldName idField = AnnotationTools.getFieldName(AnnotationTools.getIdField(objectClass),
							options);
					// In the lazy mode, the request is done in asynchronous mode, they will be done after...
					final LazyGetter lambda = () -> {
						final List<UUID> childs = new ArrayList<>(idList);
						// TODO: update to have get with abstract types ....
						final Object foreignData = ioDb.getsWhere(decorators.targetEntity(),
								new Condition(new QueryInList<>(idField.inTable(), childs)));
						if (foreignData == null) {
							return;
						}
						field.set(data, foreignData);
					};
					lazyCall.add(lambda);
				}
			} else if (foreignKeyType == ObjectId.class) {
				final List<ObjectId> idList = objectMapper.readValue(jsonData, new TypeReference<List<ObjectId>>() {});
				if (idList != null && idList.size() > 0) {
					final FieldName idField = AnnotationTools.getFieldName(AnnotationTools.getIdField(objectClass),
							options);
					// In the lazy mode, the request is done in asynchronous mode, they will be done after...
					final LazyGetter lambda = () -> {
						final List<ObjectId> childs = new ArrayList<>(idList);
						// TODO: update to have get with abstract types ....
						final Object foreignData = ioDb.getsWhere(decorators.targetEntity(),
								new Condition(new QueryInList<>(idField.inTable(), childs)));
						if (foreignData == null) {
							return;
						}
						field.set(data, foreignData);
					};
					lazyCall.add(lambda);
				}
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
		// store data as json to response like a no-sql
		DataFactory.createTablesSpecificType(tableName, primaryField, field, mainTableBuilder, preActionList,
				postActionList, createIfNotExist, createDrop, fieldId, JsonValue.class, options);
	}

}
