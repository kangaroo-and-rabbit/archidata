package org.kar.archidata.dataAccess;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;
import org.kar.archidata.annotation.AnnotationTools;
import org.kar.archidata.annotation.CreationTimestamp;
import org.kar.archidata.annotation.UpdateTimestamp;
import org.kar.archidata.dataAccess.addOnMongo.AddOnDataJson;
import org.kar.archidata.dataAccess.addOnMongo.AddOnManyToMany;
import org.kar.archidata.dataAccess.addOnMongo.AddOnManyToOne;
import org.kar.archidata.dataAccess.addOnMongo.AddOnOneToMany;
import org.kar.archidata.dataAccess.addOnMongo.DataAccessAddOn;
import org.kar.archidata.dataAccess.options.CheckFunction;
import org.kar.archidata.dataAccess.options.Condition;
import org.kar.archidata.dataAccess.options.FilterValue;
import org.kar.archidata.dataAccess.options.Limit;
import org.kar.archidata.dataAccess.options.OrderBy;
import org.kar.archidata.dataAccess.options.QueryOption;
import org.kar.archidata.db.DbInterfaceMorphia;
import org.kar.archidata.exception.DataAccessException;
import org.kar.archidata.tools.DateTools;
import org.kar.archidata.tools.UuidUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;

import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.InternalServerErrorException;

/* TODO list:
   - Manage to group of SQL action to permit to commit only at the end.
 */

/** Data access is an abstraction class that permit to access on the DB with a function wrapping that permit to minimize the SQL writing of SQL code. This interface support the SQL and SQLite
 * back-end. */
public class DataAccessMorphia extends DataAccess {
	static final Logger LOGGER = LoggerFactory.getLogger(DataAccessMorphia.class);
	// by default we manage some add-on that permit to manage non-native model (like json serialization, List of external key as String list...)
	static final List<DataAccessAddOn> addOn = new ArrayList<>();

	static {
		addOn.add(new AddOnManyToMany());
		addOn.add(new AddOnManyToOne());
		addOn.add(new AddOnOneToMany());
		addOn.add(new AddOnDataJson());
	}

	/** Add a new add-on on the current management.
	 * @param addOn instantiate object on the Add-on
	 */
	public static void addAddOn(final DataAccessAddOn addOn) {
		DataAccessMorphia.addOn.add(addOn);
	}

	private final DbInterfaceMorphia db;

	public DataAccessMorphia(final DbInterfaceMorphia db) {
		this.db = db;
	}

	public DbInterfaceMorphia getInterface() {
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
	public void deleteDB(final String name) {
		final MongoDatabase database = this.db.getClient().getDatabase(name);
		database.drop();
	}

	@Override
	public boolean isTableExist(final String name, final QueryOption... option) throws InternalServerErrorException {
		return true;
	}

	/** Extract a list of Long with "-" separated element from a SQL input data.
	 * @param rs Result Set of the BDD
	 * @param iii Id in the result set
	 * @return The list of Long value
	 * @throws SQLException if an error is generated in the SQL request. */
	public List<Long> getListOfIds(final ResultSet rs, final int iii, final String separator) throws SQLException {
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
	public List<UUID> getListOfUUIDs(final ResultSet rs, final int iii, final String separator) throws SQLException {
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

	public List<UUID> getListOfRawUUIDs(final ResultSet rs, final int iii) throws SQLException, DataAccessException {
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

	public UUID getListOfRawUUID(final ResultSet rs, final int iii) throws SQLException, DataAccessException {
		final byte[] elem = rs.getBytes(iii);
		if (rs.wasNull()) {
			return null;
		}
		return UuidUtils.asUuid(elem);
	}

	protected <T> void setValuedb(
			final Class<?> type,
			final T data,
			final Field field,
			final String fieldName,
			final Document docSet,
			final Document docUnSet) throws Exception {
		if (field.get(data) == null) {
			docUnSet.append(fieldName, "");
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
		if (type == Double.class) {
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
		if (type == UUID.class) {
			docSet.append(fieldName, tmp);
			return;
		}
		if (type == Date.class) {
			// TODO ...
			/*
			final Object tmp = field.get(data);
			if (tmp == null) {
				ps.setNull(iii.value, Types.INTEGER);
			} else {
				final Timestamp sqlDate = java.sql.Timestamp.from(((Date) tmp).toInstant());
				ps.setTimestamp(iii.value, sqlDate);
			}*/
		}
		if (type == Instant.class) {
			/*
			final Object tmp = field.get(data);
			if (tmp == null) {
				ps.setNull(iii.value, Types.INTEGER);
			} else {
				final String sqlDate = ((Instant) tmp).toString();
				ps.setString(iii.value, sqlDate);
			}
			*/
		}
		if (type == LocalDate.class) {
			/*
			final Object tmp = field.get(data);
			if (tmp == null) {
				ps.setNull(iii.value, Types.INTEGER);
			} else {
				final java.sql.Date sqlDate = java.sql.Date.valueOf((LocalDate) tmp);
				ps.setDate(iii.value, sqlDate);
			}
			*/
		}
		if (type == LocalTime.class) {
			/*
			final Object tmp = field.get(data);
			if (tmp == null) {
				ps.setNull(iii.value, Types.INTEGER);
			} else {
				final java.sql.Time sqlDate = java.sql.Time.valueOf((LocalTime) tmp);
				ps.setTime(iii.value, sqlDate);
			}
			*/
		}
		throw new DataAccessException("Unknown Field Type");

	}

	protected <T> void setValueFromDoc(
			final Class<?> type,
			final Object data,
			final CountInOut count,
			final Field field,
			final Document doc,
			final CountInOut countNotNull) throws Exception {
		final String fieldName = AnnotationTools.getFieldName(field);
		if (!doc.containsKey(fieldName)) {
			field.set(data, null);
			return;
		}
		if (type == UUID.class) {
			final UUID value = doc.get(fieldName, UUID.class);
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
		if (type.isEnum()) {
			final String value = doc.getString(fieldName);
			boolean find = false;
			final Object[] arr = type.getEnumConstants();
			for (final Object elem : arr) {
				if (elem.toString().equals(value)) {
					field.set(data, elem);
					countNotNull.inc();
					find = true;
					break;
				}
			}
			if (!find) {
				throw new DataAccessException("Enum value does not exist in the Model: '" + value + "'");
			}
			return;
		}
		final Object value = doc.get(fieldName, field.getType());
		field.set(data, value);
		return;
		//throw new ArchiveException("wrong type of field [" + fieldName + "]: " + doc.toJson());
	}

	// TODO: this function will replace the previous one !!!
	protected RetreiveFromDB createSetValueFromDbCallback(final int count, final Field field) throws Exception {
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
		final MongoCollection<Document> countersCollection = this.db.getDatastore().getDatabase()
				.getCollection("counters");

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
	public <T> T insert(final T data, final QueryOption... option) throws Exception {
		final Class<?> clazz = data.getClass();
		final QueryOptions options = new QueryOptions(option);

		// External checker of data:
		final List<CheckFunction> checks = options.get(CheckFunction.class);
		for (final CheckFunction check : checks) {
			check.getChecker().check(this, "", data, AnnotationTools.getFieldsNames(clazz), options);
		}

		final List<Field> asyncFieldUpdate = new ArrayList<>();
		final String collectionName = AnnotationTools.getCollectionName(clazz, options);
		Field primaryKeyField = null;
		Object uniqueId = null;
		// real add in the BDD:
		ObjectId insertedId = null;
		try {
			final MongoCollection<Document> collection = this.db.getDatastore().getDatabase()
					.getCollection(collectionName);
			final Document doc = new Document();
			for (final Field field : clazz.getFields()) {
				// static field is only for internal global declaration ==> remove it ..
				if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
					continue;
				}
				final String tableFieldName = AnnotationTools.getFieldName(field);
				final Object currentInsertValue = field.get(data);
				if (AnnotationTools.isPrimaryKey(field)) {
					primaryKeyField = field;
					if (primaryKeyField.getType() == UUID.class) {
						final UUID uuid = UuidUtils.nextUUID();
						uniqueId = uuid;
						doc.append(tableFieldName, uuid);
						continue;
					} else if (primaryKeyField.getType() == Long.class || primaryKeyField.getType() == long.class) {
						// By default the MongoDB does not manage the
						final long id = getNextSequenceLongValue(collectionName, tableFieldName);
						uniqueId = id;
						doc.append(tableFieldName, id);
						continue;
					}
					LOGGER.error("TODO: Manage the ID primary key for type: ");
					continue;
				}
				final DataAccessAddOn addOn = findAddOnforField(field);
				if (addOn != null && !addOn.canInsert(field)) {
					if (addOn.isInsertAsync(field)) {
						LOGGER.error("TODO: add async objects ...");
						//asyncFieldUpdate.add(field);
					}
					continue;
				}
				final boolean createTime = field.getDeclaredAnnotationsByType(CreationTimestamp.class).length != 0;
				if (createTime) {
					doc.append(tableFieldName, Date.from(Instant.now()));
					continue;
				}
				final boolean updateTime = field.getDeclaredAnnotationsByType(UpdateTimestamp.class).length != 0;
				if (updateTime) {
					doc.append(tableFieldName, Date.from(Instant.now()));
					continue;
				}
				if (!field.getClass().isPrimitive()) {
					if (currentInsertValue == null) {
						final DefaultValue[] defaultValue = field.getDeclaredAnnotationsByType(DefaultValue.class);
						LOGGER.error("TODO: convert default value in the correct value for the DB...");
						continue;
					}
				}
				doc.append(tableFieldName, currentInsertValue);
			}
			final InsertOneResult result = collection.insertOne(doc);
			// Get the Object of inserted object:
			insertedId = result.getInsertedId().asObjectId().getValue();
			LOGGER.info("Document inserted with ID: " + insertedId);

			// Rechercher et récupérer le document inséré à partir de son ObjectId
			final Document insertedDocument = collection.find(new Document("_id", insertedId)).first();

			// Afficher le document récupéré
			System.out.println("Inserted document: " + insertedDocument);

		} catch (final Exception ex) {
			LOGGER.error("Fail SQL request: {}", ex.getMessage());
			ex.printStackTrace();
			throw new DataAccessException("Fail to Insert data in DB : " + ex.getMessage());
		}
		final List<LazyGetter> asyncActions = new ArrayList<>();
		for (final Field field : asyncFieldUpdate) {
			final DataAccessAddOn addOn = findAddOnforField(field);
			if (uniqueId instanceof final Long id) {
				LOGGER.error("TODO: Add on not managed ... ");
				//addOn.asyncInsert(tableName, id, field, field.get(data), asyncActions);
			} else if (uniqueId instanceof final UUID uuid) {
				LOGGER.error("TODO: Add on not managed ... ");
				//addOn.asyncInsert(tableName, uuid, field, field.get(data), asyncActions);
			}
		}
		for (final LazyGetter action : asyncActions) {
			action.doRequest();
		}
		return (T) getWhere(clazz, new Condition(new QueryCondition("_id", "=", insertedId)));
	}

	@Override
	public <T> long updateWhere(final T data, QueryOptions options) throws Exception {
		final Class<?> clazz = data.getClass();
		if (options == null) {
			options = new QueryOptions();
		}
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

		// real add in the BDD:
		try {
			final String collectionName = AnnotationTools.getCollectionName(clazz, options);

			final String deletedFieldName = AnnotationTools.getDeletedFieldName(clazz);
			final Bson filters = condition.getFilter(collectionName, options, deletedFieldName);
			final Document docSet = new Document();
			final Document docUnSet = new Document();
			for (final Field field : clazz.getFields()) {
				// static field is only for internal global declaration ==> remove it ..
				if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
					continue;
				}
				final String name = AnnotationTools.getFieldName(field);
				if (!filterKey.getValues().contains(name)) {
					continue;
				} else if (AnnotationTools.isGenericField(field)) {
					continue;
				}
				final DataAccessAddOn addOn = findAddOnforField(field);
				if (addOn != null && !addOn.canInsert(field)) {
					if (addOn.isInsertAsync(field)) {
						LOGGER.error("TODO: Add on not managed ... ");
						/*
						final List<TransmitKey> transmitKey = options.get(TransmitKey.class);
						if (transmitKey.size() != 1) {
							throw new DataAccessException(
									"Fail to transmit Key to update the async update... (must have only 1)");
						}
						addOn.asyncUpdate(tableName, transmitKey.get(0).getKey(), field, field.get(data), asyncActions);
						*/
					}
					continue;
				}
				if (addOn != null) {
					LOGGER.error("TODO: Add on not managed ... ");
					//addOn.insertData(ps, field, data, iii);
				} else {
					final Class<?> type = field.getType();
					if (!type.isPrimitive()) {
						final Object tmp = field.get(data);
						if (tmp == null && field.getDeclaredAnnotationsByType(DefaultValue.class).length != 0) {
							continue;
						}
					}
					setValuedb(type, data, field, name, docSet, docUnSet);
					/*
					if (!field.getClass().isPrimitive()) {
						final Object tmp = field.get(data);
						if (tmp != null) {
							docSet.append(name, tmp);
						} else {
							docUnSet.append(name, null);
						}
					}
					*/
				}

			}
			// Do the query ...

			final MongoCollection<Document> collection = this.db.getDatastore().getDatabase()
					.getCollection(collectionName);
			final Document actions = new Document();
			if (!docSet.isEmpty()) {
				actions.append("$set", docSet);
			}
			if (!docUnSet.isEmpty()) {
				actions.append("$unset", docUnSet);
			}
			LOGGER.info("update some values: {}", actions.toJson());
			final UpdateResult ret = collection.updateMany(filters, actions);
			return ret.getModifiedCount();
		} catch (final SQLException ex) {
			ex.printStackTrace();
		}
		for (final LazyGetter action : asyncActions) {
			action.doRequest();
		}
		return 0;
	}

	@Override
	public void addElement(final PreparedStatement ps, final Object value, final CountInOut iii) throws Exception {
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

	// This must be refactored to manage the .project of Morphia.
	public void generateSelectField(//
			final StringBuilder querySelect, //
			final StringBuilder query, //
			final Class<?> clazz, //
			final QueryOptions options, //
			final CountInOut count//
	) throws Exception {
		/*
		final boolean readAllfields = QueryOptions.readAllColomn(options);
		final String collectionName = AnnotationTools.getCollectionName(clazz, options);
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
				LOGGER.error("TODO: Add on not managed ... ");
				//addOn.generateQuery(tableName, primaryKey, elem, querySelect, query, name, count, options);
			} else {
				querySelect.append(tableName);
				querySelect.append(".");
				querySelect.append(name);
				count.inc();
			}
		}
		*/
	}

	@Override
	public <T> List<T> getsWhere(final Class<T> clazz, final QueryOption... option) throws Exception {
		final QueryOptions options = new QueryOptions(option);
		return getsWhere(clazz, options);
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
	@SuppressWarnings("unchecked")
	public <T> List<T> getsWhere(final Class<T> clazz, final QueryOptions options)
			throws DataAccessException, IOException {

		final Condition condition = conditionFusionOrEmpty(options, false);
		final List<LazyGetter> lazyCall = new ArrayList<>();
		final String deletedFieldName = AnnotationTools.getDeletedFieldName(clazz);
		final String collectionName = AnnotationTools.getCollectionName(clazz, options);
		final List<T> outs = new ArrayList<>();
		final MongoCollection<Document> collection = this.db.getDatastore().getDatabase().getCollection(collectionName);
		try {
			final CountInOut count = new CountInOut();
			// Select values to read
			//generateSelectField(querySelect, query, clazz, options, count);
			// Generate the filtering of the data:
			final Bson filters = condition.getFilter(collectionName, options, deletedFieldName);
			FindIterable<Document> retFind = null;
			if (filters != null) {
				LOGGER.info("getsWhere Find filter: {}", filters.toBsonDocument().toJson());
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

			final MongoCursor<Document> cursor = retFind.iterator();
			try (cursor) {
				while (cursor.hasNext()) {
					final Document doc = cursor.next();
					count.value = 1;
					final CountInOut countNotNull = new CountInOut(0);
					System.out.println(doc.toJson()); // Affichage du document en format JSON
					final Object data = createObjectFromDocument(doc, clazz, count, countNotNull, options, lazyCall);
					final T out = (T) data;
					outs.add(out);
				}
				LOGGER.info("Async calls: {}", lazyCall.size());
				for (final LazyGetter elem : lazyCall) {
					elem.doRequest();
				}
			}
		} catch (final SQLException ex) {
			ex.printStackTrace();
			throw new DataAccessException("Catch a SQL Exception: " + ex.getMessage());
		} catch (final Exception ex) {
			ex.printStackTrace();
			throw new DataAccessException("Catch an Exception: " + ex.getMessage());
		}
		return outs;
	}

	public Object createObjectFromDocument(
			final Document doc,
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
				break;
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
				LOGGER.error("TODO: Add on not managed ... ");
				//addOn.fillFromDoc(doc, elem, data, count, options, lazyCall);
				//addOn.fillFromQuery(rs, elem, data, count, options, lazyCall);
			} else {
				setValueFromDoc(elem.getType(), data, count, elem, doc, countNotNull);
				//setValueFromDb(elem.getType(), data, count, elem, rs, countNotNull);
			}
		}
		return data;
	}

	@Override
	public <ID_TYPE> long count(final Class<?> clazz, final ID_TYPE id, final QueryOption... option) throws Exception {
		final QueryOptions options = new QueryOptions(option);
		options.add(new Condition(getTableIdCondition(clazz, id)));
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
		final String collectionName = AnnotationTools.getCollectionName(clazz, options);
		final MongoCollection<Document> collection = this.db.getDatastore().getDatabase().getCollection(collectionName);
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
		options.add(new Condition(getTableIdCondition(clazz, id)));
		return this.getWhere(clazz, options.getAllArray());
	}

	@Override
	public <T> List<T> gets(final Class<T> clazz) throws Exception {
		return getsWhere(clazz);
	}

	@Override
	public <T> List<T> gets(final Class<T> clazz, final QueryOption... option) throws Exception {
		return getsWhere(clazz, option);
	}

	/** Delete items with the specific Id (cf @Id) and some options. If the Entity is manage as a softDeleted model, then it is flag as removed (if not already done before).
	 * @param <ID_TYPE> Type of the reference @Id
	 * @param clazz Data model that might remove element
	 * @param id Unique Id of the model
	 * @param options (Optional) Options of the request
	 * @return Number of element that is removed. */
	@Override
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
	 * @param condition Condition to remove elements.
	 * @param options (Optional) Options of the request.
	 * @return Number of element that is removed. */
	@Override
	public long deleteWhere(final Class<?> clazz, final QueryOption... option) throws Exception {
		final String hasDeletedFieldName = AnnotationTools.getDeletedFieldName(clazz);
		if (hasDeletedFieldName != null) {
			return deleteSoftWhere(clazz, option);
		} else {
			return deleteHardWhere(clazz, option);
		}
	}

	@Override
	public <ID_TYPE> long deleteHard(final Class<?> clazz, final ID_TYPE id, final QueryOption... option)
			throws Exception {
		final QueryOptions options = new QueryOptions(option);
		options.add(new Condition(getTableIdCondition(clazz, id)));
		return deleteHardWhere(clazz, options.getAllArray());
	}

	@Override
	public long deleteHardWhere(final Class<?> clazz, final QueryOption... option) throws Exception {
		final QueryOptions options = new QueryOptions(option);
		final Condition condition = conditionFusionOrEmpty(options, true);
		final String collectionName = AnnotationTools.getCollectionName(clazz, options);
		final String deletedFieldName = AnnotationTools.getDeletedFieldName(clazz);
		final MongoCollection<Document> collection = this.db.getDatastore().getDatabase().getCollection(collectionName);
		final Bson filters = condition.getFilter(collectionName, options, deletedFieldName);
		DeleteResult retFind;
		if (filters != null) {
			retFind = collection.deleteMany(filters);
		} else {
			throw new DataAccessException("Too dangerout to delete element with no filter values !!!");
		}
		return retFind.getDeletedCount();
	}

	private <ID_TYPE> long deleteSoft(final Class<?> clazz, final ID_TYPE id, final QueryOption... option)
			throws Exception {
		final QueryOptions options = new QueryOptions(option);
		options.add(new Condition(getTableIdCondition(clazz, id)));
		return deleteSoftWhere(clazz, options.getAllArray());
	}

	@Override
	public long deleteSoftWhere(final Class<?> clazz, final QueryOption... option) throws Exception {
		final QueryOptions options = new QueryOptions(option);
		final Condition condition = conditionFusionOrEmpty(options, true);
		final String collectionName = AnnotationTools.getCollectionName(clazz, options);
		final String deletedFieldName = AnnotationTools.getDeletedFieldName(clazz);
		final MongoCollection<Document> collection = this.db.getDatastore().getDatabase().getCollection(collectionName);
		final Bson filters = condition.getFilter(collectionName, options, deletedFieldName);
		final Document actions = new Document("$set", new Document(deletedFieldName, true));
		LOGGER.info("update some values: {}", actions.toJson());
		final UpdateResult ret = collection.updateMany(filters, actions);
		return ret.getModifiedCount();
	}

	@Override
	public <ID_TYPE> long unsetDelete(final Class<?> clazz, final ID_TYPE id) throws DataAccessException {
		return unsetDeleteWhere(clazz, new Condition(getTableIdCondition(clazz, id)));
	}

	@Override
	public <ID_TYPE> long unsetDelete(final Class<?> clazz, final ID_TYPE id, final QueryOption... option)
			throws DataAccessException {
		final QueryOptions options = new QueryOptions(option);
		options.add(new Condition(getTableIdCondition(clazz, id)));
		return unsetDeleteWhere(clazz, options.getAllArray());
	}

	@Override
	public long unsetDeleteWhere(final Class<?> clazz, final QueryOption... option) throws DataAccessException {
		final QueryOptions options = new QueryOptions(option);
		final Condition condition = conditionFusionOrEmpty(options, true);
		final String collectionName = AnnotationTools.getCollectionName(clazz, options);
		final String deletedFieldName = AnnotationTools.getDeletedFieldName(clazz);
		if (deletedFieldName == null) {
			throw new DataAccessException("The class " + clazz.getCanonicalName() + " has no deleted field");
		}
		final MongoCollection<Document> collection = this.db.getDatastore().getDatabase().getCollection(collectionName);
		final Bson filters = condition.getFilter(collectionName, options, deletedFieldName);
		final Document actions = new Document("$set", new Document(deletedFieldName, false));
		LOGGER.info("update some values: {}", actions.toJson());
		final UpdateResult ret = collection.updateMany(filters, actions);
		return ret.getModifiedCount();
	}

	@Override
	public void drop(final Class<?> clazz, final QueryOption... option) throws Exception {
		final QueryOptions options = new QueryOptions(option);
		final String collectionName = AnnotationTools.getCollectionName(clazz, options);
		final MongoCollection<Document> collection = this.db.getDatastore().getDatabase().getCollection(collectionName);
		collection.drop();
	}

	@Override
	public void cleanAll(final Class<?> clazz, final QueryOption... option) throws Exception {
		final QueryOptions options = new QueryOptions(option);
		final String collectionName = AnnotationTools.getCollectionName(clazz, options);
		final MongoCollection<Document> collection = this.db.getDatastore().getDatabase().getCollection(collectionName);
		collection.deleteMany(new Document());
	}
}
