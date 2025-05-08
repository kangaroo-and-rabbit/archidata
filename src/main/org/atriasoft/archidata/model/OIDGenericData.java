package org.atriasoft.archidata.model;

import org.atriasoft.archidata.annotation.checker.GroupCreate;
import org.atriasoft.archidata.annotation.checker.GroupRead;
import org.atriasoft.archidata.annotation.checker.GroupUpdate;
import org.bson.types.ObjectId;

import dev.morphia.annotations.Id;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;

public class OIDGenericData extends GenericTiming {
	@Id
	@jakarta.persistence.Id
	@Column(nullable = false, unique = true, name = "_id")
	@Schema(description = "Unique ObjectID of the object", example = "65161616841351")
	@NotNull(groups = { GroupRead.class })
	@Null(groups = { GroupCreate.class, GroupUpdate.class })
	public ObjectId oid = null;
}
