package test.atriasoft.archidata.dataAccess.model;

import java.util.List;

import org.atriasoft.archidata.model.OIDGenericData;

import dev.morphia.annotations.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Table(name = "TypeManyToManyOIDRoot")
// for Mongo
@Entity(value = "TypeManyToManyOIDRoot")
public class TypeManyToManyOIDRootExpand extends OIDGenericData {

	public String otherData;

	@ManyToMany(fetch = FetchType.LAZY, targetEntity = TypeManyToManyOIDRemote.class)
	public List<TypeManyToManyOIDRemote> remote;
}
