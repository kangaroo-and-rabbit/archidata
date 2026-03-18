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

	public String getSha512() {
		return this.sha512;
	}

	public void setSha512(final String sha512) {
		this.sha512 = sha512;
	}

	public String getMimeType() {
		return this.mimeType;
	}

	public void setMimeType(final String mimeType) {
		this.mimeType = mimeType;
	}

	public Long getSize() {
		return this.size;
	}

	public void setSize(final Long size) {
		this.size = size;
	}

	public List<DataStream> getStreams() {
		return this.streams;
	}

	public void setStreams(final List<DataStream> streams) {
		this.streams = streams;
	}

	public Date getStreamsAnalyzedAt() {
		return this.streamsAnalyzedAt;
	}

	public void setStreamsAnalyzedAt(final Date streamsAnalyzedAt) {
		this.streamsAnalyzedAt = streamsAnalyzedAt;
	}
}
