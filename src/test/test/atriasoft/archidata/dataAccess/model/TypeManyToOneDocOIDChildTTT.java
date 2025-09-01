package test.atriasoft.archidata.dataAccess.model;

import org.atriasoft.archidata.annotation.ManyToOneDoc;
import org.atriasoft.archidata.model.OIDGenericData;
import org.bson.types.ObjectId;

import jakarta.persistence.Column;
import jakarta.persistence.Table;

@Table(name = "TypeManyToOneDocOIDChild")
public class TypeManyToOneDocOIDChildTTT extends OIDGenericData {

	public String otherData;

	@ManyToOneDoc(targetEntity = TypeManyToOneDocOIDParentIgnore.class, //
			remoteField = "childOids", //
			addLinkWhenCreate = true, //
			removeLinkWhenDelete = true, //
			updateLinkWhenUpdate = true)
	@Column(nullable = false)
	public ObjectId parentOid;

	public TypeManyToOneDocOIDChildTTT() {}

	public TypeManyToOneDocOIDChildTTT(String otherData, ObjectId remoteOid) {
		super();
		this.otherData = otherData;
		this.parentOid = remoteOid;
	}

}