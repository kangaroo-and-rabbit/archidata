package test.kar.archidata.dataAccess.model;

import java.util.List;

import org.bson.types.ObjectId;
import org.kar.archidata.model.OIDGenericData;

import dev.morphia.annotations.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.OneToMany;

@Entity
public class TypeOneToManyOIDRoot extends OIDGenericData {

	public String otherData;

	@OneToMany(targetEntity = TypeOneToManyOIDRemote.class, mappedBy = "rootOid")
	@Column(nullable = false)
	public List<ObjectId> remoteIds;
}