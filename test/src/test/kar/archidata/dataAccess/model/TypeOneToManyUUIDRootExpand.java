package test.kar.archidata.dataAccess.model;

import java.util.List;

import org.kar.archidata.model.UUIDGenericData;

import dev.morphia.annotations.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Table(name = "TypeOneToManyUUIDRoot")
//for Mongo
@Entity(value = "TypeOneToManyUUIDRoot")
public class TypeOneToManyUUIDRootExpand extends UUIDGenericData {

	public String otherData;

	@OneToMany(targetEntity = TypeOneToManyUUIDRemote.class, mappedBy = "rootUuid")
	@Column(nullable = false)
	public List<TypeOneToManyUUIDRemote> remotes;
}
