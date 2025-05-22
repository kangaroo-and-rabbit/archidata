package test.atriasoft.archidata.dataAccess.model;

import java.util.List;

import org.atriasoft.archidata.model.OIDGenericData;

import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Table(name = "TypeManyToManyOIDRoot")
public class TypeManyToManyOIDRootExpand extends OIDGenericData {

	public String otherData;

	@ManyToMany(fetch = FetchType.LAZY, targetEntity = TypeManyToManyOIDRemote.class, mappedBy = "remoteToParent")
	public List<TypeManyToManyOIDRemote> remote;
}
