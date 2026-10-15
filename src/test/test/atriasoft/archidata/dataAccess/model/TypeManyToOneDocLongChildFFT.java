package test.atriasoft.archidata.dataAccess.model;

import org.atriasoft.archidata.annotation.Collection;
import org.atriasoft.archidata.annotation.ManyToOneDoc;
import org.atriasoft.archidata.model.GenericData;

@Collection("TypeManyToOneDocLongChild")
public class TypeManyToOneDocLongChildFFT extends GenericData {
	public String otherData;

	@ManyToOneDoc(targetEntity = TypeManyToOneDocLongParentIgnore.class, remoteField = "childIds", addLinkWhenCreate = false, removeLinkWhenDelete = false, updateLinkWhenUpdate = true)
	public Long parentId;

	public TypeManyToOneDocLongChildFFT() {}

	public TypeManyToOneDocLongChildFFT(String otherData, Long parentId) {
		this.otherData = otherData;
		this.parentId = parentId;
	}
}
