package test.atriasoft.archidata.dataAccess.model;

import java.util.List;

import org.atriasoft.archidata.annotation.ManyToManyNoSQL;
import org.atriasoft.archidata.model.OIDGenericData;
import org.bson.types.ObjectId;

public class TypeManyToManyNoSqlOIDRemote extends OIDGenericData {
	@ManyToManyNoSQL(targetEntity = TypeManyToManyNoSqlOIDRoot.class, remoteField = "remote")
	public List<ObjectId> remoteToParent;
	public String data;

}