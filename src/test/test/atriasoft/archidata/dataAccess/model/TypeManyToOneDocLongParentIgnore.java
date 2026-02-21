package test.atriasoft.archidata.dataAccess.model;

import java.util.ArrayList;
import java.util.List;

import org.atriasoft.archidata.annotation.OneToManyDoc;
import org.atriasoft.archidata.annotation.OneToManyDoc.CascadeMode;
import org.atriasoft.archidata.model.GenericData;

public class TypeManyToOneDocLongParentIgnore extends GenericData {
	public String data;

	@OneToManyDoc(targetEntity = TypeManyToOneDocLongChildTTT.class,
			remoteField = "parentId",
			addLinkWhenCreate = false,
			cascadeUpdate = CascadeMode.IGNORE,
			cascadeDelete = CascadeMode.IGNORE)
	public List<Long> childIds;

	public TypeManyToOneDocLongParentIgnore() {}

	public TypeManyToOneDocLongParentIgnore(String data, List<Long> childIds) {
		this.data = data;
		this.childIds = childIds != null ? new ArrayList<>(childIds) : null;
	}
}
