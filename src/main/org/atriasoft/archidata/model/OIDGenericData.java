package org.atriasoft.archidata.model;

import org.atriasoft.archidata.annotation.apiGenerator.ApiReadOnly;
import org.atriasoft.archidata.annotation.checker.GroupCreate;
import org.atriasoft.archidata.annotation.checker.GroupPersistant;
import org.atriasoft.archidata.annotation.checker.GroupRead;
import org.atriasoft.archidata.annotation.checker.GroupUpdate;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.types.ObjectId;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;

/**
 * Base data model with a MongoDB {@link ObjectId} primary key. Extends {@link GenericTiming} to include creation and update timestamps.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OIDGenericData extends GenericTiming {

	@Id
	@BsonId
	@Column(nullable = false, unique = true, name = "_id")
	@Schema(description = "Unique ObjectID of the object", example = "65161616841351")
	@ApiReadOnly
	@NotNull(groups = { GroupRead.class, GroupPersistant.class })
	@Null(groups = { GroupCreate.class, GroupUpdate.class })
	private ObjectId oid = null;

	/**
	 * Gets the MongoDB ObjectId of this object.
	 * @return the ObjectId
	 */
	public ObjectId getOid() {
		return this.oid;
	}

	/**
	 * Sets the MongoDB ObjectId of this object.
	 * @param oid the ObjectId to set
	 */
	public void setOid(final ObjectId oid) {
		this.oid = oid;
	}
}
