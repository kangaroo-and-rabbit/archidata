package org.atriasoft.archidata.journal;

import java.util.Date;

import org.atriasoft.archidata.annotation.apiGenerator.ApiReadOnly;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.types.ObjectId;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * A single entry of the change journal: one version of one source document, captured by
 * {@link ChangeJournalEngine} at a given point in time.
 * <p>
 * All the journalized collections share this single collection: the source collection is
 * identified by {@link #getCollectionName()} and the source document content is stored
 * serialized in {@link #getData()} (BSON extended JSON, lossless round-trip).
 * <p>
 * The journal is append-only: every capture of a modified document adds a new entry, so the
 * successive versions of a document are the entries sharing the same
 * ({@code collectionName}, {@code sourceId}) ordered by {@code recordedAt}.
 */
@Table(name = ChangeJournalEntry.COLLECTION_NAME)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChangeJournalEntry {

	/** Default MongoDB collection storing every journal entry. */
	public static final String COLLECTION_NAME = "changeJournal";

	/** Creates an empty {@link ChangeJournalEntry} instance. */
	public ChangeJournalEntry() {}

	@Id
	@BsonId
	@Column(nullable = false, unique = true, name = "_id")
	@Schema(description = "Unique ObjectID of the journal entry")
	@ApiReadOnly
	private ObjectId oid = null;

	@Column(nullable = false)
	@Schema(description = "Name of the source collection (table) the document comes from", example = "user")
	private String collectionName = null;

	@Column(nullable = false)
	@Schema(description = "String representation of the source document primary key", example = "65161616841351")
	private String sourceId = null;

	@Column(nullable = false)
	@Schema(description = "Source document serialized in BSON extended JSON")
	private String data = null;

	@Schema(description = "Update (or creation) date of the source document when it was captured", example = "2000-01-23T00:23:45.678Z")
	private Date sourceUpdatedAt = null;

	@Column(nullable = false)
	@Schema(description = "Date at which this entry was written in the journal", example = "2000-01-23T00:23:45.678Z")
	private Date recordedAt = null;

	/**
	 * Gets the MongoDB ObjectId of this journal entry.
	 * @return the ObjectId
	 */
	public ObjectId getOid() {
		return this.oid;
	}

	/**
	 * Sets the MongoDB ObjectId of this journal entry.
	 * @param oid the ObjectId to set
	 */
	public void setOid(final ObjectId oid) {
		this.oid = oid;
	}

	/**
	 * Gets the name of the source collection the captured document comes from.
	 * @return the source collection name
	 */
	public String getCollectionName() {
		return this.collectionName;
	}

	/**
	 * Sets the name of the source collection the captured document comes from.
	 * @param collectionName the source collection name
	 */
	public void setCollectionName(final String collectionName) {
		this.collectionName = collectionName;
	}

	/**
	 * Gets the string representation of the source document primary key.
	 * @return the source document identifier
	 */
	public String getSourceId() {
		return this.sourceId;
	}

	/**
	 * Sets the string representation of the source document primary key.
	 * @param sourceId the source document identifier
	 */
	public void setSourceId(final String sourceId) {
		this.sourceId = sourceId;
	}

	/**
	 * Gets the serialized source document (BSON extended JSON).
	 * @return the serialized document
	 */
	public String getData() {
		return this.data;
	}

	/**
	 * Sets the serialized source document (BSON extended JSON).
	 * @param data the serialized document
	 */
	public void setData(final String data) {
		this.data = data;
	}

	/**
	 * Gets the update (or creation) date the source document carried when it was captured.
	 * @return the source document date, or {@code null} if the document carries none
	 */
	public Date getSourceUpdatedAt() {
		return this.sourceUpdatedAt;
	}

	/**
	 * Sets the update (or creation) date the source document carried when it was captured.
	 * @param sourceUpdatedAt the source document date
	 */
	public void setSourceUpdatedAt(final Date sourceUpdatedAt) {
		this.sourceUpdatedAt = sourceUpdatedAt;
	}

	/**
	 * Gets the date at which this entry was written in the journal.
	 * @return the record date
	 */
	public Date getRecordedAt() {
		return this.recordedAt;
	}

	/**
	 * Sets the date at which this entry was written in the journal.
	 * @param recordedAt the record date
	 */
	public void setRecordedAt(final Date recordedAt) {
		this.recordedAt = recordedAt;
	}
}
