package test.atriasoft.archidata.dataAccess.model;

import java.util.List;

import org.atriasoft.archidata.annotation.ManyToManyDoc;
import org.atriasoft.archidata.model.OIDGenericData;

import jakarta.persistence.Table;

@Table(name = "TypeManyToManyDocOIDRoot")
public class TypeManyToManyDocOIDRootExpand extends OIDGenericData {

	public String otherData;

	@ManyToManyDoc(targetEntity = TypeManyToManyDocOIDRemote.class, remoteField = "remoteToParent")
	public List<TypeManyToManyDocOIDRemote> remote;
}
