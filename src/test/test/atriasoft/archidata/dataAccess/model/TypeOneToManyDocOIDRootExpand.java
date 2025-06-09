package test.atriasoft.archidata.dataAccess.model;

import java.util.List;

import org.atriasoft.archidata.annotation.OneToManyDoc;
import org.atriasoft.archidata.model.OIDGenericData;

import jakarta.persistence.Column;
import jakarta.persistence.Table;

@Table(name = "TypeOneToManyDocOIDRoot")
public class TypeOneToManyDocOIDRootExpand extends OIDGenericData {

	public String otherData;

	@OneToManyDoc(targetEntity = TypeOneToManyDocOIDRemote.class, remoteField = "rootOid")
	@Column(nullable = false, name = "remoteIds")
	public List<TypeOneToManyDocOIDRemote> remotes;
}
