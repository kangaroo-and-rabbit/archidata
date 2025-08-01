package test.atriasoft.archidata.backup.model;

import java.util.Date;

import org.atriasoft.archidata.annotation.apiGenerator.ApiReadOnly;
import org.atriasoft.archidata.annotation.checker.GroupCreate;
import org.atriasoft.archidata.annotation.checker.GroupPersistant;
import org.atriasoft.archidata.annotation.checker.GroupRead;
import org.atriasoft.archidata.annotation.checker.GroupUpdate;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.types.ObjectId;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;

public class DataStoreWithoutUpdate {
	@Id
	@BsonId
	@Column(nullable = false, unique = true, name = "_id")
	@Schema(description = "Unique ObjectID of the object", example = "65161616841351")
	@ApiReadOnly
	@NotNull(groups = { GroupRead.class, GroupPersistant.class })
	@Null(groups = { GroupCreate.class, GroupUpdate.class })
	public ObjectId oid = null;

	public String dataString;

	public Date dataTime;
}
