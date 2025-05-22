package test.atriasoft.archidata.dataAccess.model;

import java.util.List;

import org.atriasoft.archidata.model.UUIDGenericData;

import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Table(name = "TypeManyToManyUUIDRoot")
public class TypeManyToManyUUIDRootExpand extends UUIDGenericData {

	public String otherData;

	@ManyToMany(fetch = FetchType.LAZY, targetEntity = TypeManyToManyUUIDRemote.class, mappedBy = "remoteToParent")
	public List<TypeManyToManyUUIDRemote> remote;
}
