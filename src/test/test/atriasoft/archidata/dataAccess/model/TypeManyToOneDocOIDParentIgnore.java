package test.atriasoft.archidata.dataAccess.model;

import java.util.ArrayList;
import java.util.List;

import org.atriasoft.archidata.annotation.OneToManyDoc;
import org.atriasoft.archidata.annotation.OneToManyDoc.CascadeMode;
import org.atriasoft.archidata.model.OIDGenericData;
import org.bson.types.ObjectId;

import jakarta.persistence.Table;

@Table(name = "TypeManyToOneDocOIDParent")
public class TypeManyToOneDocOIDParentIgnore extends OIDGenericData {

	public String data;

	@OneToManyDoc(targetEntity = TypeManyToOneDocOIDChildTTT.class, //
			remoteField = "parentOid", //
			addLinkWhenCreate = false, //
			cascadeUpdate = CascadeMode.IGNORE, //
			cascadeDelete = CascadeMode.IGNORE)
	public List<ObjectId> childOids;

	public TypeManyToOneDocOIDParentIgnore(String data, List<ObjectId> childOids) {
		this.data = data;
		this.childOids = new ArrayList<ObjectId>();
		this.childOids.addAll(childOids);
	}

	public TypeManyToOneDocOIDParentIgnore() {}

}