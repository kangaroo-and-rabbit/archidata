package test.atriasoft.archidata.dataAccess.model;

import org.atriasoft.archidata.annotation.ManyToOneDoc;
import org.atriasoft.archidata.model.OIDGenericData;
import org.bson.types.ObjectId;

import jakarta.persistence.Column;
import jakarta.persistence.Table;

@Table(name = "TypeManyToOneDocOIDChild")
public class TypeManyToOneDocOIDChildFFF extends OIDGenericData {

	public String otherData;

	@ManyToOneDoc(targetEntity = TypeManyToOneDocOIDParentIgnore.class, //
			remoteField = "childOids", //
			addLinkWhenCreate = false, //
			removeLinkWhenDelete = false, //
			updateLinkWhenUpdate = false)
	@Column(nullable = false)
	public ObjectId parentOid;

	public TypeManyToOneDocOIDChildFFF() {}

	public TypeManyToOneDocOIDChildFFF(String otherData, ObjectId remoteOid) {
		this.otherData = otherData;
		this.parentOid = remoteOid;
	}

}