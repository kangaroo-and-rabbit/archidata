package test.atriasoft.archidata.dataAccess.model;

import java.util.List;

import org.atriasoft.archidata.annotation.OneToManyNoSQL;
import org.atriasoft.archidata.model.OIDGenericData;
import org.bson.types.ObjectId;

public class TypeManyToOneNoSqlOIDRemote extends OIDGenericData {

	public String data;

	@OneToManyNoSQL(targetEntity = TypeManyToOneNoSqlOIDRoot.class, remoteField = "remoteOid")
	public List<ObjectId> remoteOids;

}