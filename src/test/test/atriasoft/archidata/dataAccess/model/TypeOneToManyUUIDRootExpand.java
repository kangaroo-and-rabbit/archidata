package test.atriasoft.archidata.dataAccess.model;

import java.util.List;

import org.atriasoft.archidata.model.UUIDGenericData;

import jakarta.persistence.Column;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Table(name = "TypeOneToManyUUIDRoot")
public class TypeOneToManyUUIDRootExpand extends UUIDGenericData {

	public String otherData;

	@OneToMany(targetEntity = TypeOneToManyUUIDRemote.class, mappedBy = "rootUuid")
	@Column(nullable = false)
	public List<TypeOneToManyUUIDRemote> remotes;
}
