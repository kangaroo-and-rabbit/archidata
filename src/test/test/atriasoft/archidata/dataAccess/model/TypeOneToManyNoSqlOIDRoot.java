package test.atriasoft.archidata.dataAccess.model;

import java.util.List;

import org.atriasoft.archidata.annotation.OneToManyNoSQL;
import org.atriasoft.archidata.model.OIDGenericData;
import org.bson.types.ObjectId;

import dev.morphia.annotations.Entity;
import jakarta.persistence.Column;

@Entity
public class TypeOneToManyNoSqlOIDRoot extends OIDGenericData {

	public String otherData;

	@OneToManyNoSQL(targetEntity = TypeOneToManyNoSqlOIDRemote.class, remoteField = "rootOid")
	@Column(nullable = false)
	public List<ObjectId> remoteIds;
}