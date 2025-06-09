package test.atriasoft.archidata.dataAccess.model;

import java.util.List;

import org.atriasoft.archidata.annotation.OneToManyDoc;
import org.atriasoft.archidata.model.OIDGenericData;
import org.bson.types.ObjectId;

public class TypeManyToOneDocOIDRemote extends OIDGenericData {

	public String data;

	@OneToManyDoc(targetEntity = TypeManyToOneDocOIDRoot.class, remoteField = "remoteOid")
	public List<ObjectId> remoteOids;

}