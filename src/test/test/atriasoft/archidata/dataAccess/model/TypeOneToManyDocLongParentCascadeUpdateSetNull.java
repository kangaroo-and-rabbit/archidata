package test.atriasoft.archidata.dataAccess.model;

import java.util.List;

import org.atriasoft.archidata.annotation.OneToManyDoc;
import org.atriasoft.archidata.annotation.OneToManyDoc.CascadeMode;
import org.atriasoft.archidata.model.GenericData;

public class TypeOneToManyDocLongParentCascadeUpdateSetNull extends GenericData {
	public String data;

	@OneToManyDoc(targetEntity = TypeOneToManyDocLongRemote.class,
			remoteField = "parentId",
			addLinkWhenCreate = true,
			cascadeUpdate = CascadeMode.SET_NULL,
			cascadeDelete = CascadeMode.IGNORE)
	public List<Long> remoteIds;

	public TypeOneToManyDocLongParentCascadeUpdateSetNull() {}

	public TypeOneToManyDocLongParentCascadeUpdateSetNull(String data, List<Long> remoteIds) {
		this.data = data;
		this.remoteIds = remoteIds;
	}
}
