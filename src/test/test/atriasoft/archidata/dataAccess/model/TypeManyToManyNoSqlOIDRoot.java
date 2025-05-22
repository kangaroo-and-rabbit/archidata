package test.atriasoft.archidata.dataAccess.model;

import java.util.List;

import org.atriasoft.archidata.annotation.ManyToManyNoSQL;
import org.atriasoft.archidata.model.OIDGenericData;
import org.bson.types.ObjectId;

public class TypeManyToManyNoSqlOIDRoot extends OIDGenericData {

	public String otherData;

	@ManyToManyNoSQL(targetEntity = TypeManyToManyNoSqlOIDRemote.class, remoteField = "remoteToParent")
	public List<ObjectId> remote;
}
