package test.atriasoft.archidata.dataAccess.model;

import java.util.List;

import org.atriasoft.archidata.annotation.Collection;
import org.atriasoft.archidata.annotation.ManyToManyDoc;
import org.atriasoft.archidata.model.GenericData;

@Collection("TypeManyToManyDocLongRoot")
public class TypeManyToManyDocLongRootExpand extends GenericData {

	public String otherData;

	@ManyToManyDoc(targetEntity = TypeManyToManyDocLongRemote.class, remoteField = "remoteToParent")
	public List<TypeManyToManyDocLongRemote> remote;
}
