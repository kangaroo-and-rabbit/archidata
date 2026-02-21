package test.atriasoft.archidata.dataAccess.model;

import java.util.List;

import org.atriasoft.archidata.annotation.ManyToManyDoc;
import org.atriasoft.archidata.model.GenericData;

public class TypeManyToManyDocLongRemote extends GenericData {
	@ManyToManyDoc(targetEntity = TypeManyToManyDocLongRoot.class, remoteField = "remote")
	public List<Long> remoteToParent;
	public String data;

}
