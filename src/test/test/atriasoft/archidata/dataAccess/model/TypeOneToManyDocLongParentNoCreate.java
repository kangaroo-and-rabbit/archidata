package test.atriasoft.archidata.dataAccess.model;

import java.util.List;

import org.atriasoft.archidata.annotation.OneToManyDoc;
import org.atriasoft.archidata.annotation.OneToManyDoc.CascadeMode;
import org.atriasoft.archidata.model.GenericData;

public class TypeOneToManyDocLongParentNoCreate extends GenericData {
	public String data;

	@OneToManyDoc(targetEntity = TypeOneToManyDocLongRemote.class,
			remoteField = "parentId",
			addLinkWhenCreate = false,
			cascadeUpdate = CascadeMode.IGNORE,
			cascadeDelete = CascadeMode.IGNORE)
	public List<Long> remoteIds;

	public TypeOneToManyDocLongParentNoCreate() {}

	public TypeOneToManyDocLongParentNoCreate(String data, List<Long> remoteIds) {
		this.data = data;
		this.remoteIds = remoteIds;
	}
}
