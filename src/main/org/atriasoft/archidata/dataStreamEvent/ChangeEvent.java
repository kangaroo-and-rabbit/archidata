package org.atriasoft.archidata.dataStreamEvent;

import java.time.Instant;
import java.util.Collections;
import java.util.Set;

import org.bson.BsonDocument;
import org.bson.Document;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;

import com.mongodb.client.model.changestream.OperationType;
import com.mongodb.client.model.changestream.UpdateDescription;

/**
 * Represents a MongoDB change event with all relevant metadata. This class
 * wraps MongoDB's ChangeStreamDocument and provides convenient helper methods
 * for accessing change stream data.
 *
 * <p>
 * Change events are created when MongoDB detects a change in a watched
 * collection. They contain information about the operation type (INSERT,
 * UPDATE, DELETE, etc.), the affected document, and metadata like timestamps
 * and resume tokens.
 * </p>
 *
 * <p>
 * The availability of the full document depends on the FullDocument mode:
 * </p>
 * <ul>
 * <li>DEFAULT: Full document available for INSERT operations only</li>
 * <li>UPDATE_LOOKUP: Full document available for INSERT and UPDATE
 * operations</li>
 * <li>WHEN_AVAILABLE: Full document when available</li>
 * </ul>
 *
 * @see ChangeNotificationManager
 * @see ChangeNotificationListener
 */
public class ChangeEvent {
	private final OperationType operationType;
	private final String collectionName;
	private final String entityName;
	private final Object oid;
	private final Document fullDocument;
	private final UpdateDescription updateDescription;
	private final Instant timestamp;
	private final BsonDocument resumeToken;

	/**
	 * Constructs a new ChangeEvent with all metadata.
	 *
	 * @param operationType The type of operation (INSERT, UPDATE, DELETE, etc.)
	 * @param collectionName The name of the collection where the change occurred
	 * @param entityName The entity name (typically same as collection name)
	 * @param oid The ObjectId of the affected document
	 * @param fullDocument The full document after the change (may be null
	 *            depending on FullDocument mode)
	 * @param updateDescription Description of updated/removed fields (for UPDATE
	 *            operations)
	 * @param timestamp The timestamp when the change occurred
	 * @param resumeToken Token that can be used to resume the change stream from
	 *            this point
	 */
	public ChangeEvent(final OperationType operationType, final String collectionName, final String entityName,
			final Object oid, final Document fullDocument, final UpdateDescription updateDescription,
			final Instant timestamp, final BsonDocument resumeToken) {
		this.operationType = operationType;
		this.collectionName = collectionName;
		this.entityName = entityName;
		this.oid = oid;
		this.fullDocument = fullDocument;
		this.updateDescription = updateDescription;
		this.timestamp = timestamp;
		this.resumeToken = resumeToken;
	}

	// Getters

	/**
	 * Gets the type of operation that triggered this change event.
	 *
	 * @return The operation type (INSERT, UPDATE, DELETE, REPLACE, etc.)
	 */
	public OperationType getOperationType() {
		return this.operationType;
	}

	/**
	 * Gets the name of the collection where the change occurred.
	 *
	 * @return The collection name
	 */
	public String getCollectionName() {
		return this.collectionName;
	}

	/**
	 * Gets the entity name associated with this change.
	 *
	 * @return The entity name (typically same as collection name)
	 */
	public String getEntityName() {
		return this.entityName;
	}

	/**
	 * Gets the ObjectId (_id field) of the affected document.
	 *
	 * @return The ObjectId, or null if not available
	 */
	public Object getOid() {
		return this.oid;
	}

	/**
	 * Gets the full document after the change operation. Availability depends on
	 * the FullDocument mode:
	 * <ul>
	 * <li>DEFAULT: Available for INSERT only</li>
	 * <li>UPDATE_LOOKUP: Available for INSERT and UPDATE</li>
	 * <li>WHEN_AVAILABLE: Available when possible</li>
	 * </ul>
	 *
	 * @return The full document, or null if not available
	 */
	public Document getFullDocument() {
		return this.fullDocument;
	}

	/**
	 * Gets the update description for UPDATE operations. Contains information
	 * about which fields were updated or removed.
	 *
	 * @return The update description, or null if not an UPDATE operation
	 */
	public UpdateDescription getUpdateDescription() {
		return this.updateDescription;
	}

	/**
	 * Gets the timestamp when the change occurred.
	 *
	 * @return The timestamp from MongoDB cluster time
	 */
	public Instant getTimestamp() {
		return this.timestamp;
	}

	/**
	 * Gets the resume token for this change event. This token can be used to
	 * resume the change stream from this point if the stream is interrupted.
	 *
	 * @return The resume token
	 */
	public BsonDocument getResumeToken() {
		return this.resumeToken;
	}

	// Helper methods

	/**
	 * Checks if this event has a full document available.
	 *
	 * @return true if fullDocument is not null, false otherwise
	 */
	public boolean hasFullDocument() {
		return this.fullDocument != null;
	}

	/**
	 * Checks if this is an INSERT operation.
	 *
	 * @return true if operation type is INSERT
	 */
	public boolean isInsert() {
		return this.operationType == OperationType.INSERT;
	}

	/**
	 * Checks if this is an UPDATE operation.
	 *
	 * @return true if operation type is UPDATE
	 */
	public boolean isUpdate() {
		return this.operationType == OperationType.UPDATE;
	}

	/**
	 * Checks if this is a REPLACE operation.
	 *
	 * @return true if operation type is REPLACE
	 */
	public boolean isReplace() {
		return this.operationType == OperationType.REPLACE;
	}

	/**
	 * Checks if this is a DELETE operation.
	 *
	 * @return true if operation type is DELETE
	 */
	public boolean isDelete() {
		return this.operationType == OperationType.DELETE;
	}

	/**
	 * Get the ObjectId cast to a specific type
	 *
	 * @param <T> The type to cast to
	 * @param clazz The class to cast to
	 * @return The ObjectId cast to the specified type
	 */
	@SuppressWarnings("unchecked")
	public <T> T getOidAs(final Class<T> clazz) {
		if (this.oid == null) {
			return null;
		}
		return (T) this.oid;
	}

	/**
	 * Get the set of updated field names (for UPDATE operations)
	 *
	 * @return Set of field names that were updated, or empty set if not an update
	 */
	public Set<String> getUpdatedFields() {
		if (this.updateDescription == null || this.updateDescription.getUpdatedFields() == null) {
			return Collections.emptySet();
		}
		return this.updateDescription.getUpdatedFields().keySet();
	}

	/**
	 * Get the set of removed field names (for UPDATE operations)
	 *
	 * @return Set of field names that were removed, or empty set if not an update
	 */
	public Set<String> getRemovedFields() {
		if (this.updateDescription == null || this.updateDescription.getRemovedFields() == null) {
			return Collections.emptySet();
		}
		return Set.copyOf(this.updateDescription.getRemovedFields());
	}

	/**
	 * Converts the full document to a JSON string using standard JSON format.
	 *
	 * @return JSON representation of the document, or null if no full document is
	 *         available
	 */
	public String toJson() {
		if (this.fullDocument == null) {
			return null;
		}
		return this.fullDocument.toJson();
	}

	/**
	 * Converts the full document to a JSON string using MongoDB Extended JSON
	 * format. Extended JSON preserves type information for BSON-specific types
	 * like ObjectId, Date, etc.
	 *
	 * @return Extended JSON representation of the document, or null if no full
	 *         document is available
	 */
	public String toJsonExtended() {
		if (this.fullDocument == null) {
			return null;
		}
		final JsonWriterSettings settings = JsonWriterSettings.builder()//
				.outputMode(JsonMode.EXTENDED) //
				.build();
		return this.fullDocument.toJson(settings);
	}

	@Override
	public String toString() {
		return String.format("ChangeEvent{operation=%s, collection=%s, oid=%s, hasFullDoc=%s}", this.operationType,
				this.collectionName, this.oid, hasFullDocument());
	}
}
