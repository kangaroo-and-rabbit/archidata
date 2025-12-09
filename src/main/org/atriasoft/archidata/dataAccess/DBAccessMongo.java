package org.atriasoft.archidata.dataAccess;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import org.atriasoft.archidata.annotation.AnnotationTools;
import org.atriasoft.archidata.annotation.AnnotationTools.FieldName;
import org.atriasoft.archidata.annotation.CreationTimestamp;
import org.atriasoft.archidata.annotation.UpdateTimestamp;
import org.atriasoft.archidata.dataAccess.addOn.AddOnManyToManyDoc;
import org.atriasoft.archidata.dataAccess.addOn.AddOnManyToOneDoc;
import org.atriasoft.archidata.dataAccess.addOn.AddOnOneToManyDoc;
import org.atriasoft.archidata.dataAccess.addOn.DataAccessAddOn;
import org.atriasoft.archidata.dataAccess.options.AccessDeletedItems;
import org.atriasoft.archidata.dataAccess.options.Condition;
import org.atriasoft.archidata.dataAccess.options.DirectData;
import org.atriasoft.archidata.dataAccess.options.DirectPrimaryKey;
import org.atriasoft.archidata.dataAccess.options.FilterOmit;
import org.atriasoft.archidata.dataAccess.options.FilterValue;
import org.atriasoft.archidata.dataAccess.options.Limit;
import org.atriasoft.archidata.dataAccess.options.OptionSpecifyType;
import org.atriasoft.archidata.dataAccess.options.OrderBy;
import org.atriasoft.archidata.dataAccess.options.OverrideTableName;
import org.atriasoft.archidata.dataAccess.options.QueryOption;
import org.atriasoft.archidata.dataAccess.options.ReadAllColumn;
import org.atriasoft.archidata.dataAccess.options.TransmitKey;
import org.atriasoft.archidata.db.DbConfig;
import org.atriasoft.archidata.db.DbIo;
import org.atriasoft.archidata.db.DbIoFactory;
import org.atriasoft.archidata.db.DbIoMongo;
import org.atriasoft.archidata.exception.DataAccessException;
import org.atriasoft.archidata.tools.ContextGenericTools;
import org.atriasoft.archidata.tools.TypeUtils;
import org.atriasoft.archidata.tools.UuidUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.json.JsonWriterSettings;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.ReturnDocument;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.InternalServerErrorException;

/**
 * Data access class that provides MongoDB database operations with a functional
 * wrapper to minimize direct database code writing.
 */
public class DBAccessMongo implements Closeable {
	static final Logger LOGGER = LoggerFactory.getLogger(DBAccessMongo.class);

	// Some element for statistic:
	public static class MongoDbStatistic {
		public long countCountDocuments = 0L;
		public long countDeleteMany = 0L;
		public long countDrop = 0L;
		public long countFind = 0L;
		public long countFindOneAndUpdate = 0L;
		public long countInsertOne = 0L;
		public long countUpdateMany = 0L;
		public long countRunCommand = 0L;

		public void display() {
			LOGGER.info("""
					statistic on access on DB:
					    - insertOne        = {}
					    - updateMany       = {}
					    - find             = {}
					    - findOneAndUpdate = {}
					    - countDocuments   = {}
					    - deleteMany       = {}
					    - drop             = {}
					    - runCommand       = {}
					""", //
					String.format("%10d", this.countInsertOne), //
					String.format("%10d", this.countUpdateMany), //
					String.format("%10d", this.countFind), //
					String.format("%10d", this.countFindOneAndUpdate), //
					String.format("%10d", this.countCountDocuments), //
					String.format("%10d", this.countDeleteMany), //
					String.format("%10d", this.countDrop), //
					String.format("%10d", this.countRunCommand)); //
		}
	};

	public static MongoDbStatistic statistic = new MongoDbStatistic();

	// by default we manage some add-on that permit to manage non-native model (like
	// json serialization, List of external key as String list...)
	static final List<DataAccessAddOn> addOn = new ArrayList<>();

	static {
		addOn.add(new AddOnManyToManyDoc());
		addOn.add(new AddOnManyToOneDoc());
		addOn.add(new AddOnOneToManyDoc());
	}

	/**
	 * Add a new add-on on the current management.
	 *
	 * @param addOn instantiate object on the Add-on
	 */
	public static void addAddOn(final DataAccessAddOn addOn) {
		DBAccessMongo.addOn.add(addOn);
	}

	// ========================================================================
	// Static factory methods
	// ========================================================================

	public static final DBAccessMongo createInterface()
			throws InternalServerErrorException, IOException, DataAccessException {
		return DBAccessMongo.createInterface(DbIoFactory.create());
	}

	public static final DBAccessMongo createInterface(final DbConfig config)
			throws InternalServerErrorException, IOException {
		return DBAccessMongo.createInterface(DbIoFactory.create(config));
	}

	public static final DBAccessMongo createInterface(final DbIo io) throws InternalServerErrorException {
		if (io instanceof final DbIoMongo ioMorphia) {
			try {
				return new DBAccessMongo(ioMorphia);
			} catch (final IOException e) {
				e.printStackTrace();
				throw new InternalServerErrorException("Fail to create DB interface.");
			}
		}
		throw new InternalServerErrorException("unknown DB interface ... ");
	}

	// ========================================================================
	// Instance fields
	// ========================================================================

	private final DbIoMongo db;

	public DBAccessMongo(final DbIoMongo db) throws IOException {
		this.db = db;
		db.open();
	}

	public void close() throws IOException {
		this.db.close();
	}

	public DbIoMongo getInterface() {
		return this.db;
	}

	// ========================================================================
	// Helper methods from DBAccessMongo
	// ========================================================================

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

	// ========================================================================
	// Insert methods
	// ========================================================================

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
		final List<ReadAllColumn> readAllColumnOptions = options.get(ReadAllColumn.class);
		for (final ReadAllColumn elem : readAllColumnOptions) {
			injectedOptions.add(elem);
		}
		final List<AccessDeletedItems> accessDeletedItemsOptions = options.get(AccessDeletedItems.class);
		for (final AccessDeletedItems elem : accessDeletedItemsOptions) {
			injectedOptions.add(elem);
		}
		return (T) get(data.getClass(), insertedId, injectedOptions.getAllArray());
	}

	public <T> T insertWithJson(final Class<T> clazz, final String jsonData) throws Exception {
		final ObjectMapper mapper = ContextGenericTools.createObjectMapper();
		// parse the object to be sure the data are valid:
		final T data = mapper.readValue(jsonData, clazz);
		return insert(data);
	}

	// ========================================================================
	// Update methods
	// ========================================================================

	@Deprecated
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

	@Deprecated
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
		options.add(new TransmitKey(id));
		return updateWhere(data, options);
	}

	public <T> long updateWhere(final T data, final QueryOption... option) throws Exception {
		final QueryOptions options = new QueryOptions(option);
		return updateWhere(data, options);
	}

	// ========================================================================
	// Get methods
	// ========================================================================

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

	@SuppressWarnings("unchecked")
	public <T> List<T> getsWhere(final Class<T> clazz, final QueryOptions options)
			throws DataAccessException, IOException {
		final List<Object> out = getsWhereRaw(clazz, options);
		return (List<T>) out;
	}

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

	// ========================================================================
	// Count methods
	// ========================================================================

	public <ID_TYPE> long count(final Class<?> clazz, final ID_TYPE id, final QueryOption... option) throws Exception {
		final QueryOptions options = new QueryOptions(option);
		options.add(new Condition(getTableIdCondition(clazz, id, options)));
		return countWhere(clazz, options);
	}

	public long countWhere(final Class<?> clazz, final QueryOption... option) throws Exception {
		final QueryOptions options = new QueryOptions(option);
		return countWhere(clazz, options);
	}

	// ========================================================================
	// Delete methods
	// ========================================================================

	public <ID_TYPE> long delete(final Class<?> clazz, final ID_TYPE id, final QueryOption... options)
			throws Exception {
		final String hasDeletedFieldName = AnnotationTools.getDeletedFieldName(clazz);
		if (hasDeletedFieldName != null) {
			return deleteSoft(clazz, id, options);
		} else {
			return deleteHard(clazz, id, options);
		}
	}

	public long deleteWhere(final Class<?> clazz, final QueryOption... option) throws Exception {
		final String hasDeletedFieldName = AnnotationTools.getDeletedFieldName(clazz);
		if (hasDeletedFieldName != null) {
			return deleteSoftWhere(clazz, option);
		} else {
			return deleteHardWhere(clazz, option);
		}
	}

	// ========================================================================
	// MongoDB-specific and existing implementations
	// ========================================================================

	public List<String> listCollections(final String name, final QueryOption... option)
			throws InternalServerErrorException {
		return this.db.getDatabase().listCollectionNames().into(new ArrayList<>());
	}

	public void renameCollection(final String source, final String destination) {
		final MongoCollection<Document> previousCollection = this.db.getDatabase().getCollection(source);
		previousCollection
				.renameCollection(new com.mongodb.MongoNamespace(this.db.getDatabase().getName(), destination));
	}

	public boolean isDBExist(final String name, final QueryOption... option) throws InternalServerErrorException {
		// in Mongo DB we do not need to create a DB, then we have no need to check if
		// it exist
		return true;
	}

	public boolean createDB(final String name) {
		// in Mongo DB we do not need to create a DB it is dynamically created when
		// insert the first element.
		return true;
	}

	public boolean deleteDB(final String name) {
		final MongoDatabase database = this.db.getClient().getDatabase(name);
		database.drop();
		return true;
	}

	public boolean isTableExist(final String name, final QueryOption... option) throws InternalServerErrorException {
		// With mongo the dB exist only when the data is inserted
		return true;
	}

	/**
	 * This is a test
	 */
	public void ascendingIndex(final String collectionName, final String fieldName) {
		final MongoCollection<Document> collection = this.db.getDatabase().getCollection(collectionName);
		collection.createIndex(Indexes.ascending(fieldName));
	}

	/**
	 * Disables schema validation for an existing MongoDB collection.
	 *
	 * This method removes any JSON Schema validator associated with the specified
	 * collection and turns off server-side validation entirely. Documents can be
	 * inserted or updated without being checked against any schema.
	 *
	 * @param collection the name of the MongoDB collection to disable schema
	 *                   validation on.
	 */
	public void disableSchema(final String collection) {
		final Document command = new Document("collMod", collection)//
				.append("validator", new Document())//
				.append("validationLevel", "off");
		statistic.countRunCommand++;
		this.db.getDatabase().runCommand(command);
	}

	/**
	 * Sets or updates a JSON Schema validator for an existing MongoDB collection.
	 *
	 * The provided schema will be strictly enforced on all insert and update
	 * operations for the specified collection. This ensures that documents conform
	 * to the defined structure.
	 *
	 * @param collection the name of the MongoDB collection to apply the schema to.
	 * @param schema     a BSON Document representing the JSON Schema to enforce
	 *                   (must follow MongoDB's schema validation format).
	 */
	public void enableSchema(final String collection, final Document schema) {
		final Document command = new Document("collMod", collection)//
				.append("validator", schema)//
				.append("validationLevel", "strict");
		statistic.countRunCommand++;
		this.db.getDatabase().runCommand(command);
	}

	// TODO: add a mode for update like "variable.subVariable"
	public <T> void setValueToDb(
			final String parentFieldName, // Can be null if we update the full sub Object
			final Class<?> type,
			final T data,
			final Field field,
			final String fieldName,
			final Document docSet,
			final Document docUnSet // Can be null if we want to not use the unset (case for sub-object
	) throws Exception {
		String fieldComposeName = fieldName;
		if (parentFieldName != null && parentFieldName.isEmpty()) {
			fieldComposeName = parentFieldName + "." + fieldName;
		}
		if (field.get(data) == null) {
			if (docUnSet != null) {
				docUnSet.append(fieldComposeName, "");
			}
			return;
		}
		if (type == long.class) {
			docSet.append(fieldComposeName, field.getLong(data));
			return;
		}
		if (type == int.class) {
			docSet.append(fieldComposeName, field.getInt(data));
			return;
		}
		if (type == float.class) {
			docSet.append(fieldComposeName, field.getFloat(data));
			return;
		}
		if (type == double.class) {
			docSet.append(fieldComposeName, field.getDouble(data));
			return;
		}
		if (type == boolean.class) {
			docSet.append(fieldComposeName, field.getBoolean(data));
			return;
		}
		final Object tmp = field.get(data);
		if (tmp == null) {
			docUnSet.append(fieldComposeName, "");
			return;
		}
		if (type.isEnum()) {
			docSet.append(fieldComposeName, tmp.toString());
			return;
		}
		if (type == Long.class) {
			docSet.append(fieldComposeName, tmp);
			return;
		}
		if (type == Integer.class) {
			docSet.append(fieldComposeName, tmp);
			return;
		}
		if (type == Float.class) {
			docSet.append(fieldComposeName, tmp);
			return;
		}
		if (type == Double.class) {
			docSet.append(fieldComposeName, tmp);
			return;
		}
		if (type == Boolean.class) {
			docSet.append(fieldComposeName, tmp);
			return;
		}
		if (type == String.class) {
			docSet.append(fieldComposeName, tmp);
			return;
		}
		if (type == ObjectId.class) {
			docSet.append(fieldComposeName, tmp);
			return;
		}
		if (type == UUID.class) {
			docSet.append(fieldComposeName, tmp);
			return;
		}
		if (type == Date.class) {
			docSet.append(fieldComposeName, tmp);
			return;
		}
		if (type == Instant.class) {
			docSet.append(fieldComposeName, Date.from((Instant) tmp));
			return;
		}
		if (type == LocalDate.class) {
			final String dataToInsert = ((LocalDate) tmp).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
			docSet.append(fieldComposeName, dataToInsert);
			return;
		}
		if (type == LocalTime.class) {
			final Long timeNano = ((LocalTime) tmp).toNanoOfDay();
			docSet.append(fieldComposeName, timeNano);
			return;
		}
		if (tmp instanceof final List tmpList) {
			final List<Object> documentData = new ArrayList<>();
			for (final Object elem : tmpList) {
				documentData.add(convertInDocument(elem));
			}
			docSet.append(fieldComposeName, documentData);
			return;
		}
		if (tmp instanceof final Set tmpList) {
			final List<Object> documentData = new ArrayList<>();
			for (final Object elem : tmpList) {
				documentData.add(convertInDocument(elem));
			}
			docSet.append(fieldComposeName, documentData);
			return;
		}
		if (tmp instanceof final Map<?, ?> tmpMap) {
			// final Object value = doc.get(fieldName, field.getType());
			final Document documentData = new Document();
			for (final Entry<?, ?> elem : tmpMap.entrySet()) {
				final Object key = elem.getKey();
				final String keyString = convertMapKeyInString(key);
				final Object tmp1 = convertInDocument(elem.getValue());
				documentData.append(keyString, tmp1);
			}
			docSet.append(fieldComposeName, documentData);
			return;
		}
		final Object documentData = convertInDocument(tmp);
		docSet.append(fieldName, documentData);
	}

	/**
	 * Checks if the provided key class is supported for converting a map's string
	 * key into an object key.
	 *
	 * Supported key types are String, Integer, Long, Short, ObjectId, or any Enum.
	 *
	 * @param keyClass the Class object representing the key type to check
	 * @throws DataAccessException if the key class is not one of the supported
	 *                             types
	 */
	private void checkIfConvertMapStringKeyToObjectIsPossible(final Class<?> keyClass) throws DataAccessException {
		if (keyClass == String.class || keyClass == Integer.class || keyClass == Long.class || keyClass == Short.class
				|| keyClass == ObjectId.class || keyClass.isEnum()) {
			return;
		}
		throw new DataAccessException("Fail Map key is not a string, int, long, short, enum or ObjectId");
	}

	/**
	 * Converts a string key to an object of the specified key class without
	 * throwing an exception.
	 *
	 * Supports conversion to String, Integer, Long, Short, ObjectId, and Enum
	 * types.
	 *
	 * @param keyString the string representation of the key
	 * @param keyClass  the Class object of the desired key type
	 * @return the key converted to the target type, or null if the type is
	 *         unsupported
	 * @throws DataAccessException if the key cannot be converted properly (e.g.,
	 *                             enum conversion fails)
	 */
	private Object convertMapStringKeyToObjectNoThrow(final String keyString, final Class<?> keyClass)
			throws DataAccessException {
		if (keyClass == String.class) {
			return keyString;
		} else if (keyClass == Integer.class) {
			return Integer.parseInt(keyString);
		} else if (keyClass == Long.class) {
			return Long.parseLong(keyString);
		} else if (keyClass == Short.class) {
			return Short.parseShort(keyString);
		} else if (keyClass == ObjectId.class) {
			return new ObjectId(keyString);
		} else if (keyClass.isEnum()) {
			return retreiveValueEnum(keyClass, keyString);
		}
		return null;
	}

	/**
	 * Converts a map key object to its string representation.
	 *
	 * Supports keys of type String, Integer, Long, Short, ObjectId, and Enum.
	 * Throws an exception if the key type is unsupported.
	 *
	 * @param key the key object to convert to a string
	 * @return the string representation of the key
	 * @throws DataAccessException if the key type is not supported for conversion
	 *                             to string
	 */
	private String convertMapKeyInString(final Object key) throws DataAccessException {
		if (key instanceof final String keyTyped) {
			return keyTyped;
		}
		if (key instanceof final Integer keyTyped) {
			return Integer.toString(keyTyped);
		}
		if (key instanceof final Long keyTyped) {
			return Long.toString(keyTyped);
		}
		if (key instanceof final Short keyTyped) {
			return Short.toString(keyTyped);
		}
		if (key instanceof final ObjectId keyTyped) {
			return keyTyped.toString();
		}
		if (key.getClass().isEnum()) {
			return key.toString();
		}
		throw new DataAccessException("Fail Map key is not a string");
	}

	public <T> Object convertInDocument(final T data) throws Exception {
		if (data == null) {
			return null;
		}
		if (data instanceof Long) {
			return data;
		}
		if (data instanceof Integer) {
			return data;
		}
		if (data instanceof Short) {
			return data;
		}
		if (data instanceof Character) {
			return data;
		}
		if (data instanceof Float) {
			return data;
		}
		if (data instanceof Double) {
			return data;
		}
		if (data instanceof Boolean) {
			return data;
		}
		if (data instanceof ObjectId) {
			return data;
		}
		if (data instanceof UUID) {
			return data;
		}
		if (data instanceof final Instant tmp) {
			return Date.from(tmp);
		}
		if (data instanceof Date) {
			return data;
		}
		if (data instanceof final LocalDate tmp) {
			return tmp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
		}
		if (data instanceof final LocalTime tmp) {
			return tmp.toNanoOfDay();
		}
		if (data.getClass().isEnum()) {
			return data.toString();
		}
		if (data instanceof final List tmpList) {
			final List<Object> documentData = new ArrayList<>();
			for (final Object elem : tmpList) {
				documentData.add(convertInDocument(elem));
			}
			return documentData;
		}
		if (data instanceof final Set tmpList) {
			final List<Object> documentData = new ArrayList<>();
			for (final Object elem : tmpList) {
				documentData.add(convertInDocument(elem));
			}
			return documentData;
		}
		if (data instanceof final Map<?, ?> tmpMap) {
			// final Object value = doc.get(fieldName, field.getType());
			final Document documentData = new Document();
			for (final Entry<?, ?> elem : tmpMap.entrySet()) {
				final Object key = elem.getKey();
				final String keyString = convertMapKeyInString(key);
				final Object tmp1 = convertInDocument(elem.getValue());
				documentData.append(keyString, tmp1);
			}
			return documentData;
		}
		// =======================================
		// generic document:
		// =======================================

		final Document out = new Document();
		final Class<?> clazz = data.getClass();
		for (final Field field : clazz.getFields()) {
			// static field is only for internal global declaration ==> remove it ..
			if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
				continue;
			}
			final FieldName tableFieldName = AnnotationTools.getFieldName(field, null);
			Object currentInsertValue = field.get(data);
			final DataAccessAddOn addOn = findAddOnforField(field);
			if (addOn != null) {
				if (addOn.isInsertAsync(field)) {
					// asyncFieldUpdate.add(field);
					LOGGER.error("Not supported async action for the field : {}", tableFieldName);
				}
				if (!addOn.canInsert(field)) {
					continue;
				}
			}
			if (currentInsertValue == null && !field.getClass().isPrimitive()) {
				final DefaultValue[] defaultValue = field.getDeclaredAnnotationsByType(DefaultValue.class);
				if (defaultValue.length == 0) {
					continue;
				} else {
					final String value = defaultValue[0].value();
					if (value == null) {
						continue;
					}
					currentInsertValue = convertDefaultField(value, field);
				}
			}
			if (addOn != null) {
				addOn.insertData(this, field, data, null, out, null);
			} else {
				final Class<?> type = field.getType();
				if (!type.isPrimitive()) {
					if (field.get(data) == null) {
						if (currentInsertValue != null) {
							out.append(tableFieldName.inTable(), currentInsertValue);
						}
						continue;
					}
				}
				// if set name @Null this mean we want to update the full subObject
				setValueToDb(null, type, data, field, tableFieldName.inTable(), out, null);
			}
		}
		return out;
	}

	public <T> void setValueFromDoc(
			final Type type,
			final Object data,
			final Field field,
			final Document doc,
			final List<LazyGetter> lazyCall,
			final QueryOptions options) throws Exception {
		final String fieldName = AnnotationTools.getFieldName(field, options).inTable();
		if (type instanceof final ParameterizedType parameterizedType) {
			// Manage List & Set:
			if (type instanceof List) {
				final Object value = doc.get(fieldName);
				if (value == null) {
					return;
				}
				if (!(value instanceof List<?>)) {
					throw new DataAccessException("mapping a list on somethin not a list ... bad request");
				}
				// get type of the object:
				final Type dataType = parameterizedType.getActualTypeArguments()[0];
				// generate the output
				final List<Object> out = new ArrayList<>();
				final List<?> valueList = (List<?>) value;
				for (final Object item : valueList) {
					out.add(createObjectFromDocument(item, dataType, new QueryOptions(), lazyCall));
				}
				field.set(data, out);
				return;
			}
			if (Set.class == field.getType()) {
				final Object value = doc.get(fieldName);
				if (value == null) {
					return;
				}
				if (!(value instanceof Set<?>)) {
					throw new DataAccessException("mapping a Set on somethin not a list ... bad request");
				}
				// get type of the object:
				final Type dataType = parameterizedType.getActualTypeArguments()[0];
				// generate the output
				final List<Object> out = new ArrayList<>();
				final Set<?> valueList = (Set<?>) value;
				for (final Object item : valueList) {
					out.add(createObjectFromDocument(item, dataType, new QueryOptions(), lazyCall));
				}
				field.set(data, out);
				return;
			}
			// Manage Map:
			if (Map.class == field.getType()) {
				// inspectType(field.getGenericType(), 0);
				// field.set(data, value);
				final ParameterizedType typeTemplate = (ParameterizedType) field.getGenericType();
				final Class<?> keyClass = (Class<?>) typeTemplate.getActualTypeArguments()[0];

				final Class<?> objectClass = (Class<?>) (typeTemplate).getActualTypeArguments()[1];
				if (keyClass != String.class) {
					throw new DataAccessException("Fail Map key is not a string");
				}
				final Map<String, Object> out = new HashMap<>();

				final Object fieldValue = doc.get(fieldName);
				if (fieldValue instanceof final Document subDoc) {
					for (final Map.Entry<String, Object> entry : subDoc.entrySet()) {
						final String key = entry.getKey();
						final Object value = entry.getValue();
						if (value == null) {
							out.put(key, null);
						} else if (objectClass.isAssignableFrom(value.getClass())) {
							out.put(key, value);
						} else if (value instanceof final Document temporaryDocumentObject) {
							final Object valueObject = createObjectFromDocument(temporaryDocumentObject, objectClass,
									new QueryOptions(), lazyCall);
							out.put(key, valueObject);
						} else if (value instanceof final String temporaryString && objectClass.isEnum()) {
							final Object valueEnum = retreiveValueEnum(objectClass, temporaryString);
							out.put(key, valueEnum);
						} else {
							throw new DataAccessException(
									"type of object " + key.toString() + "=>" + value.getClass().getCanonicalName()
											+ " requested=" + objectClass.getCanonicalName());
						}
					}
					field.set(data, out);
					return;
				}
			}
		} else if (type instanceof final Class clazz) {
			if (!doc.containsKey(fieldName)) {
				field.set(data, null);
				return;
			}
			if (clazz == UUID.class) {
				final UUID value = doc.get(fieldName, UUID.class);
				field.set(data, value);
				return;
			}
			if (clazz == ObjectId.class) {
				final ObjectId value = doc.get(fieldName, ObjectId.class);
				field.set(data, value);
				return;
			}
			if (clazz == Long.class || clazz == long.class) {
				final Long value = doc.getLong(fieldName);
				field.set(data, value);
				return;
			}
			if (clazz == Integer.class || clazz == int.class) {
				final Integer value = doc.getInteger(fieldName);
				field.set(data, value);
				return;
			}
			if (clazz == Float.class || clazz == float.class) {
				final Double value = doc.getDouble(fieldName);
				field.set(data, (float) ((double) value));
				return;
			}
			if (clazz == Double.class || clazz == double.class) {
				final Double value = doc.getDouble(fieldName);
				field.set(data, value);
				return;
			}
			if (clazz == Boolean.class || clazz == boolean.class) {
				final Boolean value = doc.getBoolean(fieldName);
				field.set(data, value);
				return;
			}
			if (clazz == Date.class) {
				final Date value = doc.get(fieldName, Date.class);
				field.set(data, value);
				return;
			}
			if (clazz == Instant.class) {
				final Date value = doc.get(fieldName, Date.class);
				final Instant newData = value.toInstant();
				field.set(data, newData);
				return;
			}
			if (clazz == LocalDate.class) {
				final String value = doc.get(fieldName, String.class);
				final LocalDate newData = LocalDate.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
				field.set(data, newData);
				return;
			}
			if (clazz == LocalTime.class) {
				final Long value = doc.getLong(fieldName);
				final LocalTime newData = LocalTime.ofNanoOfDay(value);
				field.set(data, newData);
				return;
			}
			if (clazz == String.class) {
				final String value = doc.getString(fieldName);
				field.set(data, value);
				return;
			}
			if (clazz == UUID.class) {
				final Object value = doc.get(fieldName, field.getType());
				field.set(data, value);
				return;
			}
			if (clazz.isEnum()) {
				final String value = doc.getString(fieldName);
				field.set(data, retreiveValueEnum(clazz, value));
				return;
			}
			// manage a sub-object
			final Object value = createObjectFromDocument(doc.get(fieldName, Document.class), field.getType(),
					new QueryOptions(), lazyCall);
			field.set(data, value);
		} else {
			throw new DataAccessException("Fail to analyze type: " + type.toString());
		}
	}

	private Object retreiveValueEnum(final Class<?> objectClass, final String temporaryString)
			throws DataAccessException {
		final Object[] arr = objectClass.getEnumConstants();
		for (final Object elem : arr) {
			if (elem.toString().equals(temporaryString)) {
				return elem;
			}
		}
		throw new DataAccessException("Enum value does not exist in the Model val='" + temporaryString + "' model="
				+ objectClass.getCanonicalName());

	}

	protected Object convertDefaultField(String data, final Field field) throws Exception {
		if (data.startsWith("'") && data.endsWith("'")) {
			data = data.substring(1, data.length() - 1);
		}
		final Class<?> type = field.getType();
		if (type == Long.class || type == long.class) {
			return Long.parseLong(data);
		}
		if (type == Integer.class || type == int.class) {
			return Integer.parseInt(data);
		}
		if (type == Float.class || type == float.class) {
			return Float.parseFloat(data);
		}
		if (type == Double.class || type == double.class) {
			return Double.parseDouble(data);
		}
		if (type == Boolean.class || type == boolean.class) {
			return Boolean.parseBoolean(data);
		}
		if (type == Date.class) {}
		if (type == Instant.class) {}
		if (type == LocalDate.class) {}
		if (type == LocalTime.class) {}
		if (type == String.class) {
			return data;
		}
		if (type.isEnum()) {
			return retreiveValueEnum(type, data);
		}
		throw new DataAccessException(
				"Request default of unknow native type " + type.getCanonicalName() + " => " + data);
	}

	public boolean isAddOnField(final Field field) {
		return findAddOnforField(field) != null;
	}

	public DataAccessAddOn findAddOnforField(final Field field) {
		for (final DataAccessAddOn elem : addOn) {
			if (elem.isCompatibleField(field)) {
				return elem;
			}
		}
		return null;
	}

	public long getNextSequenceLongValue(final String collectionName, String fieldName) {
		if (fieldName == null || fieldName.isEmpty()) {
			fieldName = "sequence_id";
		}
		// Collection "counters" to store the sequences if Ids
		final MongoCollection<Document> countersCollection = this.db.getDatabase().getCollection("counters");

		// Filter to find the specific counter for the collections
		final Document filter = new Document("_id", collectionName);

		// Update the field <fieldName> of 1
		final Document update = new Document("$inc", new Document(fieldName, 1L));

		// get the value after updated it
		final FindOneAndUpdateOptions options = new FindOneAndUpdateOptions().returnDocument(ReturnDocument.AFTER)
				.upsert(true); // create field if not exist

		// Real creation of the unique counter.
		statistic.countFindOneAndUpdate++;
		final Document updatedCounter = countersCollection.findOneAndUpdate(filter, update, options);

		// Return the new sequence value...
		return updatedCounter.getLong(fieldName);
	}

	@SuppressWarnings("unchecked")
	public <T extends Object> Object insertPrimaryKey_test(final T data, final QueryOption... option) throws Exception {
		final Class<?> clazz = data.getClass();
		final QueryOptions options = new QueryOptions(option);
		final boolean directdata = options.exist(DirectData.class);

		final String collectionName = AnnotationTools.getTableName(clazz, options);
		Long primaryKey = null;
		try {
			for (final Field field : clazz.getFields()) {
				// static field is only for internal global declaration ==> remove it ..
				if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
					continue;
				}
				if (AnnotationTools.isPrimaryKey(field)) {
					if (!directdata) {
						if (field.getType() == Long.class || field.getType() == long.class) {
							// By default the MongoDB does not manage the
							primaryKey = getNextSequenceLongValue(collectionName, field.getName());
							field.setLong(data, primaryKey);
							continue;
						}
						continue;
					}
				}
				final boolean createTime = field.getDeclaredAnnotationsByType(CreationTimestamp.class).length != 0;
				if (!directdata && createTime) {
					field.set(data, Date.from(Instant.now()));
					continue;
				}
				final boolean updateTime = field.getDeclaredAnnotationsByType(UpdateTimestamp.class).length != 0;
				if (!directdata && updateTime) {
					field.set(data, Date.from(Instant.now()));
					continue;
				}
				// TODO:
				// - Manage the add-on of manyToOne and ...
			}

			@SuppressWarnings("unchecked")
			final MongoCollection<T> collection = this.db.getDatabase().getCollection(collectionName, (Class<T>) clazz);
			statistic.countInsertOne++;
			final InsertOneResult res = collection.insertOne(data);
			if (primaryKey != null) {
				return primaryKey;
			}
			final ObjectId insertedId = res.getInsertedId().asObjectId().getValue();
			return insertedId;
		} catch (final Exception ex) {
			LOGGER.error("Fail Mongo request: {}", ex.getMessage());
			ex.printStackTrace();
			throw new DataAccessException("Fail to Insert data in DB : " + ex.getMessage());
		}
	}

	@SuppressWarnings("unchecked")
	public <T> Object insertPrimaryKey(final T data, final QueryOption... option) throws Exception {
		final Class<?> clazz = data.getClass();
		final QueryOptions options = new QueryOptions(option);
		final boolean directdata = options.exist(DirectData.class);
		final boolean directPrimaryKey = options.exist(DirectPrimaryKey.class);

		final List<Field> asyncFieldUpdate = new ArrayList<>();
		final String collectionName = AnnotationTools.getTableName(clazz, options);
		Field primaryKeyField = null;
		Object uniqueId = null;
		// real add in the BDD:
		try {
			final MongoCollection<Document> collection = this.db.getDatabase().getCollection(collectionName);
			final Document docSet = new Document();
			final Document docUnSet = new Document();
			for (final Field field : clazz.getFields()) {
				// static field is only for internal global declaration ==> remove it ..
				if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
					continue;
				}
				final FieldName tableFieldName = AnnotationTools.getFieldName(field, options);
				Object currentInsertValue = field.get(data);
				if (AnnotationTools.isPrimaryKey(field)) {
					if (!directdata && !directPrimaryKey) {
						if (field.get(data) != null) {
							throw new DataAccessException(
									"Unexpected comportment, try to add an object with a primary key. use option 'DirectPrimaryKey.class' to do that");
						}
						primaryKeyField = field;
						if (primaryKeyField.getType() == ObjectId.class) {
							uniqueId = new ObjectId();
							docSet.append(tableFieldName.inTable(), uniqueId);
							continue;
						} else if (primaryKeyField.getType() == UUID.class) {
							final UUID uuid = UuidUtils.nextUUID();
							uniqueId = uuid;
							docSet.append(tableFieldName.inTable(), uuid);
							continue;
						} else if (primaryKeyField.getType() == Long.class || primaryKeyField.getType() == long.class) {
							// By default the MongoDB does not manage the
							final long id = getNextSequenceLongValue(collectionName, tableFieldName.inTable());
							uniqueId = id;
							docSet.append(tableFieldName.inTable(), id);
							continue;
						}
						throw new DataAccessException("TODO: Manage the ID primary key for type: "
								+ clazz.getCanonicalName() + " => " + primaryKeyField.getType());
					} else {
						// Do nothing...
						final Object primaryKeyValue = field.get(data);
						if (primaryKeyValue == null) {
							throw new DataAccessException(
									"Fail to Insert data in DB.. when use 'DirectData.class' or 'DirectPrimaryKey.class' you need to provide a primary key...");
						}
						uniqueId = primaryKeyValue;
					}
				}
				final boolean createTime = field.getDeclaredAnnotationsByType(CreationTimestamp.class).length != 0;
				if (!directdata && createTime) {
					docSet.append(tableFieldName.inTable(), Date.from(Instant.now()));
					continue;
				}
				final boolean updateTime = field.getDeclaredAnnotationsByType(UpdateTimestamp.class).length != 0;
				if (!directdata && updateTime) {
					docSet.append(tableFieldName.inTable(), Date.from(Instant.now()));
					continue;
				}
				final DataAccessAddOn addOn = findAddOnforField(field);
				if (addOn != null) {
					if (addOn.isInsertAsync(field)) {
						asyncFieldUpdate.add(field);
					}
					if (!addOn.canInsert(field)) {
						continue;
					}
				}
				if (currentInsertValue == null && !field.getClass().isPrimitive()) {
					final DefaultValue[] defaultValue = field.getDeclaredAnnotationsByType(DefaultValue.class);
					if (defaultValue.length == 0) {
						continue;
					} else {
						final String value = defaultValue[0].value();
						if (value == null) {
							continue;
						}
						currentInsertValue = convertDefaultField(value, field);
					}
				}
				// conversion table ...
				// doc.append(tableFieldName, currentInsertValue);
				if (addOn != null) {
					addOn.insertData(this, field, data, options, docSet, docUnSet);
				} else {
					final Class<?> type = field.getType();
					if (!type.isPrimitive()) {
						if (field.get(data) == null) {
							if (currentInsertValue != null) {
								docSet.append(tableFieldName.inTable(), currentInsertValue);
							}
							continue;
						}
					}
					// if set name @Null this mean we want to update the full subObject
					setValueToDb(null, type, data, field, tableFieldName.inTable(), docSet, null);
				}
			}
			// LOGGER.trace("insertPrimaryKey: docSet={}",
			// docSet.toJson(JsonWriterSettings.builder().indent(true).build()));
			statistic.countInsertOne++;
			final InsertOneResult result = collection.insertOne(docSet);
			// Get the Object of inserted object:
			// TODO: to keep the inserted id value: ObjectId insertedId = result.getInsertedId().asObjectId().getValue(); // only work with objectId ...
			// LOGGER.trace("Document inserted with ID: " + insertedId);
		} catch (final Exception ex) {
			LOGGER.error("Fail Mongo request: {}", ex.getMessage());
			ex.printStackTrace();
			throw new DataAccessException("Fail to Insert data in DB : " + ex.getMessage());
		}
		final List<LazyGetter> asyncActions = new ArrayList<>();
		for (final Field field : asyncFieldUpdate) {
			final DataAccessAddOn addOn = findAddOnforField(field);
			if (uniqueId instanceof final Long id) {
				addOn.asyncInsert(this, data.getClass(), id, field, field.get(data), asyncActions, options);
			} else if (uniqueId instanceof final UUID uuid) {
				addOn.asyncInsert(this, data.getClass(), uuid, field, field.get(data), asyncActions, options);
			} else if (uniqueId instanceof final ObjectId oid) {
				addOn.asyncInsert(this, data.getClass(), oid, field, field.get(data), asyncActions, options);
			}
		}
		List<LazyGetter> actionsAsync = asyncActions;
		for (int kkk = 0; kkk < 500 && actionsAsync.size() != 0; kkk++) {
			final List<LazyGetter> actionsAsyncNew = new ArrayList<>();
			for (final LazyGetter action : actionsAsync) {
				action.doRequest(actionsAsyncNew);
			}
			actionsAsync = actionsAsyncNew;
		}
		return uniqueId;
	}

	public <T> long updateWhere(final T data, QueryOptions options) throws Exception {
		final Class<?> clazz = data.getClass();
		if (options == null) {
			options = new QueryOptions();
		}
		final boolean directdata = options.exist(DirectData.class);
		final Condition condition = conditionFusionOrEmpty(options, true);
		final List<FilterValue> filterKeys = options != null ? options.get(FilterValue.class) : new ArrayList<>();
		if (filterKeys.size() != 1) {
			throw new DataAccessException("request a gets without/or with more 1 FilterValue of values");
		}
		final FilterValue filterKey = filterKeys.get(0);
		final List<FilterOmit> filterOmitKeys = options != null ? options.get(FilterOmit.class) : new ArrayList<>();
		FilterOmit filterOmitKey = null;
		if (filterOmitKeys.size() > 1) {
			throw new DataAccessException("request a gets without/or with more 1 FilterOmit of values");
		} else if (filterOmitKeys.size() == 1) {
			filterOmitKey = filterOmitKeys.get(0);
		}

		final List<LazyGetter> asyncActions = new ArrayList<>();

		// Some mode need to get the previous data to perform a correct update...
		boolean needPreviousValues = false;
		for (final Field field : clazz.getFields()) {
			// field is only for internal global declaration ==> remove it ..
			if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
				continue;
			}
			if (AnnotationTools.isGenericField(field)) {
				continue;
			}
			final DataAccessAddOn addOn = findAddOnforField(field);
			if (addOn != null && addOn.isPreviousDataNeeded(field)) {
				needPreviousValues = true;
				break;
			}
		}
		Object previousData = null;
		if (needPreviousValues) {
			final List<TransmitKey> transmitKey = options.get(TransmitKey.class);
			previousData = this.get(data.getClass(), transmitKey.get(0).getKey(), new AccessDeletedItems(),
					new ReadAllColumn());
		}

		// real add in the BDD:
		try {
			final String collectionName = AnnotationTools.getTableName(clazz, options);

			final String deletedFieldName = AnnotationTools.getDeletedFieldName(clazz);
			final Bson filters = condition.getFilter(collectionName, options, deletedFieldName);
			final Document docSet = new Document();
			final Document docUnSet = new Document();
			for (final Field field : clazz.getFields()) {
				// static field is only for internal global declaration ==> remove it ..
				if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
					continue;
				}
				final FieldName fieldName = AnnotationTools.getFieldName(field, options);
				// update field is not conditioned by filter:
				final boolean updateTime = field.getDeclaredAnnotationsByType(UpdateTimestamp.class).length != 0;
				if (!directdata && updateTime) {
					docSet.append(fieldName.inTable(), Date.from(Instant.now()));
					continue;
				}
				final boolean createTime = field.getDeclaredAnnotationsByType(CreationTimestamp.class).length != 0;
				if (!directdata && createTime) {
					continue;
				}
				if (AnnotationTools.isGenericField(field)) {
					continue;
				} else if (filterOmitKey != null && filterOmitKey.getValues().contains(fieldName.inStruct())) {
					continue;
				} else if (!filterKey.getValues().contains(fieldName.inStruct())) {
					continue;
				}
				final DataAccessAddOn addOn = findAddOnforField(field);
				if (addOn != null) {
					if (addOn.isUpdateAsync(field)) {
						final List<TransmitKey> transmitKey = options.get(TransmitKey.class);
						if (transmitKey.size() != 1) {
							throw new DataAccessException(
									"Fail to transmit Key to update the async update... (must have only 1)");
						}
						addOn.asyncUpdate(this, previousData, transmitKey.get(0).getKey(), field, field.get(data),
								asyncActions, options);
					}
					if (!addOn.canInsert(field)) {
						continue;
					}
				}
				if (addOn != null) {
					addOn.insertData(this, field, data, options, docSet, docUnSet);
				} else {
					final Class<?> type = field.getType();
					if (!type.isPrimitive()) {
						final Object tmp = field.get(data);
						if (tmp == null && field.getDeclaredAnnotationsByType(DefaultValue.class).length != 0) {
							continue;
						}
					}
					// if set name @Null this mean we want to update the full subObject
					setValueToDb(null, type, data, field, fieldName.inTable(), docSet, docUnSet);
				}
			}
			// Do the query ...
			final MongoCollection<Document> collection = this.db.getDatabase().getCollection(collectionName);
			final Document actions = new Document();
			if (!docSet.isEmpty()) {
				actions.append("$set", docSet);
			}
			if (!docUnSet.isEmpty()) {
				actions.append("$unset", docUnSet);
			}

			// LOGGER.debug("updateWhere filter: {}",
			// filters.toBsonDocument().toJson(JsonWriterSettings.builder().indent(true).build()));
			// LOGGER.debug("updateWhere Actions: {}",
			// actions.toJson(JsonWriterSettings.builder().indent(true).build()));
			statistic.countUpdateMany++;
			final UpdateResult ret = collection.updateMany(filters, actions);
			List<LazyGetter> actionsAsync = asyncActions;
			for (int kkk = 0; kkk < 500 && actionsAsync.size() != 0; kkk++) {
				final List<LazyGetter> actionsAsyncNew = new ArrayList<>();
				for (final LazyGetter action : actionsAsync) {
					action.doRequest(actionsAsyncNew);
				}
				actionsAsync = actionsAsyncNew;
			}
			return ret.getModifiedCount();
		} catch (final Exception ex) {
			ex.printStackTrace();
		}
		return 0;
	}

	public List<String> generateSelectField(final Class<?> clazz, final QueryOptions options) throws Exception {
		final boolean readAllfields = QueryOptions.readAllColomn(options);
		final List<String> fieldsName = new ArrayList<>();

		for (final Field elem : clazz.getFields()) {
			// static field is only for internal global declaration ==> remove it ..
			if (java.lang.reflect.Modifier.isStatic(elem.getModifiers())) {
				continue;
			}
			final DataAccessAddOn addOn = findAddOnforField(elem);
			if (addOn != null && !addOn.canRetrieve(elem)) {
				continue;
			}
			final boolean notRead = AnnotationTools.isDefaultNotRead(elem);
			if (!readAllfields && notRead) {
				continue;
			}
			final String name = AnnotationTools.getFieldName(elem, options).inTable();
			fieldsName.add(name);
		}
		return fieldsName;
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

	public List<Object> getsWhereRaw(final Class<?> clazz, final QueryOptions options)
			throws DataAccessException, IOException {
		final Condition condition = conditionFusionOrEmpty(options, false);
		final List<LazyGetter> lazyCall = new ArrayList<>();
		final String deletedFieldName = AnnotationTools.getDeletedFieldName(clazz);
		final String collectionName = AnnotationTools.getTableName(clazz, options);
		final List<Object> outs = new ArrayList<>();
		final MongoCollection<Document> collection = this.db.getDatabase().getCollection(collectionName);
		try {
			// Generate the filtering of the data:
			final Bson filters = condition.getFilter(collectionName, options, deletedFieldName);
			if (filters != null) {
				LOGGER.trace("filter = {}",
						filters.toBsonDocument().toJson(JsonWriterSettings.builder().indent(true).build()));
			} else {
				LOGGER.trace("filter = None");
			}
			FindIterable<Document> retFind = null;
			statistic.countFind++;
			if (filters != null) {
				// LOGGER.debug("getsWhere Find filter: {}", filters.toBsonDocument().toJson());
				retFind = collection.find(filters);
			} else {
				retFind = collection.find();
			}
			/*
			 * Not manage right now ... final List<GroupBy> groups =
			 * options.get(GroupBy.class); for (final GroupBy group : groups) {
			 * group.generateQuery(query, tableName); }
			 */
			final List<OrderBy> orders = options.get(OrderBy.class);
			if (orders.size() != 0) {
				final Document sorts = new Document();
				for (final OrderBy order : orders) {
					order.generateSort(sorts);
				}
				retFind = retFind.sort(sorts);
			}

			final List<Limit> limits = options.get(Limit.class);
			if (limits.size() == 1) {
				retFind = retFind.limit((int) limits.get(0).getValue());
			} else if (limits.size() > 1) {
				throw new DataAccessException("Request with multiple 'limit'...");
			}
			// Select values to read
			final List<String> listFields = generateSelectField(clazz, options);
			listFields.add("_id");
			retFind = retFind.projection(Projections.include(listFields.toArray(new String[0])));

			LOGGER.trace("GetsWhere ...");
			final MongoCursor<Document> cursor = retFind.iterator();
			try (cursor) {
				while (cursor.hasNext()) {
					final Document doc = cursor.next();
					LOGGER.trace(" - receive data from DB: {}",
							doc.toJson(JsonWriterSettings.builder().indent(true).build()));
					final Object data = createObjectFromDocument(doc, clazz, options, lazyCall);
					outs.add(data);
				}
				// LOGGER.trace("Async calls: {}", lazyCall.size());
				List<LazyGetter> actionsAsync = lazyCall;
				for (int kkk = 0; kkk < 500 && actionsAsync.size() != 0; kkk++) {
					final List<LazyGetter> actionsAsyncNew = new ArrayList<>();
					for (final LazyGetter action : actionsAsync) {
						action.doRequest(actionsAsyncNew);
					}
					actionsAsync = actionsAsyncNew;
				}
			}
		} catch (final Exception ex) {
			ex.printStackTrace();
			throw new DataAccessException("Catch an Exception: " + ex.getMessage());
		}
		return outs;
	}

	public boolean isType(final Type type, final Class<?> destType) {
		if (type instanceof final ParameterizedType pType) {
			final Type rawType = pType.getRawType();
			if (rawType instanceof Class<?>) {
				return destType.isAssignableFrom((Class<?>) rawType);
			}
		} else if (type instanceof Class<?>) {
			// Si c'est une Map brute sans génériques (rare, mais possible)
			return destType.isAssignableFrom((Class<?>) type);
		}
		return false;
	}

	public Object createObjectFromDocument(
			final Object doc,
			final Type type,
			final QueryOptions options,
			final List<LazyGetter> lazyCall) throws Exception {
		// inspectType(type, 0);
		if (doc == null) {
			return null;
		}
		if (type instanceof final ParameterizedType parameterizedType) {
			// Manage List & Set:
			if (isType(type, List.class)) {
				final Object value = doc;
				if (!(value instanceof List<?>)) {
					throw new DataAccessException("mapping a 'List' on somethin not a 'List' ... bad request");
				}
				// get type of the object:
				final Type dataType = parameterizedType.getActualTypeArguments()[0];
				// generate the output
				final List<Object> out = new ArrayList<>();
				final List<?> valueList = (List<?>) value;
				for (final Object item : valueList) {
					out.add(createObjectFromDocument(item, dataType, new QueryOptions(), lazyCall));
				}
				return out;
			}
			if (isType(type, Set.class)) {
				final Object value = doc;
				if (!(value instanceof List<?>)) {
					throw new DataAccessException("mapping a 'Set' on somethin not a 'List' ... bad request");
				}
				// get type of the object:
				final Type dataType = parameterizedType.getActualTypeArguments()[0];
				// generate the output
				final Set<Object> out = new HashSet<>();
				final List<?> valueList = (List<?>) value;
				for (final Object item : valueList) {
					out.add(createObjectFromDocument(item, dataType, new QueryOptions(), lazyCall));
				}
				return out;
			}
			// Manage Map:
			if (isType(type, Map.class)) {
				final Object value = doc;
				final Class<?> keyClass = (Class<?>) parameterizedType.getActualTypeArguments()[0];
				checkIfConvertMapStringKeyToObjectIsPossible(keyClass);

				final Type objectType = parameterizedType.getActualTypeArguments()[1];
				final Map<Object, Object> out = new HashMap<>();

				if (value instanceof final Document subDoc) {
					for (final Map.Entry<String, Object> entry : subDoc.entrySet()) {
						final String keyString = entry.getKey();
						final Object key = convertMapStringKeyToObjectNoThrow(keyString, keyClass);
						out.put(key,
								createObjectFromDocument(entry.getValue(), objectType, new QueryOptions(), lazyCall));
					}
					return out;
				}
			}
			throw new DataAccessException("Fail to read data for type: '" + type.getTypeName() + "' (NOT IMPLEMENTED)");
		} else if (type instanceof final Class clazz) {
			if (clazz == UUID.class) {
				// final UUID value = doc.get(fieldName, UUID.class);
				// field.set(data, value);
				return null;
			}
			if (clazz == ObjectId.class && doc instanceof final ObjectId temporary) {
				return temporary;
			}
			if (clazz == Long.class || clazz == long.class) {
				if (doc instanceof final Long temporary) {
					return temporary;
				}
				if (doc instanceof final Integer temporary) {
					return temporary.longValue();
				}
				if (doc instanceof final Short temporary) {
					return temporary.longValue();
				}
			}
			if (clazz == Integer.class || clazz == int.class) {
				if (doc instanceof final Integer temporary) {
					return temporary;
				}
				if (doc instanceof final Long temporary) {
					return temporary.intValue();
				}
				if (doc instanceof final Short temporary) {
					return temporary.intValue();
				}
			}
			if (clazz == Short.class || clazz == short.class) {
				if (doc instanceof final Integer temporary) {
					return temporary.shortValue();
				}
				if (doc instanceof final Long temporary) {
					return temporary.shortValue();
				}
				if (doc instanceof final Short temporary) {
					return temporary;
				}
			}
			if (clazz == Float.class || clazz == float.class) {
				if (doc instanceof final Float temporary) {
					return temporary;
				}
				if (doc instanceof final Double temporary) {
					return temporary.floatValue();
				}
				if (doc instanceof final Integer temporary) {
					return temporary.floatValue();
				}
				if (doc instanceof final Long temporary) {
					return temporary.floatValue();
				}
				if (doc instanceof final Short temporary) {
					return temporary.floatValue();
				}
			}
			if (clazz == Double.class || clazz == double.class) {
				if (doc instanceof final Float temporary) {
					return temporary.doubleValue();
				}
				if (doc instanceof final Double temporary) {
					return temporary;
				}
				if (doc instanceof final Integer temporary) {
					return temporary.doubleValue();
				}
				if (doc instanceof final Long temporary) {
					return temporary.doubleValue();
				}
				if (doc instanceof final Short temporary) {
					return temporary.doubleValue();
				}
			}
			if ((clazz == Boolean.class || clazz == boolean.class) && doc instanceof final Boolean temporary) {
				return temporary;
			}
			if (clazz == Date.class && doc instanceof final Date temporary) {
				return temporary;
			}
			if (clazz == Instant.class && doc instanceof final Date temporary) {
				return temporary.toInstant();
			}
			if (clazz == LocalDate.class && doc instanceof final String temporary) {
				return LocalDate.parse(temporary, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
			}
			if (clazz == LocalTime.class && doc instanceof final Long temporary) {
				return LocalTime.ofNanoOfDay(temporary);
			}
			if (clazz == String.class && doc instanceof final String temporary) {
				return temporary;
			}
			if (clazz == UUID.class) {
				// final Object value = doc.get(fieldName, field.getType());
				// field.set(data, value);
				return null;
			}
			if (doc instanceof final String temporary && clazz.isEnum()) {
				return retreiveValueEnum(clazz, temporary);
			}

			if (doc instanceof final Document documentModel) {
				final List<OptionSpecifyType> specificTypes = options.get(OptionSpecifyType.class);
				// LOGGER.trace("createObjectFromDocument: {}", clazz.getCanonicalName());
				final boolean readAllfields = QueryOptions.readAllColomn(options);
				// TODO: manage class that is defined inside a class ==> Not manage for now...
				Object data = null;
				for (final Constructor<?> contructor : clazz.getConstructors()) {
					if (contructor.getParameterCount() == 0) {
						data = contructor.newInstance();
						break;
					}
				}
				if (data == null) {
					throw new DataAccessException(
							"Can not find the default constructor for the class: " + clazz.getCanonicalName());
				}
				for (final Field field : clazz.getFields()) {
					// static field is only for internal global declaration ==> remove it ..
					if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
						continue;
					}
					final DataAccessAddOn addOn = findAddOnforField(field);
					if (addOn != null && !addOn.canRetrieve(field)) {
						continue;
					}
					final boolean notRead = AnnotationTools.isDefaultNotRead(field);
					if (!readAllfields && notRead) {
						continue;
					}
					if (addOn != null) {
						addOn.fillFromDoc(this, documentModel, field, data, options, lazyCall);
					} else {
						final Type typeModified = getFieldModifiedType(field, specificTypes, clazz);
						final String fieldName = AnnotationTools.getFieldName(field, options).inTable();
						final Object dataField = createObjectFromDocument(documentModel.get(fieldName), typeModified,
								options, lazyCall);
						field.set(data, dataField);
					}
				}
				return data;
			} else {
				throw new DataAccessException("Fail to read data for type: '" + type.getTypeName()
						+ "' detect data that is not a Document doc=" + doc + "  " + doc.getClass());
			}
		}
		throw new DataAccessException("Fail to read data for type: '" + type.getTypeName() + "' (NOT IMPLEMENTED)");

	}

	/**
	 * Determines the modified generic type of a given field based on a list of type
	 * specifications.
	 *
	 * This method checks if the field's name matches any entry in the provided list
	 * of {@code OptionSpecifyType}. If a match is found and the specification
	 * indicates a list type, it replaces the generic type with a list of the
	 * specified class type. Otherwise, if the field's type is {@code Object}, it
	 * overrides it with the specified class.
	 *
	 * @param field         the {@code Field} whose type is to be potentially
	 *                      modified
	 * @param specificTypes a list of {@code OptionSpecifyType} defining specific
	 *                      type overrides for fields
	 * @param clazz         the class context in which this type modification is
	 *                      applied (unused in the current method)
	 * @return the potentially modified {@code Type} of the field after applying the
	 *         specifications
	 */
	private Type getFieldModifiedType(
			final Field field,
			final List<OptionSpecifyType> specificTypes,
			final Class<?> clazz) {
		Type typeModified = field.getGenericType();
		for (final OptionSpecifyType specify : specificTypes) {
			if (specify.name.equals(field.getName())) {
				if (specify.isList) {
					if (isType(typeModified, List.class)) {
						typeModified = TypeUtils.listOf(specify.clazz);
						break;
					}
				} else if (typeModified == Object.class) {
					typeModified = specify.clazz;
					break;
				}
			}
		}
		return typeModified;
	}

	public long countWhere(final Class<?> clazz, final QueryOptions options) throws Exception {
		final Condition condition = conditionFusionOrEmpty(options, false);
		final String deletedFieldName = AnnotationTools.getDeletedFieldName(clazz);
		final String collectionName = AnnotationTools.getTableName(clazz, options);
		final MongoCollection<Document> collection = this.db.getDatabase().getCollection(collectionName);
		try {
			// Generate the filtering of the data:
			final Bson filters = condition.getFilter(collectionName, options, deletedFieldName);
			statistic.countCountDocuments++;
			if (filters != null) {
				return collection.countDocuments(filters);
			}
			return collection.countDocuments();
		} catch (final Exception ex) {
			ex.printStackTrace();
			throw new DataAccessException("Catch an Exception: " + ex.getMessage());
		}
	}

	public <ID_TYPE> long deleteHard(final Class<?> clazz, final ID_TYPE id, final QueryOption... option)
			throws Exception {
		final QueryOptions options = new QueryOptions(option);
		options.add(new Condition(getTableIdCondition(clazz, id, options)));
		return deleteHardWhere(clazz, options.getAllArray());
	}

	public void actionOnDelete(final Class<?> clazz, final QueryOption... option) throws Exception {
		final List<LazyGetter> lazyCall = new ArrayList<>();
		// Some mode need to get the previous data to perform a correct update...
		boolean needPreviousValues = false;
		final List<Field> hasDeletedActionFields = new ArrayList<>();
		for (final Field field : clazz.getFields()) {
			// field is only for internal global declaration ==> remove it ..
			if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
				continue;
			}
			if (AnnotationTools.isGenericField(field)) {
				continue;
			}
			final DataAccessAddOn addOn = findAddOnforField(field);
			if (addOn != null && addOn.asDeleteAction(field)) {
				hasDeletedActionFields.add(field);
				if (addOn.isPreviousDataNeeded(field)) {
					needPreviousValues = true;
				}
			}
		}
		List<Object> previousData = null;
		if (needPreviousValues) {
			final QueryOptions options = new QueryOptions(option);
			options.add(new AccessDeletedItems());
			options.add(new ReadAllColumn());
			previousData = this.getsWhereRaw(clazz, options);
		}
		for (final Field field : hasDeletedActionFields) {
			final DataAccessAddOn addOn = findAddOnforField(field);
			if (addOn != null) {
				addOn.onDelete(this, clazz, field, previousData, lazyCall);
			}
		}
		List<LazyGetter> actionsAsync = lazyCall;
		for (int kkk = 0; kkk < 500 && actionsAsync.size() != 0; kkk++) {
			final List<LazyGetter> actionsAsyncNew = new ArrayList<>();
			for (final LazyGetter action : actionsAsync) {
				action.doRequest(actionsAsyncNew);
			}
			actionsAsync = actionsAsyncNew;
		}
	}

	public long deleteHardWhere(final Class<?> clazz, final QueryOption... option) throws Exception {
		final QueryOptions options = new QueryOptions(option);
		final Condition condition = conditionFusionOrEmpty(options, true);
		final String collectionName = AnnotationTools.getTableName(clazz, options);
		final String deletedFieldName = AnnotationTools.getDeletedFieldName(clazz);
		final MongoCollection<Document> collection = this.db.getDatabase().getCollection(collectionName);
		final Bson filters = condition.getFilter(collectionName, options, deletedFieldName);

		actionOnDelete(clazz, option);

		DeleteResult retFind;
		if (filters != null) {
			statistic.countDeleteMany++;
			retFind = collection.deleteMany(filters);
		} else {
			throw new DataAccessException("Too dangerout to delete element with no filter values !!!");
		}
		return retFind.getDeletedCount();
	}

	public <ID_TYPE> long deleteSoft(final Class<?> clazz, final ID_TYPE id, final QueryOption... option)
			throws Exception {
		final QueryOptions options = new QueryOptions(option);
		options.add(new Condition(getTableIdCondition(clazz, id, options)));
		return deleteSoftWhere(clazz, options.getAllArray());
	}

	public long deleteSoftWhere(final Class<?> clazz, final QueryOption... option) throws Exception {
		final QueryOptions options = new QueryOptions(option);
		final Condition condition = conditionFusionOrEmpty(options, true);
		final String collectionName = AnnotationTools.getTableName(clazz, options);
		final String deletedFieldName = AnnotationTools.getDeletedFieldName(clazz);
		final MongoCollection<Document> collection = this.db.getDatabase().getCollection(collectionName);
		final Bson filters = condition.getFilter(collectionName, options, deletedFieldName);
		final Document actions = new Document("$set", new Document(deletedFieldName, true));
		actionOnDelete(clazz, option);
		statistic.countUpdateMany++;
		final UpdateResult ret = collection.updateMany(filters, actions);
		return ret.getModifiedCount();
	}

	public <ID_TYPE> long unsetDelete(final Class<?> clazz, final ID_TYPE id) throws DataAccessException {
		return unsetDeleteWhere(clazz, new Condition(getTableIdCondition(clazz, id, new QueryOptions())));
	}

	public <ID_TYPE> long unsetDelete(final Class<?> clazz, final ID_TYPE id, final QueryOption... option)
			throws DataAccessException {
		final QueryOptions options = new QueryOptions(option);
		options.add(new Condition(getTableIdCondition(clazz, id, options)));
		return unsetDeleteWhere(clazz, options.getAllArray());
	}

	public long unsetDeleteWhere(final Class<?> clazz, final QueryOption... option) throws DataAccessException {
		final QueryOptions options = new QueryOptions(option);
		final Condition condition = conditionFusionOrEmpty(options, true);
		final String collectionName = AnnotationTools.getTableName(clazz, options);
		final String deletedFieldName = AnnotationTools.getDeletedFieldName(clazz);
		if (deletedFieldName == null) {
			throw new DataAccessException("The class " + clazz.getCanonicalName() + " has no deleted field");
		}
		final MongoCollection<Document> collection = this.db.getDatabase().getCollection(collectionName);
		final Bson filters = condition.getFilter(collectionName, options, deletedFieldName);
		final Document actions = new Document("$set", new Document(deletedFieldName, false));
		statistic.countUpdateMany++;
		final UpdateResult ret = collection.updateMany(filters, actions);
		return ret.getModifiedCount();
	}

	public void drop(final Class<?> clazz, final QueryOption... option) throws Exception {
		final QueryOptions options = new QueryOptions(option);
		final String collectionName = AnnotationTools.getTableName(clazz, options);
		final MongoCollection<Document> collection = this.db.getDatabase().getCollection(collectionName);
		statistic.countDrop++;
		collection.drop();
	}

	public void cleanAll(final Class<?> clazz, final QueryOption... option) throws Exception {
		final QueryOptions options = new QueryOptions(option);
		final String collectionName = AnnotationTools.getTableName(clazz, options);
		final MongoCollection<Document> collection = this.db.getDatabase().getCollection(collectionName);
		statistic.countDeleteMany++;
		collection.deleteMany(new Document());
	}

}
