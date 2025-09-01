package test.atriasoft.archidata.dataAccess.model;

import java.util.List;

import org.atriasoft.archidata.annotation.OneToManyDoc;
import org.atriasoft.archidata.annotation.OneToManyDoc.CascadeMode;
import org.atriasoft.archidata.model.OIDGenericData;
import org.bson.types.ObjectId;

import jakarta.persistence.Table;

@Table(name = "TypeManyToOneDocOIDParent")
public class TypeManyToOneDocOIDParentDelete extends OIDGenericData {

	public String data;

	@OneToManyDoc(targetEntity = TypeManyToOneDocOIDChildTTT.class, //
			remoteField = "parentOid", //
			addLinkWhenCreate = true, //
			cascadeUpdate = CascadeMode.DELETE, //
			cascadeDelete = CascadeMode.DELETE)
	public List<ObjectId> childOids;

	public TypeManyToOneDocOIDParentDelete(String data, List<ObjectId> childOids) {
		this.data = data;
		this.childOids = childOids;
	}

	public TypeManyToOneDocOIDParentDelete() {}
}