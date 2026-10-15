package test.atriasoft.archidata.dataAccess.model;

import java.util.List;

import org.atriasoft.archidata.annotation.Collection;
import org.atriasoft.archidata.annotation.Field;
import org.atriasoft.archidata.annotation.OneToManyDoc;
import org.atriasoft.archidata.model.OIDGenericData;

@Collection("TypeOneToManyDocOIDRoot")
public class TypeOneToManyDocOIDRootExpand extends OIDGenericData {

	public String otherData;

	@OneToManyDoc(targetEntity = TypeOneToManyDocOIDRemote.class, remoteField = "rootOid")
	@Field("remoteIds")
	public List<TypeOneToManyDocOIDRemote> remotes;
}
