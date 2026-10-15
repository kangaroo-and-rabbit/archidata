package test.atriasoft.archidata.dataAccess.model;

import org.atriasoft.archidata.annotation.Collection;
import org.atriasoft.archidata.annotation.ManyToOneDoc;
import org.atriasoft.archidata.model.GenericData;

@Collection("TypeManyToOneDocLongChild")
public class TypeManyToOneDocLongChildFFF extends GenericData {
	public String otherData;

	@ManyToOneDoc(targetEntity = TypeManyToOneDocLongParentIgnore.class, remoteField = "childIds", addLinkWhenCreate = false, removeLinkWhenDelete = false, updateLinkWhenUpdate = false)
	public Long parentId;

	public TypeManyToOneDocLongChildFFF() {}

	public TypeManyToOneDocLongChildFFF(String otherData, Long parentId) {
		this.otherData = otherData;
		this.parentId = parentId;
	}
}
