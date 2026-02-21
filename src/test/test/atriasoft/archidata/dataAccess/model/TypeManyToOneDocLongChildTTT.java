package test.atriasoft.archidata.dataAccess.model;

import org.atriasoft.archidata.annotation.ManyToOneDoc;
import org.atriasoft.archidata.model.GenericData;

import jakarta.persistence.Column;
import jakarta.persistence.Table;

@Table(name = "TypeManyToOneDocLongChild")
public class TypeManyToOneDocLongChildTTT extends GenericData {
	public String otherData;

	@ManyToOneDoc(targetEntity = TypeManyToOneDocLongParentIgnore.class,
			remoteField = "childIds",
			addLinkWhenCreate = true,
			removeLinkWhenDelete = true,
			updateLinkWhenUpdate = true)
	@Column(nullable = false)
	public Long parentId;

	public TypeManyToOneDocLongChildTTT() {}

	public TypeManyToOneDocLongChildTTT(String otherData, Long parentId) {
		this.otherData = otherData;
		this.parentId = parentId;
	}
}
