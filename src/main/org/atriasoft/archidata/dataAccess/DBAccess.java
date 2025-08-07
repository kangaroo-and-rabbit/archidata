package org.atriasoft.archidata.dataAccess;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.atriasoft.archidata.annotation.AnnotationTools;
import org.atriasoft.archidata.annotation.AnnotationTools.FieldName;
import org.atriasoft.archidata.dataAccess.options.Condition;
import org.atriasoft.archidata.dataAccess.options.FilterValue;
import org.atriasoft.archidata.dataAccess.options.Limit;
import org.atriasoft.archidata.dataAccess.options.OptionSpecifyType;
import org.atriasoft.archidata.dataAccess.options.OverrideTableName;
import org.atriasoft.archidata.dataAccess.options.QueryOption;
import org.atriasoft.archidata.dataAccess.options.TransmitKey;
import org.atriasoft.archidata.db.DbConfig;
import org.atriasoft.archidata.db.DbIo;
import org.atriasoft.archidata.db.DbIoFactory;
import org.atriasoft.archidata.db.DbIoMongo;
import org.atriasoft.archidata.db.DbIoSql;
import org.atriasoft.archidata.exception.DataAccessException;
import org.atriasoft.archidata.tools.ContextGenericTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.ws.rs.InternalServerErrorException;

/* TODO list:
   - Manage to group of SQL action to permit to commit only at the end.
 */

/** Data access is an abstraction class that permit to access on the DB with a function wrapping that permit to minimize the SQL writing of SQL code. This interface support the SQL and SQLite
 * back-end. */
public abstract class DBAccess implements Closeable {
	final static Logger LOGGER = LoggerFactory.getLogger(DBAccess.class);

	public static final DBAccess createInterface()
			throws InternalServerErrorException, IOException, DataAccessException {
		return DBAccess.createInterface(DbIoFactory.create());
	}

	public static final DBAccess createInterface(final DbConfig config)
			throws InternalServerErrorException, IOException {
		return DBAccess.createInterface(DbIoFactory.create(config));
	}

	public static final DBAccess createInterface(final DbIo io) throws InternalServerErrorException {
		if (io instanceof final DbIoMongo ioMorphia) {
			try {
				return new DBAccessMongo(ioMorphia);
			} catch (final IOException e) {
				e.printStackTrace();
				throw new InternalServerErrorException("Fail to create DB interface.");
			}
		} else if (io instanceof final DbIoSql ioSQL) {
			try {
				return new DBAccessSQL(ioSQL);
			} catch (final IOException e) {
				e.printStackTrace();
				throw new InternalServerErrorException("Fail to create DB interface.");
			}
		}
		throw new InternalServerErrorException("unknow DB interface ... ");
	}

	public List<String> listCollections(final String name, final QueryOption... option)
			throws InternalServerErrorException {
		throw new InternalServerErrorException("Can Not manage the DB-access");
	}

	public boolean isDBExist(final String name, final QueryOption... option) throws InternalServerErrorException {
		throw new InternalServerErrorException("Can Not manage the DB-access");
	}

	public boolean createDB(final String name) {
		throw new InternalServerErrorException("Can Not manage the DB-access");
	}

	public boolean deleteDB(final String name) {
		throw new InternalServerErrorException("Can Not manage the DB-access");
	}

	public boolean isTableExist(final String name, final QueryOption... option) throws InternalServerErrorException {
		throw new InternalServerErrorException("Can Not manage the DB-access");
	}

	public <ID_TYPE> QueryCondition getTableIdCondition(
			final Class<?> clazz,
			final ID_TYPE idKey,
			final QueryOptions options) throws DataAccessException {
		// Find the ID field type ....
		final Field idField = AnnotationTools.getIdField(clazz);
		if (idField == null) {
			throw new DataAccessException(
					"The class have no annotation @Id ==> can not determine the default type searching");
		}
		// check the compatibility of the id and the declared ID
		Class<?> typeClass = idField.getType();
		if (idKey == null) {
			throw new DataAccessException("Try to identify the ID type and object was null.");
		}
		final FieldName fieldName = AnnotationTools.getFieldName(idField, options);
		final List<OptionSpecifyType> specificTypes = options.get(OptionSpecifyType.class);
		if (typeClass == Object.class) {
			for (final OptionSpecifyType specify : specificTypes) {
				if (specify.name.equals(fieldName.inStruct())) {
					typeClass = specify.clazz;
					LOGGER.trace("Detect overwrite of typing ... '{}' => '{}'", typeClass.getCanonicalName(),
							specify.clazz.getCanonicalName());
					break;
				}
			}
		}
		if (idKey.getClass() != typeClass) {
			if (idKey.getClass() == Condition.class) {
				throw new DataAccessException(
						"Try to identify the ID type on a condition 'close' internal API error use xxxWhere(...) instead.");
			}
			throw new DataAccessException("Request update with the wrong type ...");
		}
		return new QueryCondition(fieldName.inTable(), "=", idKey);
	}

	// TODO: manage insert batch...
	public <T> List<T> insertMultiple(final List<T> data, final QueryOption... options) throws Exception {
		final List<T> out = new ArrayList<>();
		for (final T elem : data) {
			final T tmp = insert(elem, options);
			out.add(tmp);
		}
		return out;
	}

	@SuppressWarnings("unchecked")
	public <T> T insert(final T data, final QueryOption... option) throws Exception {
		final Object insertedId = insertPrimaryKey(data, option);
		final QueryOptions options = new QueryOptions(option);
		final QueryOptions injectedOptions = new QueryOptions();
		final List<OverrideTableName> override = options.get(OverrideTableName.class);
		if (override.size() != 0) {
			injectedOptions.add(override.get(0));
		}
		final List<OptionSpecifyType> typeOptions = options.get(OptionSpecifyType.class);
		for (final OptionSpecifyType elem : typeOptions) {
			injectedOptions.add(elem);
		}
		return (T) get(data.getClass(), insertedId, injectedOptions.getAllArray());
	}

	abstract public <T> Object insertPrimaryKey(final T data, final QueryOption... option) throws Exception;

	// seems a good idea, but very dangerous if we not filter input data... if set an id it can be complicated...
	public <T> T insertWithJson(final Class<T> clazz, final String jsonData) throws Exception {
		final ObjectMapper mapper = ContextGenericTools.createObjectMapper();
		// parse the object to be sure the data are valid:
		final T data = mapper.readValue(jsonData, clazz);
		return insert(data);
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
	public <T, ID_TYPE> long updateWithJson(
			final Class<T> clazz,
			final ID_TYPE id,
			final String jsonData,
			final QueryOption... option) throws Exception {
		final QueryOptions options = new QueryOptions(option);
		options.add(new Condition(getTableIdCondition(clazz, id, options)));
		options.add(new TransmitKey(id));
		return updateWhereWithJson(clazz, jsonData, options.getAllArray());
	}

	public <T> long updateWhereWithJson(final Class<T> clazz, final String jsonData, final QueryOption... option)
			throws Exception {
		final QueryOptions options = new QueryOptions(option);
		if (options.get(Condition.class).size() == 0) {
			throw new DataAccessException("request a updateWhereWithJson without any condition");
		}
		final ObjectMapper mapper = ContextGenericTools.createObjectMapper();
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

	public <T, ID_TYPE> long update(final T data, final ID_TYPE id, final QueryOption... option) throws Exception {
		final QueryOptions options = new QueryOptions(option);
		if (!options.exist(FilterValue.class)) {
			options.add(FilterValue.getEditableFieldsNames(data.getClass()));
		}
		return updateFull(data, id, options.getAllArray());
	}

	public <T, ID_TYPE> long updateFull(final T data, final ID_TYPE id, final QueryOption... option) throws Exception {
		final QueryOptions options = new QueryOptions(option);
		options.add(new Condition(getTableIdCondition(data.getClass(), id, options)));
		//options.add(new FilterValue(updateColomn));
		options.add(new TransmitKey(id));
		return updateWhere(data, options);
	}

	public <T> long updateWhere(final T data, final QueryOption... option) throws Exception {
		final QueryOptions options = new QueryOptions(option);
		return updateWhere(data, options);
	}

	public abstract <T> long updateWhere(final T data, QueryOptions options) throws Exception;

	public <T> T getWhere(final Class<T> clazz, final QueryOptions options) throws Exception {
		options.add(new Limit(1));
		final List<T> values = getsWhere(clazz, options);
		if (values.size() == 0) {
			return null;
		}
		return values.get(0);
	}

	public Object getWhereRaw(final Class<?> clazz, final QueryOptions options) throws Exception {
		options.add(new Limit(1));
		final List<Object> values = getsWhereRaw(clazz, options);
		if (values.size() == 0) {
			return null;
		}
		return values.get(0);
	}

	public <T> T getWhere(final Class<T> clazz, final QueryOption... option) throws Exception {
		final QueryOptions options = new QueryOptions(option);
		return getWhere(clazz, options);
	}

	public Object getWhereRaw(final Class<?> clazz, final QueryOption... option) throws Exception {
		final QueryOptions options = new QueryOptions(option);
		return getWhereRaw(clazz, options);
	}

	public <T> List<T> getsWhere(final Class<T> clazz, final QueryOption... option) throws Exception {
		final QueryOptions options = new QueryOptions(option);
		return getsWhere(clazz, options);
	}

	public List<Object> getsWhereRaw(final Class<?> clazz, final QueryOption... option) throws Exception {
		final QueryOptions options = new QueryOptions(option);
		return getsWhereRaw(clazz, options);
	}

	public Condition conditionFusionOrEmpty(final QueryOptions options, final boolean throwIfEmpty)
			throws DataAccessException {
		if (options == null) {
			return new Condition();
		}
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
	public <T> List<T> getsWhere(final Class<T> clazz, final QueryOptions options)
			throws DataAccessException, IOException {
		final List<Object> out = getsWhereRaw(clazz, options);
		return (List<T>) out;
	}

	abstract public List<Object> getsWhereRaw(final Class<?> clazz, final QueryOptions options)
			throws DataAccessException, IOException;

	public <ID_TYPE> long count(final Class<?> clazz, final ID_TYPE id, final QueryOption... option) throws Exception {
		final QueryOptions options = new QueryOptions(option);
		options.add(new Condition(getTableIdCondition(clazz, id, options)));
		return countWhere(clazz, options);
	}

	public long countWhere(final Class<?> clazz, final QueryOption... option) throws Exception {
		final QueryOptions options = new QueryOptions(option);
		return countWhere(clazz, options);
	}

	public abstract long countWhere(final Class<?> clazz, final QueryOptions options) throws Exception;

	public <T, ID_TYPE> T get(final Class<T> clazz, final ID_TYPE id, final QueryOption... option) throws Exception {
		final QueryOptions options = new QueryOptions(option);
		options.add(new Condition(getTableIdCondition(clazz, id, options)));
		return getWhere(clazz, options.getAllArray());
	}

	public <T> List<T> gets(final Class<T> clazz) throws Exception {
		return getsWhere(clazz);
	}

	public <T> List<T> gets(final Class<T> clazz, final QueryOption... option) throws Exception {
		return getsWhere(clazz, option);
	}

	/** Delete items with the specific Id (cf @Id) and some options. If the Entity is manage as a softDeleted model, then it is flag as removed (if not already done before).
	 * @param <ID_TYPE> Type of the reference @Id
	 * @param clazz Data model that might remove element
	 * @param id Unique Id of the model
	 * @param options (Optional) Options of the request
	 * @return Number of element that is removed. */
	public <ID_TYPE> long delete(final Class<?> clazz, final ID_TYPE id, final QueryOption... options)
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
	 * @param option (Optional) Options of the request.
	 * @return Number of element that is removed. */
	public long deleteWhere(final Class<?> clazz, final QueryOption... option) throws Exception {
		final String hasDeletedFieldName = AnnotationTools.getDeletedFieldName(clazz);
		if (hasDeletedFieldName != null) {
			return deleteSoftWhere(clazz, option);
		} else {
			return deleteHardWhere(clazz, option);
		}
	}

	public <ID_TYPE> long deleteHard(final Class<?> clazz, final ID_TYPE id, final QueryOption... option)
			throws Exception {
		final QueryOptions options = new QueryOptions(option);
		options.add(new Condition(getTableIdCondition(clazz, id, options)));
		return deleteHardWhere(clazz, options.getAllArray());
	}

	public abstract long deleteHardWhere(final Class<?> clazz, final QueryOption... option) throws Exception;

	public <ID_TYPE> long deleteSoft(final Class<?> clazz, final ID_TYPE id, final QueryOption... option)
			throws Exception {
		final QueryOptions options = new QueryOptions(option);
		options.add(new Condition(getTableIdCondition(clazz, id, options)));
		return deleteSoftWhere(clazz, options.getAllArray());
	}

	public abstract long deleteSoftWhere(final Class<?> clazz, final QueryOption... option) throws Exception;

	public <ID_TYPE> long unsetDelete(final Class<?> clazz, final ID_TYPE id) throws DataAccessException {
		return unsetDeleteWhere(clazz, new Condition(getTableIdCondition(clazz, id, new QueryOptions())));
	}

	public <ID_TYPE> long unsetDelete(final Class<?> clazz, final ID_TYPE id, final QueryOption... option)
			throws DataAccessException {
		final QueryOptions options = new QueryOptions(option);
		options.add(new Condition(getTableIdCondition(clazz, id, options)));
		return unsetDeleteWhere(clazz, options.getAllArray());
	}

	public abstract long unsetDeleteWhere(final Class<?> clazz, final QueryOption... option) throws DataAccessException;

	public abstract void drop(final Class<?> clazz, final QueryOption... option) throws Exception;

	public abstract void cleanAll(final Class<?> clazz, final QueryOption... option) throws Exception;

}
