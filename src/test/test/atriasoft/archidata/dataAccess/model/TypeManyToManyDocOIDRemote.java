package test.atriasoft.archidata.dataAccess.model;

import java.util.List;

import org.atriasoft.archidata.annotation.ManyToManyDoc;
import org.atriasoft.archidata.model.OIDGenericData;
import org.bson.types.ObjectId;

public class TypeManyToManyDocOIDRemote extends OIDGenericData {
	@ManyToManyDoc(targetEntity = TypeManyToManyDocOIDRoot.class, remoteField = "remote")
	public List<ObjectId> remoteToParent;
	public String data;

}