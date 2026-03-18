package test.atriasoft.archidata.dataAccess.model;

import java.util.List;

import org.atriasoft.archidata.annotation.OneToManyDoc;
import org.atriasoft.archidata.annotation.OneToManyDoc.CascadeMode;
import org.atriasoft.archidata.model.GenericData;

import jakarta.persistence.Column;
import jakarta.persistence.Table;

@Table(name = "TypeOneToManyDocLongParentIgnore")
public class TypeOneToManyDocLongParentExpandIgnore extends GenericData {
	public String data;

	@OneToManyDoc(targetEntity = TypeOneToManyDocLongRemote.class, remoteField = "parentId", addLinkWhenCreate = true, cascadeUpdate = CascadeMode.IGNORE, cascadeDelete = CascadeMode.IGNORE)
	@Column(nullable = false, name = "remoteIds")
	public List<TypeOneToManyDocLongRemote> remoteEntities;
}
