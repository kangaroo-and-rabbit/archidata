package org.atriasoft.archidata.dataAccess.mongo;

import java.util.Date;
import java.util.List;

import org.atriasoft.archidata.dataAccess.DBAccessMongo;
import org.atriasoft.archidata.dataAccess.model.DbClassModel;
import org.atriasoft.archidata.dataAccess.model.DbPropertyDescriptor;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

/**
 * Direct MongoDB operations for managing links between documents.
 *
 * <p>Provides atomic MongoDB operations ({@code $addToSet}, {@code $pull},
 * {@code $set}, {@code $unset}, {@code findOneAndUpdate}) for link management.
 *
 * <p>Used by {@code ManyToManyTools}, {@code ListInDbTools}, {@code FieldTools},
 * and the AddOn classes for single-round-trip link operations.
 */
public final class MongoLinkManager {
	private static final Logger LOGGER = LoggerFactory.getLogger(MongoLinkManager.class);

	private MongoLinkManager() {}

	// ========== List operations (for ManyToMany / OneToMany arrays) ==========

	/**
	 * Atomically add a value to an array field if not already present.
	 * Also updates the updateAt timestamp if the class has one.
	 *
	 * <p>Uses MongoDB {@code $addToSet} for atomic, duplicate-free addition.
	 *
	 * @param ioDb the DB access instance
	 * @param clazz the target entity class
	 * @param primaryKeyValue the primary key of the document to update
	 * @param fieldColumnName the DB column name of the array field
	 * @param valueToAdd the value to add to the array
	 */
	public static void addToList(
			final DBAccessMongo ioDb,
			final Class<?> clazz,
			final Object primaryKeyValue,
			final String fieldColumnName,
			final Object valueToAdd) throws Exception {
		final DbClassModel model = DbClassModel.of(clazz);
		final String collectionName = model.getTableName();
		final String pkColumn = model.getPrimaryKey().getDbFieldName();

		final MongoCollection<Document> collection = ioDb.getInterface().getDatabase()
				.getCollection(collectionName);

		final Bson filter = Filters.eq(pkColumn, primaryKeyValue);
		Bson update = Updates.addToSet(fieldColumnName, valueToAdd);

		// Also update the updateAt timestamp if present
		final DbPropertyDescriptor updateTs = model.getUpdateTimestamp();
		if (updateTs != null) {
			update = Updates.combine(update,
					Updates.set(updateTs.getDbFieldName(), new Date()));
		}

		collection.updateOne(filter, update);
		LOGGER.trace("addToList: {}.{} += {} (pk={})", collectionName, fieldColumnName,
				valueToAdd, primaryKeyValue);
	}

	/**
	 * Atomically remove a value from an array field.
	 * Also updates the updateAt timestamp if the class has one.
	 *
	 * <p>Uses MongoDB {@code $pull} for atomic removal.
	 *
	 * @param ioDb the DB access instance
	 * @param clazz the target entity class
	 * @param primaryKeyValue the primary key of the document to update
	 * @param fieldColumnName the DB column name of the array field
	 * @param valueToRemove the value to remove from the array
	 */
	public static void removeFromList(
			final DBAccessMongo ioDb,
			final Class<?> clazz,
			final Object primaryKeyValue,
			final String fieldColumnName,
			final Object valueToRemove) throws Exception {
		final DbClassModel model = DbClassModel.of(clazz);
		final String collectionName = model.getTableName();
		final String pkColumn = model.getPrimaryKey().getDbFieldName();

		final MongoCollection<Document> collection = ioDb.getInterface().getDatabase()
				.getCollection(collectionName);

		final Bson filter = Filters.eq(pkColumn, primaryKeyValue);
		Bson update = Updates.pull(fieldColumnName, valueToRemove);

		final DbPropertyDescriptor updateTs = model.getUpdateTimestamp();
		if (updateTs != null) {
			update = Updates.combine(update,
					Updates.set(updateTs.getDbFieldName(), new Date()));
		}

		collection.updateOne(filter, update);
		LOGGER.trace("removeFromList: {}.{} -= {} (pk={})", collectionName, fieldColumnName,
				valueToRemove, primaryKeyValue);
	}

	// ========== Scalar operations (for ManyToOne / OneToMany single values) ==========

	/**
	 * Atomically set a scalar field value on a document.
	 * Also updates the updateAt timestamp if the class has one.
	 *
	 * <p>Uses MongoDB {@code $set} for atomic field update.
	 *
	 * @param ioDb the DB access instance
	 * @param clazz the target entity class
	 * @param primaryKeyValue the primary key of the document to update
	 * @param fieldColumnName the DB column name of the field
	 * @param value the value to set (null will use $unset)
	 */
	public static void setField(
			final DBAccessMongo ioDb,
			final Class<?> clazz,
			final Object primaryKeyValue,
			final String fieldColumnName,
			final Object value) throws Exception {
		final DbClassModel model = DbClassModel.of(clazz);
		final String collectionName = model.getTableName();
		final String pkColumn = model.getPrimaryKey().getDbFieldName();

		final MongoCollection<Document> collection = ioDb.getInterface().getDatabase()
				.getCollection(collectionName);

		final Bson filter = Filters.eq(pkColumn, primaryKeyValue);
		Bson update;
		if (value != null) {
			update = Updates.set(fieldColumnName, value);
		} else {
			update = Updates.unset(fieldColumnName);
		}

		final DbPropertyDescriptor updateTs = model.getUpdateTimestamp();
		if (updateTs != null) {
			update = Updates.combine(update,
					Updates.set(updateTs.getDbFieldName(), new Date()));
		}

		collection.updateOne(filter, update);
		LOGGER.trace("setField: {}.{} = {} (pk={})", collectionName, fieldColumnName,
				value, primaryKeyValue);
	}

	/**
	 * Atomically set a scalar field to a new value and return the previous value.
	 * Uses MongoDB {@code findOneAndUpdate} for atomic read+write.
	 *
	 * @param ioDb the DB access instance
	 * @param clazz the target entity class
	 * @param primaryKeyValue the primary key of the document to update
	 * @param fieldColumnName the DB column name of the field
	 * @param newValue the new value to set
	 * @return the previous value, or null if not set
	 */
	public static Object setFieldAndGetPrevious(
			final DBAccessMongo ioDb,
			final Class<?> clazz,
			final Object primaryKeyValue,
			final String fieldColumnName,
			final Object newValue) throws Exception {
		final DbClassModel model = DbClassModel.of(clazz);
		final String collectionName = model.getTableName();
		final String pkColumn = model.getPrimaryKey().getDbFieldName();

		final MongoCollection<Document> collection = ioDb.getInterface().getDatabase()
				.getCollection(collectionName);

		final Bson filter = Filters.eq(pkColumn, primaryKeyValue);
		Bson update;
		if (newValue != null) {
			update = Updates.set(fieldColumnName, newValue);
		} else {
			update = Updates.unset(fieldColumnName);
		}

		final DbPropertyDescriptor updateTs = model.getUpdateTimestamp();
		if (updateTs != null) {
			update = Updates.combine(update,
					Updates.set(updateTs.getDbFieldName(), new Date()));
		}

		// findOneAndUpdate returns the document BEFORE the update by default
		final Document previousDoc = collection.findOneAndUpdate(filter, update);
		if (previousDoc == null) {
			return null;
		}
		final Object previousValue = previousDoc.get(fieldColumnName);
		if (previousValue != null && previousValue.equals(newValue)) {
			// Value unchanged, return null to indicate no change
			return null;
		}
		return previousValue;
	}

	/**
	 * Atomically set a field to null (unset) on a document found by a non-PK filter.
	 * Also updates the updateAt timestamp.
	 *
	 * <p>Used when resetting remote fields (e.g., OneToMany SET_NULL on delete).
	 *
	 * @param ioDb the DB access instance
	 * @param clazz the target entity class
	 * @param filterFieldName the DB column name to filter on
	 * @param filterValue the value to match
	 * @param fieldToNullify the DB column name of the field to set to null
	 */
	public static void setFieldToNullWhere(
			final DBAccessMongo ioDb,
			final Class<?> clazz,
			final String filterFieldName,
			final Object filterValue,
			final String fieldToNullify) throws Exception {
		final DbClassModel model = DbClassModel.of(clazz);
		final String collectionName = model.getTableName();

		final MongoCollection<Document> collection = ioDb.getInterface().getDatabase()
				.getCollection(collectionName);

		final Bson filter = filterFieldName != null
				? Filters.eq(filterFieldName, filterValue)
				: new Document();
		Bson update = Updates.unset(fieldToNullify);

		final DbPropertyDescriptor updateTs = model.getUpdateTimestamp();
		if (updateTs != null) {
			update = Updates.combine(update,
					Updates.set(updateTs.getDbFieldName(), new Date()));
		}

		// Use updateMany in case multiple documents match
		collection.updateMany(filter, update);
		LOGGER.trace("setFieldToNullWhere: {}.{} where {}={}", collectionName,
				fieldToNullify, filterFieldName, filterValue);
	}

	// ========== Batch operations ==========

	/**
	 * Atomically add multiple values to an array field.
	 * Uses MongoDB {@code $addToSet} with {@code $each} for efficient batch addition.
	 *
	 * @param ioDb the DB access instance
	 * @param clazz the target entity class
	 * @param primaryKeyValue the primary key of the document to update
	 * @param fieldColumnName the DB column name of the array field
	 * @param valuesToAdd the values to add to the array
	 */
	public static void addAllToList(
			final DBAccessMongo ioDb,
			final Class<?> clazz,
			final Object primaryKeyValue,
			final String fieldColumnName,
			final List<?> valuesToAdd) throws Exception {
		if (valuesToAdd == null || valuesToAdd.isEmpty()) {
			return;
		}
		final DbClassModel model = DbClassModel.of(clazz);
		final String collectionName = model.getTableName();
		final String pkColumn = model.getPrimaryKey().getDbFieldName();

		final MongoCollection<Document> collection = ioDb.getInterface().getDatabase()
				.getCollection(collectionName);

		final Bson filter = Filters.eq(pkColumn, primaryKeyValue);
		Bson update = Updates.addEachToSet(fieldColumnName, valuesToAdd);

		final DbPropertyDescriptor updateTs = model.getUpdateTimestamp();
		if (updateTs != null) {
			update = Updates.combine(update,
					Updates.set(updateTs.getDbFieldName(), new Date()));
		}

		collection.updateOne(filter, update);
		LOGGER.trace("addAllToList: {}.{} += {} values (pk={})", collectionName,
				fieldColumnName, valuesToAdd.size(), primaryKeyValue);
	}

	/**
	 * Atomically remove multiple values from an array field.
	 * Uses MongoDB {@code $pullAll} for efficient batch removal.
	 *
	 * @param ioDb the DB access instance
	 * @param clazz the target entity class
	 * @param primaryKeyValue the primary key of the document to update
	 * @param fieldColumnName the DB column name of the array field
	 * @param valuesToRemove the values to remove from the array
	 */
	public static void removeAllFromList(
			final DBAccessMongo ioDb,
			final Class<?> clazz,
			final Object primaryKeyValue,
			final String fieldColumnName,
			final List<?> valuesToRemove) throws Exception {
		if (valuesToRemove == null || valuesToRemove.isEmpty()) {
			return;
		}
		final DbClassModel model = DbClassModel.of(clazz);
		final String collectionName = model.getTableName();
		final String pkColumn = model.getPrimaryKey().getDbFieldName();

		final MongoCollection<Document> collection = ioDb.getInterface().getDatabase()
				.getCollection(collectionName);

		final Bson filter = Filters.eq(pkColumn, primaryKeyValue);
		Bson update = Updates.pullAll(fieldColumnName, valuesToRemove);

		final DbPropertyDescriptor updateTs = model.getUpdateTimestamp();
		if (updateTs != null) {
			update = Updates.combine(update,
					Updates.set(updateTs.getDbFieldName(), new Date()));
		}

		collection.updateOne(filter, update);
		LOGGER.trace("removeAllFromList: {}.{} -= {} values (pk={})", collectionName,
				fieldColumnName, valuesToRemove.size(), primaryKeyValue);
	}
}
