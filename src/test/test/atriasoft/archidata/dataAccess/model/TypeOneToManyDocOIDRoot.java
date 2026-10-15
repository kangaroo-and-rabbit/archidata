package test.atriasoft.archidata.dataAccess.model;

import java.util.List;

import org.atriasoft.archidata.annotation.OneToManyDoc;
import org.atriasoft.archidata.model.OIDGenericData;
import org.bson.types.ObjectId;

public class TypeOneToManyDocOIDRoot extends OIDGenericData {

	public String otherData;

	@OneToManyDoc(targetEntity = TypeOneToManyDocOIDRemote.class, remoteField = "rootOid")
	public List<ObjectId> remoteIds;
}