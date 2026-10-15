package test.atriasoft.archidata.dataAccess.model;

import org.atriasoft.archidata.annotation.Collection;
import org.atriasoft.archidata.annotation.ManyToOneDoc;
import org.atriasoft.archidata.model.GenericData;

@Collection("TypeManyToOneDocLongChild")
public class TypeManyToOneDocLongChildTTT extends GenericData {
	public String otherData;

	@ManyToOneDoc(targetEntity = TypeManyToOneDocLongParentIgnore.class, remoteField = "childIds", addLinkWhenCreate = true, removeLinkWhenDelete = true, updateLinkWhenUpdate = true)
	public Long parentId;

	public TypeManyToOneDocLongChildTTT() {}

	public TypeManyToOneDocLongChildTTT(String otherData, Long parentId) {
		this.otherData = otherData;
		this.parentId = parentId;
	}
}
