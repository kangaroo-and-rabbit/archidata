package test.atriasoft.archidata.dataAccess.model;

import java.util.List;

import org.atriasoft.archidata.annotation.Collection;
import org.atriasoft.archidata.annotation.Field;
import org.atriasoft.archidata.annotation.OneToManyDoc;
import org.atriasoft.archidata.annotation.OneToManyDoc.CascadeMode;
import org.atriasoft.archidata.model.GenericData;

@Collection("TypeOneToManyDocLongParentIgnore")
public class TypeOneToManyDocLongParentExpandIgnore extends GenericData {
	public String data;

	@OneToManyDoc(targetEntity = TypeOneToManyDocLongRemote.class, remoteField = "parentId", addLinkWhenCreate = true, cascadeUpdate = CascadeMode.IGNORE, cascadeDelete = CascadeMode.IGNORE)
	@Field("remoteIds")
	public List<TypeOneToManyDocLongRemote> remoteEntities;
}
