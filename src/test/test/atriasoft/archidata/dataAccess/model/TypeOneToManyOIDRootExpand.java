package test.atriasoft.archidata.dataAccess.model;

import java.util.List;

import org.atriasoft.archidata.model.OIDGenericData;

import dev.morphia.annotations.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Table(name = "TypeOneToManyOIDRoot")
//for Mongo
@Entity(value = "TypeOneToManyOIDRoot")
public class TypeOneToManyOIDRootExpand extends OIDGenericData {

	public String otherData;

	@OneToMany(targetEntity = TypeOneToManyOIDRemote.class, mappedBy = "rootOid")
	@Column(nullable = false)
	public List<TypeOneToManyOIDRemote> remotes;
}
