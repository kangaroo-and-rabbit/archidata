package test.atriasoft.archidata.dataAccess.model;

import org.atriasoft.archidata.annotation.ManyToOneDoc;
import org.atriasoft.archidata.model.GenericData;

import jakarta.persistence.Column;
import jakarta.persistence.Table;

@Table(name = "TypeManyToOneDocLongChild")
public class TypeManyToOneDocLongChildFTF extends GenericData {
	public String otherData;

	@ManyToOneDoc(targetEntity = TypeManyToOneDocLongParentIgnore.class,
			remoteField = "childIds",
			addLinkWhenCreate = false,
			removeLinkWhenDelete = true,
			updateLinkWhenUpdate = false)
	@Column(nullable = false)
	public Long parentId;

	public TypeManyToOneDocLongChildFTF() {}

	public TypeManyToOneDocLongChildFTF(String otherData, Long parentId) {
		this.otherData = otherData;
		this.parentId = parentId;
	}
}
