package test.atriasoft.archidata.dataAccess.model;

import java.util.List;

import org.atriasoft.archidata.model.UUIDGenericData;

import dev.morphia.annotations.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Table(name = "TypeManyToManyUUIDRoot")
// for Mongo
@Entity(value = "TypeManyToManyUUIDRoot")
public class TypeManyToManyUUIDRootExpand extends UUIDGenericData {

	public String otherData;

	@ManyToMany(fetch = FetchType.LAZY, targetEntity = TypeManyToManyUUIDRemote.class, mappedBy = "remoteToParent")
	public List<TypeManyToManyUUIDRemote> remote;
}
