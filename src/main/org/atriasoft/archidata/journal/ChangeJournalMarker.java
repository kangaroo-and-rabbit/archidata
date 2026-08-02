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
 * Progress marker of the change journal for one source collection: the upper bound of the last
 * successful capture. The next run of {@link ChangeJournalEngine} only selects the documents
 * modified strictly after {@link #getLastSavedAt()}.
 * <p>
 * One marker exists per journalized collection ({@code collectionName} is unique). A collection
 * whose capture failed keeps its previous marker, so the missed documents are picked up by the
 * next run.
 */
@Table(name = ChangeJournalMarker.COLLECTION_NAME)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChangeJournalMarker {

	/** Default MongoDB collection storing the per-collection journal markers. */
	public static final String COLLECTION_NAME = "changeJournalMarker";

	/** Creates an empty {@link ChangeJournalMarker} instance. */
	public ChangeJournalMarker() {}

	@Id
	@BsonId
	@Column(nullable = false, unique = true, name = "_id")
	@Schema(description = "Unique ObjectID of the marker")
	@ApiReadOnly
	private ObjectId oid = null;

	@Column(nullable = false, unique = true)
	@Schema(description = "Name of the journalized source collection (table)", example = "user")
	private String collectionName = null;

	@Column(nullable = false)
	@Schema(description = "Upper bound of the last successful capture: only documents modified after this date are captured next time", example = "2000-01-23T00:23:45.678Z")
	private Date lastSavedAt = null;

	@Schema(description = "Date of the last successful capture run for this collection", example = "2000-01-23T00:23:45.678Z")
	private Date lastRunAt = null;

	@Schema(description = "Number of entries written by the last capture run", example = "42")
	private long lastCount = 0L;

	@Schema(description = "Total number of entries written for this collection since the journal exists", example = "4242")
	private long totalCount = 0L;

	/**
	 * Gets the MongoDB ObjectId of this marker.
	 * @return the ObjectId
	 */
	public ObjectId getOid() {
		return this.oid;
	}

	/**
	 * Sets the MongoDB ObjectId of this marker.
	 * @param oid the ObjectId to set
	 */
	public void setOid(final ObjectId oid) {
		this.oid = oid;
	}

	/**
	 * Gets the name of the journalized source collection.
	 * @return the source collection name
	 */
	public String getCollectionName() {
		return this.collectionName;
	}

	/**
	 * Sets the name of the journalized source collection.
	 * @param collectionName the source collection name
	 */
	public void setCollectionName(final String collectionName) {
		this.collectionName = collectionName;
	}

	/**
	 * Gets the upper bound of the last successful capture.
	 * @return the last saved date
	 */
	public Date getLastSavedAt() {
		return this.lastSavedAt;
	}

	/**
	 * Sets the upper bound of the last successful capture.
	 * @param lastSavedAt the last saved date
	 */
	public void setLastSavedAt(final Date lastSavedAt) {
		this.lastSavedAt = lastSavedAt;
	}

	/**
	 * Gets the date of the last successful capture run for this collection.
	 * @return the last run date
	 */
	public Date getLastRunAt() {
		return this.lastRunAt;
	}

	/**
	 * Sets the date of the last successful capture run for this collection.
	 * @param lastRunAt the last run date
	 */
	public void setLastRunAt(final Date lastRunAt) {
		this.lastRunAt = lastRunAt;
	}

	/**
	 * Gets the number of entries written by the last capture run.
	 * @return the last run entry count
	 */
	public long getLastCount() {
		return this.lastCount;
	}

	/**
	 * Sets the number of entries written by the last capture run.
	 * @param lastCount the last run entry count
	 */
	public void setLastCount(final long lastCount) {
		this.lastCount = lastCount;
	}

	/**
	 * Gets the total number of entries written for this collection.
	 * @return the cumulated entry count
	 */
	public long getTotalCount() {
		return this.totalCount;
	}

	/**
	 * Sets the total number of entries written for this collection.
	 * @param totalCount the cumulated entry count
	 */
	public void setTotalCount(final long totalCount) {
		this.totalCount = totalCount;
	}
}
