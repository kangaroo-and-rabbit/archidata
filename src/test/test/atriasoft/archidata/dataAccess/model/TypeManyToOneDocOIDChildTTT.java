package test.atriasoft.archidata.dataAccess.model;

import org.atriasoft.archidata.annotation.Collection;
import org.atriasoft.archidata.annotation.ManyToOneDoc;
import org.atriasoft.archidata.model.OIDGenericData;
import org.bson.types.ObjectId;

@Collection("TypeManyToOneDocOIDChild")
public class TypeManyToOneDocOIDChildTTT extends OIDGenericData {

	public String otherData;

	@ManyToOneDoc(targetEntity = TypeManyToOneDocOIDParentIgnore.class, //
			remoteField = "childOids", //
			addLinkWhenCreate = true, //
			removeLinkWhenDelete = true, //
			updateLinkWhenUpdate = true)
	public ObjectId parentOid;

	public TypeManyToOneDocOIDChildTTT() {}

	public TypeManyToOneDocOIDChildTTT(String otherData, ObjectId remoteOid) {
		super();
		this.otherData = otherData;
		this.parentOid = remoteOid;
	}

}