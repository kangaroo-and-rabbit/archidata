package test.kar.archidata.dataAccess.model;

import java.util.List;

import org.bson.types.ObjectId;
import org.kar.archidata.model.OIDGenericData;

import dev.morphia.annotations.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;

@Entity
public class TypeManyToManyOIDRemote extends OIDGenericData {
	@ManyToMany(fetch = FetchType.LAZY, targetEntity = TypeManyToManyOIDRoot.class, mappedBy = "remote")
	public List<ObjectId> remoteToParent;
	public String data;

}