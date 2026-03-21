package org.atriasoft.archidata.model;

import java.util.Date;
import java.util.List;

import org.atriasoft.archidata.annotation.apiGenerator.ApiReadOnly;
import org.atriasoft.archidata.annotation.checker.GroupCreate;
import org.atriasoft.archidata.annotation.checker.GroupPersistant;
import org.atriasoft.archidata.annotation.checker.GroupRead;
import org.atriasoft.archidata.annotation.checker.GroupUpdate;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;
import jakarta.validation.constraints.Size;

/**
 * Represents a stored data/media file with its metadata including SHA-512 hash, MIME type, size, and analyzed media streams.
 */
@Table(name = "data")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Data extends OIDGenericDataSoftDelete {
	@Column(length = 128, nullable = false)
	@Schema(description = "Sha512 of the data")
	@Size(max = 512)
	@ApiReadOnly
	@NotNull(groups = { GroupRead.class, GroupPersistant.class })
	@Null(groups = { GroupCreate.class, GroupUpdate.class })
	private String sha512;
	@Column(length = 128, nullable = false)
	@Schema(description = "Mime -type of the media")
	@Size(max = 512)
	@ApiReadOnly
	@NotNull(groups = { GroupRead.class, GroupPersistant.class })
	@Null(groups = { GroupCreate.class, GroupUpdate.class })
	private String mimeType;
	@Column(nullable = false)
	@Schema(description = "Size in Byte of the data")
	@ApiReadOnly
	@NotNull(groups = { GroupRead.class, GroupPersistant.class })
	@Null(groups = { GroupCreate.class, GroupUpdate.class })
	private Long size;
	@Schema(description = "Analyzed media streams (video, audio, subtitle tracks)")
	@ApiReadOnly
	@Null(groups = { GroupCreate.class, GroupUpdate.class })
	private List<DataStream> streams;
	@Schema(description = "Timestamp when streams were last analyzed")
	@ApiReadOnly
	@Null(groups = { GroupCreate.class, GroupUpdate.class })
	private Date streamsAnalyzedAt;

	/**
	 * Gets the SHA-512 hash of the data.
	 * @return the SHA-512 hash string
	 */
	public String getSha512() {
		return this.sha512;
	}

	/**
	 * Sets the SHA-512 hash of the data.
	 * @param sha512 the SHA-512 hash string to set
	 */
	public void setSha512(final String sha512) {
		this.sha512 = sha512;
	}

	/**
	 * Gets the MIME type of the media.
	 * @return the MIME type string
	 */
	public String getMimeType() {
		return this.mimeType;
	}

	/**
	 * Sets the MIME type of the media.
	 * @param mimeType the MIME type string to set
	 */
	public void setMimeType(final String mimeType) {
		this.mimeType = mimeType;
	}

	/**
	 * Gets the size of the data in bytes.
	 * @return the size in bytes
	 */
	public Long getSize() {
		return this.size;
	}

	/**
	 * Sets the size of the data in bytes.
	 * @param size the size in bytes to set
	 */
	public void setSize(final Long size) {
		this.size = size;
	}

	/**
	 * Gets the analyzed media streams (video, audio, subtitle tracks).
	 * @return the list of data streams
	 */
	public List<DataStream> getStreams() {
		return this.streams;
	}

	/**
	 * Sets the analyzed media streams.
	 * @param streams the list of data streams to set
	 */
	public void setStreams(final List<DataStream> streams) {
		this.streams = streams;
	}

	/**
	 * Gets the timestamp when streams were last analyzed.
	 * @return the analysis timestamp
	 */
	public Date getStreamsAnalyzedAt() {
		return this.streamsAnalyzedAt;
	}

	/**
	 * Sets the timestamp when streams were last analyzed.
	 * @param streamsAnalyzedAt the analysis timestamp to set
	 */
	public void setStreamsAnalyzedAt(final Date streamsAnalyzedAt) {
		this.streamsAnalyzedAt = streamsAnalyzedAt;
	}
}
