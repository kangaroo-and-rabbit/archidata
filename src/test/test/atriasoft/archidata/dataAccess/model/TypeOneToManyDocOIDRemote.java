package test.atriasoft.archidata.dataAccess.model;

import org.atriasoft.archidata.annotation.ManyToOneDoc;
import org.atriasoft.archidata.model.OIDGenericData;
import org.bson.types.ObjectId;

public class TypeOneToManyDocOIDRemote extends OIDGenericData {

	@ManyToOneDoc(targetEntity = TypeOneToManyDocOIDRoot.class, remoteField = "remoteIds")
	public ObjectId rootOid;

	public String data;

}
