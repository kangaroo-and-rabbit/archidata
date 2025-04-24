package test.atriasoft.archidata.dataAccess.model;

import org.atriasoft.archidata.annotation.ManyToOneNoSQL;
import org.atriasoft.archidata.model.OIDGenericData;
import org.bson.types.ObjectId;

import dev.morphia.annotations.Entity;

@Entity
public class TypeOneToManyNoSqlOIDRemote extends OIDGenericData {

	@ManyToOneNoSQL(targetEntity = TypeOneToManyNoSqlOIDRoot.class, remoteField = "remoteIds")
	public ObjectId rootOid;

	public String data;

}
