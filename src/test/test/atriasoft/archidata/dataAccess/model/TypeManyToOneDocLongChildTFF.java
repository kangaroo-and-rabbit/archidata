package test.atriasoft.archidata.dataAccess.model;

import org.atriasoft.archidata.annotation.Collection;
import org.atriasoft.archidata.annotation.ManyToOneDoc;
import org.atriasoft.archidata.model.GenericData;

@Collection("TypeManyToOneDocLongChild")
public class TypeManyToOneDocLongChildTFF extends GenericData {
	public String otherData;

	@ManyToOneDoc(targetEntity = TypeManyToOneDocLongParentIgnore.class, remoteField = "childIds", addLinkWhenCreate = true, removeLinkWhenDelete = false, updateLinkWhenUpdate = false)
	public Long parentId;

	public TypeManyToOneDocLongChildTFF() {}

	public TypeManyToOneDocLongChildTFF(String otherData, Long parentId) {
		this.otherData = otherData;
		this.parentId = parentId;
	}
}
