package org.atriasoft.archidata.dataAccess;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.atriasoft.archidata.annotation.AnnotationTools;
import org.atriasoft.archidata.annotation.AnnotationTools.FieldName;
import org.atriasoft.archidata.annotation.CreationTimestamp;
import org.atriasoft.archidata.annotation.UpdateTimestamp;
import org.atriasoft.archidata.dataAccess.addOnMongo.AddOnManyToManyNoSql;
import org.atriasoft.archidata.dataAccess.addOnMongo.AddOnManyToOne;
import org.atriasoft.archidata.dataAccess.addOnMongo.AddOnManyToOneNoSql;
import org.atriasoft.archidata.dataAccess.addOnMongo.AddOnOneToMany;
import org.atriasoft.archidata.dataAccess.addOnMongo.AddOnOneToManyNoSql;
import org.atriasoft.archidata.dataAccess.addOnMongo.DataAccessAddOn;
import org.atriasoft.archidata.dataAccess.options.AccessDeletedItems;
import org.atriasoft.archidata.dataAccess.options.CheckFunction;
import org.atriasoft.archidata.dataAccess.options.Condition;
import org.atriasoft.archidata.dataAccess.options.DirectData;
import org.atriasoft.archidata.dataAccess.options.FilterValue;
import org.atriasoft.archidata.dataAccess.options.Limit;
import org.atriasoft.archidata.dataAccess.options.OptionSpecifyType;
import org.atriasoft.archidata.dataAccess.options.OrderBy;
import org.atriasoft.archidata.dataAccess.options.QueryOption;
import org.atriasoft.archidata.dataAccess.options.ReadAllColumn;
import org.atriasoft.archidata.dataAccess.options.TransmitKey;
import org.atriasoft.archidata.db.DbIoMongo;
import org.atriasoft.archidata.exception.DataAccessException;
import org.atriasoft.archidata.tools.UuidUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.InternalServerErrorException;

/** Data access is an abstraction class that permit to access on the DB with a function wrapping that permit to minimize the SQL writing of SQL code. This interface support the SQL and SQLite
 * back-end. */
public class DBAccessMongo extends DBAccess {
	static final Logger LOGGER = LoggerFactory.getLogger(DBAccessMongo.class);
	// by default we manage some add-on that permit to manage non-native model (like json serialization, List of external key as String list...)
	static final List<DataAccessAddOn> addOn = new ArrayList<>();

	static {
		addOn.add(new AddOnManyToManyNoSql());
		addOn.add(new AddOnManyToOneNoSql());
		addOn.add(new AddOnOneToManyNoSql());

		// Not implementable without performance fail...
		//addOn.add(new AddOnManyToMany());
		// Deprecated
		addOn.add(new AddOnManyToOne());
		// Deprecated
		addOn.add(new AddOnOneToMany());
	}

	/** Add a new add-on on the current management.
	 * @param addOn instantiate object on the Add-on
	 */
	public static void addAddOn(final DataAccessAddOn addOn) {
		DBAccessMongo.addOn.add(addOn);
	}

	private final DbIoMongo db;

	public DBAccessMongo(final DbIoMongo db) throws IOException {
		this.db = db;
		db.open();
	}

	@Override
	public void close() throws IOException {
		this.db.close();
	}

	public DbIoMongo getInterface() {
		return this.db;
	}

	@Override
	public boolean isDBExist(final String name, final QueryOption... option) throws InternalServerErrorException {
		// in Mongo DB we do not need to create a DB, then we have no need to check if it exist
		return true;
	}

	@Override
	public boolean createDB(final String name) {
		// in Mongo DB we do not need to create a DB
		return true;
	}

	@Override
	public boolean deleteDB(final String name) {
		final MongoDatabase database = this.db.getClient().getDatabase(name);
		database.drop();
		return true;
	}

	@Override
	public boolean isTableExist(final String name, final QueryOption... option) throws InternalServerErrorException {
		return true;
	}

	public byte[][] splitIntoGroupsOf16Bytes(final byte[] input) {
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

	public <T> void setValueToDb(
			final Class<?> type,
			final T data,
			final Field field,
			final String fieldName,
			final Document docSet,
			final Document docUnSet) throws Exception {
		if (field.get(data) == null) {
			if (docUnSet != null) {
				docUnSet.append(fieldName, "");
			}
			return;
		}
		if (type == long.class) {
			docSet.append(fieldName, field.getLong(data));
			return;
		}
		if (type == int.class) {
			docSet.append(fieldName, field.getInt(data));
			return;
		}
		if (type == float.class) {
			docSet.append(fieldName, field.getFloat(data));
			return;
		}
		if (type == double.class) {
			docSet.append(fieldName, field.getDouble(data));
			return;
		}
		if (type == boolean.class) {
			docSet.append(fieldName, field.getBoolean(data));
			return;
		}
		final Object tmp = field.get(data);
		if (tmp == null) {
			docUnSet.append(fieldName, "");
			return;
		}
		if (type.isEnum()) {
			docSet.append(fieldName, tmp.toString());
			return;
		}
		if (type == Long.class) {
			docSet.append(fieldName, tmp);
			return;
		}
		if (type == Integer.class) {
			docSet.append(fieldName, tmp);
			return;
		}
		if (type == Float.class) {
			docSet.append(fieldName, tmp);
			return;
		}
		if (type == Double.class) {
			docSet.append(fieldName, tmp);
			return;
		}
		if (type == Boolean.class) {
			docSet.append(fieldName, tmp);
			return;
		}
		if (type == String.class) {
			docSet.append(fieldName, tmp);
			return;
		}
		if (type == Timestamp.class) {
			docSet.append(fieldName, tmp);
			return;
		}
		if (type == ObjectId.class) {
			docSet.append(fieldName, tmp);
			return;
		}
		if (type == UUID.class) {
			docSet.append(fieldName, tmp);
			return;
		}
		if (type == Date.class) {
			/*
				final Timestamp sqlDate = java.sql.Timestamp.from(((Date) tmp).toInstant());
				ps.setTimestamp(iii.value, sqlDate);
			*/
		}
		if (type == Instant.class) {
			/*
				final String sqlDate = ((Instant) tmp).toString();
				ps.setString(iii.value, sqlDate);
			*/
		}
		if (type == LocalDate.class) {
			/*
				final java.sql.Date sqlDate = java.sql.Date.valueOf((LocalDate) tmp);
				ps.setDate(iii.value, sqlDate);
			*/
		}
		if (type == LocalTime.class) {
			/*
				final java.sql.Time sqlDate = java.sql.Time.valueOf((LocalTime) tmp);
				ps.setTime(iii.value, sqlDate);
			*/
		}
		docSet.append(fieldName, tmp);
		//throw new DataAccessException("Unknown Field Type");
	}

	public <T> void setValueFromDoc(
			final Class<?> type,
			final Object data,
			final Field field,
			final Document doc,
			final List<LazyGetter> lazyCall,
			final QueryOptions options) throws Exception {
		final String fieldName = AnnotationTools.getFieldName(field, options).inTable();
		if (!doc.containsKey(fieldName)) {
			field.set(data, null);
			return;
		}
		if (type == UUID.class) {
			final UUID value = doc.get(fieldName, UUID.class);
			field.set(data, value);
			return;
		}
		if (type == ObjectId.class) {
			final ObjectId value = doc.get(fieldName, ObjectId.class);
			field.set(data, value);
			return;
		}
		if (type == Long.class || type == long.class) {
			final Long value = doc.getLong(fieldName);
			field.set(data, value);
			return;
		}
		if (type == Integer.class || type == int.class) {
			final Integer value = doc.getInteger(fieldName);
			field.set(data, value);
			return;
		}
		if (type == Float.class || type == float.class) {
			final Double value = doc.getDouble(fieldName);
			field.set(data, (float) ((double) value));
			return;
		}
		if (type == Double.class || type == double.class) {
			final Double value = doc.getDouble(fieldName);
			field.set(data, value);
			return;
		}
		if (type == Boolean.class || type == boolean.class) {
			final Boolean value = doc.getBoolean(fieldName);
			field.set(data, value);
			return;
		}
		if (type == Timestamp.class) {
			final Date value = doc.get(fieldName, Date.class);
			final Timestamp newData = new Timestamp(value.getTime());
			field.set(data, newData);
			return;
		}
		if (type == Date.class) {
			final Date value = doc.get(fieldName, Date.class);
			field.set(data, value);
			return;
		}
		if (type == Instant.class) {
			final Date value = doc.get(fieldName, Date.class);
			final Instant newData = value.toInstant();
			field.set(data, newData);
			return;
		}
		if (type == LocalDate.class) {
			final Date value = doc.get(fieldName, Date.class);
			final LocalDate newData = value.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
			field.set(data, newData);
			return;
		}
		if (type == LocalTime.class) {
			final Long value = doc.getLong(fieldName);
			final LocalTime newData = LocalTime.ofNanoOfDay(value);
			field.set(data, newData);
			return;
		}
		if (type == String.class) {
			final String value = doc.getString(fieldName);
			field.set(data, value);
			return;
		}
		if (type == UUID.class) {
			final Object value = doc.get(fieldName, field.getType());
			field.set(data, value);
			return;
		}
		if (type.isEnum()) {
			final String value = doc.getString(fieldName);
			boolean find = false;
			final Object[] arr = type.getEnumConstants();
			for (final Object elem : arr) {
				if (elem.toString().equals(value)) {
					field.set(data, elem);
					find = true;
					break;
				}
			}
			if (!find) {
				throw new DataAccessException("Enum value does not exist in the Model: '" + value + "'");
			}
			return;
		}
		if (List.class == field.getType()) {
			final Object value = doc.get(fieldName, field.getType());
			field.set(data, value);
		} else {
			final Object value = createObjectFromDocument(doc.get(fieldName, Document.class), field.getType(),
					new QueryOptions(), lazyCall);
			field.set(data, value);
		}

		return;
		//throw new ArchiveException("wrong type of field [" + fieldName + "]: " + doc.toJson());
	}

	protected Object convertDefaultField(String data, final Field field) throws Exception {
		if (data.startsWith("'") && data.endsWith("'")) {
			data = data.substring(1, data.length() - 1);
		}
		final Class<?> type = field.getType();
		if (type == UUID.class) {

		}
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
		if (type == Timestamp.class) {}
		if (type == Date.class) {}
		if (type == Instant.class) {}
		if (type == LocalDate.class) {}
		if (type == LocalTime.class) {}
		if (type == String.class) {}
		if (type.isEnum()) {
			final boolean find = false;
			final Object[] arr = type.getEnumConstants();
			for (final Object elem : arr) {
				if (elem.toString().equals(data)) {
					return elem;
				}
			}
			if (!find) {
				throw new DataAccessException("Enum value does not exist in the Model: '" + data + "'");
			}
		}
		LOGGER.warn("Request default of unknow native type {} => {}", type.getCanonicalName(), data);
		return null;
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
		final Document updatedCounter = countersCollection.findOneAndUpdate(filter, update, options);

		// Return the new sequence value...
		return updatedCounter.getLong(fieldName);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> Object insertPrimaryKey(final T data, final QueryOption... option) throws Exception {
		final Class<?> clazz = data.getClass();
		final QueryOptions options = new QueryOptions(option);
		final boolean directdata = options.exist(DirectData.class);

		// External checker of data:
		final List<CheckFunction> checks = options.get(CheckFunction.class);
		for (final CheckFunction check : checks) {
			check.getChecker().check(this, "", data, AnnotationTools.getEditableFieldsNames(clazz), options);
		}

		final List<Field> asyncFieldUpdate = new ArrayList<>();
		final String collectionName = AnnotationTools.getTableName(clazz, options);
		Field primaryKeyField = null;
		Object uniqueId = null;
		// real add in the BDD:
		ObjectId insertedId = null;
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
					if (!directdata) {
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
						LOGGER.error("TODO: Manage the ID primary key for type: {}=>{}", clazz.getCanonicalName(),
								primaryKeyField.getType());
						continue;
					} else {
						// Do nothing...
						final Object primaryKeyValue = field.get(data);
						if (primaryKeyValue == null) {
							throw new DataAccessException(
									"Fail to Insert data in DB.. when use 'DirectData.class' you need to provide an ID...");
						}
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
				//doc.append(tableFieldName, currentInsertValue);
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
					setValueToDb(type, data, field, tableFieldName.inTable(), docSet, null);
				}
			}
			final InsertOneResult result = collection.insertOne(docSet);
			// Get the Object of inserted object:
			insertedId = result.getInsertedId().asObjectId().getValue();
			LOGGER.debug("Document inserted with ID: " + insertedId);
			// Rechercher et récupérer le document inséré à partir de son ObjectId
			final Document insertedDocument = collection.find(new Document("_id", insertedId)).first();
			// Afficher le document récupéré
			LOGGER.trace("Inserted document: " + insertedDocument);

		} catch (final Exception ex) {
			LOGGER.error("Fail Mongo request: {}", ex.getMessage());
			ex.printStackTrace();
			throw new DataAccessException("Fail to Insert data in DB : " + ex.getMessage());
		}
		final List<LazyGetter> asyncActions = new ArrayList<>();
		for (final Field field : asyncFieldUpdate) {
			final DataAccessAddOn addOn = findAddOnforField(field);
			if (uniqueId instanceof final Long id) {
				addOn.asyncInsert(this, id, field, field.get(data), asyncActions, options);
			} else if (uniqueId instanceof final UUID uuid) {
				addOn.asyncInsert(this, uuid, field, field.get(data), asyncActions, options);
			} else if (uniqueId instanceof final ObjectId oid) {
				addOn.asyncInsert(this, oid, field, field.get(data), asyncActions, options);
			}
		}
		for (final LazyGetter action : asyncActions) {
			action.doRequest();
		}
		return uniqueId;
	}

	@Override
	public <T> long updateWhere(final T data, QueryOptions options) throws Exception {
		final Class<?> clazz = data.getClass();
		if (options == null) {
			options = new QueryOptions();
		}
		final boolean directdata = options.exist(DirectData.class);
		final Condition condition = conditionFusionOrEmpty(options, true);
		final List<FilterValue> filterKeys = options != null ? options.get(FilterValue.class) : new ArrayList<>();
		if (filterKeys.size() != 1) {
			throw new DataAccessException("request a gets without/or with more 1 filter of values");
		}
		final FilterValue filterKey = filterKeys.get(0);
		// External checker of data:
		if (options != null) {
			final List<CheckFunction> checks = options.get(CheckFunction.class);
			for (final CheckFunction check : checks) {
				check.getChecker().check(this, "", data, filterKey.getValues(), options);
			}
		}
		final List<LazyGetter> asyncActions = new ArrayList<>();

		//Some mode need to get the previous data to perform a correct update...
		boolean needPreviousValues = false;
		for (final Field field : clazz.getFields()) {
			//  field is only for internal global declaration ==> remove it ..
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
				if (!filterKey.getValues().contains(fieldName.inStruct())) {
					continue;
				} else if (AnnotationTools.isGenericField(field)) {
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
					setValueToDb(type, data, field, fieldName.inTable(), docSet, docUnSet);
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
			LOGGER.debug("updateWhere with value: {}", actions.toJson());
			final UpdateResult ret = collection.updateMany(filters, actions);
			for (final LazyGetter action : asyncActions) {
				action.doRequest();
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

	@Override
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

	@Override
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
			FindIterable<Document> retFind = null;
			if (filters != null) {
				//LOGGER.debug("getsWhere Find filter: {}", filters.toBsonDocument().toJson());
				retFind = collection.find(filters);
			} else {
				retFind = collection.find();
			}
			/* Not manage right now ...
			final List<GroupBy> groups = options.get(GroupBy.class);
			for (final GroupBy group : groups) {
				group.generateQuery(query, tableName);
			}
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
					LOGGER.trace("    - getWhere value: {}", doc.toJson());
					final Object data = createObjectFromDocument(doc, clazz, options, lazyCall);
					outs.add(data);
				}
				LOGGER.trace("Async calls: {}", lazyCall.size());
				for (final LazyGetter elem : lazyCall) {
					elem.doRequest();
				}
			}
		} catch (final Exception ex) {
			ex.printStackTrace();
			throw new DataAccessException("Catch an Exception: " + ex.getMessage());
		}
		return outs;
	}

	public Object createObjectFromDocument(
			final Document doc,
			final Class<?> clazz,
			final QueryOptions options,
			final List<LazyGetter> lazyCall) throws Exception {
		final List<OptionSpecifyType> specificTypes = options.get(OptionSpecifyType.class);
		LOGGER.trace("createObjectFromDocument: {}", clazz.getCanonicalName());
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
		for (final Field elem : clazz.getFields()) {
			LOGGER.trace("    Inspect field: name='{}' type='{}'", elem.getName(), elem.getType().getCanonicalName());
			// static field is only for internal global declaration ==> remove it ..
			if (java.lang.reflect.Modifier.isStatic(elem.getModifiers())) {
				LOGGER.trace("        ==> static");
				continue;
			}
			final DataAccessAddOn addOn = findAddOnforField(elem);
			if (addOn != null && !addOn.canRetrieve(elem)) {
				LOGGER.trace("        ==> Can not retreive this field");
				continue;
			}
			final boolean notRead = AnnotationTools.isDefaultNotRead(elem);
			if (!readAllfields && notRead) {
				LOGGER.trace("        ==> Not read this element");
				continue;
			}
			if (addOn != null) {
				addOn.fillFromDoc(this, doc, elem, data, options, lazyCall);
			} else {
				Class<?> type = elem.getType();
				if (type == Object.class) {
					for (final OptionSpecifyType specify : specificTypes) {
						if (specify.name.equals(elem.getName())) {
							type = specify.clazz;
							LOGGER.debug("Detect overwrite of typing var={} ... '{}' => '{}'", elem.getName(),
									elem.getType().getCanonicalName(), specify.clazz.getCanonicalName());
							break;
						}
					}
				}
				setValueFromDoc(type, data, elem, doc, lazyCall, options);
			}
		}
		return data;
	}

	@Override
	public <ID_TYPE> long count(final Class<?> clazz, final ID_TYPE id, final QueryOption... option) throws Exception {
		final QueryOptions options = new QueryOptions(option);
		options.add(new Condition(getTableIdCondition(clazz, id, options)));
		return this.countWhere(clazz, options);
	}

	@Override
	public long countWhere(final Class<?> clazz, final QueryOption... option) throws Exception {
		final QueryOptions options = new QueryOptions(option);
		return countWhere(clazz, options);
	}

	@Override
	public long countWhere(final Class<?> clazz, final QueryOptions options) throws Exception {
		final Condition condition = conditionFusionOrEmpty(options, false);
		final String deletedFieldName = AnnotationTools.getDeletedFieldName(clazz);
		final String collectionName = AnnotationTools.getTableName(clazz, options);
		final MongoCollection<Document> collection = this.db.getDatabase().getCollection(collectionName);
		try {
			// Generate the filtering of the data:
			final Bson filters = condition.getFilter(collectionName, options, deletedFieldName);
			if (filters != null) {
				return collection.countDocuments(filters);
			}
			return collection.countDocuments();
		} catch (final Exception ex) {
			ex.printStackTrace();
			throw new DataAccessException("Catch an Exception: " + ex.getMessage());
		}
	}

	@Override
	public <T, ID_TYPE> T get(final Class<T> clazz, final ID_TYPE id, final QueryOption... option) throws Exception {
		final QueryOptions options = new QueryOptions(option);
		options.add(new Condition(getTableIdCondition(clazz, id, options)));
		return this.getWhere(clazz, options.getAllArray());
	}

	@Override
	public <ID_TYPE> long deleteHard(final Class<?> clazz, final ID_TYPE id, final QueryOption... option)
			throws Exception {
		final QueryOptions options = new QueryOptions(option);
		options.add(new Condition(getTableIdCondition(clazz, id, options)));
		return deleteHardWhere(clazz, options.getAllArray());
	}

	public void actionOnDelete(final Class<?> clazz, final QueryOption... option) throws Exception {

		//Some mode need to get the previous data to perform a correct update...
		boolean needPreviousValues = false;
		final List<Field> hasDeletedActionFields = new ArrayList<>();
		for (final Field field : clazz.getFields()) {
			//  field is only for internal global declaration ==> remove it ..
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
				addOn.onDelete(this, clazz, field, previousData);
			}
		}
	}

	@Override
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
			retFind = collection.deleteMany(filters);
		} else {
			throw new DataAccessException("Too dangerout to delete element with no filter values !!!");
		}
		return retFind.getDeletedCount();
	}

	@Override
	public <ID_TYPE> long deleteSoft(final Class<?> clazz, final ID_TYPE id, final QueryOption... option)
			throws Exception {
		final QueryOptions options = new QueryOptions(option);
		options.add(new Condition(getTableIdCondition(clazz, id, options)));
		return deleteSoftWhere(clazz, options.getAllArray());
	}

	@Override
	public long deleteSoftWhere(final Class<?> clazz, final QueryOption... option) throws Exception {
		final QueryOptions options = new QueryOptions(option);
		final Condition condition = conditionFusionOrEmpty(options, true);
		final String collectionName = AnnotationTools.getTableName(clazz, options);
		final String deletedFieldName = AnnotationTools.getDeletedFieldName(clazz);
		final MongoCollection<Document> collection = this.db.getDatabase().getCollection(collectionName);
		final Bson filters = condition.getFilter(collectionName, options, deletedFieldName);
		final Document actions = new Document("$set", new Document(deletedFieldName, true));
		LOGGER.debug("update some values: {}", actions.toJson());

		actionOnDelete(clazz, option);

		final UpdateResult ret = collection.updateMany(filters, actions);
		return ret.getModifiedCount();
	}

	@Override
	public <ID_TYPE> long unsetDelete(final Class<?> clazz, final ID_TYPE id) throws DataAccessException {
		return unsetDeleteWhere(clazz, new Condition(getTableIdCondition(clazz, id, new QueryOptions())));
	}

	@Override
	public <ID_TYPE> long unsetDelete(final Class<?> clazz, final ID_TYPE id, final QueryOption... option)
			throws DataAccessException {
		final QueryOptions options = new QueryOptions(option);
		options.add(new Condition(getTableIdCondition(clazz, id, options)));
		return unsetDeleteWhere(clazz, options.getAllArray());
	}

	@Override
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
		LOGGER.debug("update some values: {}", actions.toJson());
		final UpdateResult ret = collection.updateMany(filters, actions);
		return ret.getModifiedCount();
	}

	@Override
	public void drop(final Class<?> clazz, final QueryOption... option) throws Exception {
		final QueryOptions options = new QueryOptions(option);
		final String collectionName = AnnotationTools.getTableName(clazz, options);
		final MongoCollection<Document> collection = this.db.getDatabase().getCollection(collectionName);
		collection.drop();
	}

	@Override
	public void cleanAll(final Class<?> clazz, final QueryOption... option) throws Exception {
		final QueryOptions options = new QueryOptions(option);
		final String collectionName = AnnotationTools.getTableName(clazz, options);
		final MongoCollection<Document> collection = this.db.getDatabase().getCollection(collectionName);
		collection.deleteMany(new Document());
	}

}
