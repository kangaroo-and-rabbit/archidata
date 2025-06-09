package test.atriasoft.archidata.dataAccess.model;

import java.util.List;

import org.atriasoft.archidata.annotation.ManyToManyDoc;
import org.atriasoft.archidata.model.OIDGenericData;
import org.bson.types.ObjectId;

public class TypeManyToManyDocOIDRoot extends OIDGenericData {

	public String otherData;

	@ManyToManyDoc(targetEntity = TypeManyToManyDocOIDRemote.class, remoteField = "remoteToParent")
	public List<ObjectId> remote;
}
