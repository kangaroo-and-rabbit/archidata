package org.atriasoft.archidata.model;

import java.util.Date;

import org.atriasoft.archidata.annotation.CreationTimestamp;
import org.atriasoft.archidata.annotation.DataNotRead;
import org.atriasoft.archidata.annotation.UpdateTimestamp;
import org.atriasoft.archidata.annotation.apiGenerator.ApiReadOnly;
import org.atriasoft.archidata.annotation.checker.GroupCreate;
import org.atriasoft.archidata.annotation.checker.GroupPersistant;
import org.atriasoft.archidata.annotation.checker.GroupUpdate;
import org.atriasoft.archidata.tools.DateTools;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;

/**
 * Base model providing automatic creation and update timestamps. All data models that need time tracking should extend this class.
 *
 * <p>
 * {@code createdAt} is filled on insert ({@link CreationTimestamp}) and {@code updatedAt} on every
 * update ({@link UpdateTimestamp}) by the data access layer. Both are read-only for the API clients
 * ({@link ApiReadOnly}), must be absent from the create and update payloads and are mandatory once
 * persisted (validation groups {@link GroupCreate}, {@link GroupUpdate} and {@link GroupPersistant}).
 * They are serialized as ISO-8601 strings. This class has no primary key; the ready-to-use bases
 * that add one are {@link OIDGenericData} (ObjectId), {@link UUIDGenericData} (UUID) and
 * {@link GenericData} (Long), with {@code ...SoftDelete} variants.
 * </p>
 */
//@ApiGenerationMode(create = true, update = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GenericTiming {

	/** Creates an empty {@link GenericTiming} instance. */
	public GenericTiming() {}

	@DataNotRead
	@CreationTimestamp
	@Schema(description = "Create time of the object", example = "2000-01-23T01:23:45.678+01:00")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
	@ApiReadOnly
	@NotNull(groups = { GroupPersistant.class })
	@Null(groups = { GroupCreate.class, GroupUpdate.class })
	private Date createdAt = null;
	@DataNotRead
	@UpdateTimestamp
	@Schema(description = "When update the object", example = "2000-01-23T00:23:45.678Z")
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
	@ApiReadOnly
	@NotNull(groups = { GroupPersistant.class })
	@Null(groups = { GroupCreate.class, GroupUpdate.class })
	private Date updatedAt = null;

	/**
	 * Gets the creation timestamp of this object.
	 * @return the creation date
	 */
	public Date getCreatedAt() {
		return DateTools.copyOf(this.createdAt);
	}

	/**
	 * Sets the creation timestamp of this object.
	 * @param createdAt the creation date to set
	 */
	public void setCreatedAt(final Date createdAt) {
		this.createdAt = DateTools.copyOf(createdAt);
	}

	/**
	 * Gets the last update timestamp of this object.
	 * @return the last update date
	 */
	public Date getUpdatedAt() {
		return DateTools.copyOf(this.updatedAt);
	}

	/**
	 * Sets the last update timestamp of this object.
	 * @param updatedAt the last update date to set
	 */
	public void setUpdatedAt(final Date updatedAt) {
		this.updatedAt = DateTools.copyOf(updatedAt);
	}
}
