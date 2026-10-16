package test.atriasoft.archidata.journal.model;

import org.atriasoft.archidata.annotation.Field;
import org.atriasoft.archidata.annotation.Id;
import org.atriasoft.archidata.annotation.apiGenerator.ApiReadOnly;
import org.atriasoft.archidata.annotation.checker.GroupCreate;
import org.atriasoft.archidata.annotation.checker.GroupPersistant;
import org.atriasoft.archidata.annotation.checker.GroupRead;
import org.atriasoft.archidata.annotation.checker.GroupUpdate;
import org.bson.codecs.pojo.annotations.BsonId;
import org.bson.types.ObjectId;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Null;

/** Journalized model without any timestamp: only the initial full capture can see it. */
public class JournalDataWithoutUpdate {
	@Id
	// Read by the driver's PojoCodecProvider.automatic(true) (DbIoMongo): maps the property to _id.
	@BsonId
	@Field("_id")
	@ApiReadOnly
	@NotNull(groups = { GroupRead.class, GroupPersistant.class })
	@Null(groups = { GroupCreate.class, GroupUpdate.class })
	public ObjectId oid = null;

	public String dataString;
}
