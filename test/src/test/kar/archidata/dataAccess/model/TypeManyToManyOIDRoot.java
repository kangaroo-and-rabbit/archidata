package test.kar.archidata.dataAccess.model;

import java.util.List;

import org.bson.types.ObjectId;
import org.kar.archidata.model.OIDGenericData;

import dev.morphia.annotations.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;

@Entity
public class TypeManyToManyOIDRoot extends OIDGenericData {

	public String otherData;

	@ManyToMany(fetch = FetchType.LAZY, targetEntity = TypeManyToManyOIDRemote.class)
	public List<ObjectId> remote;
}
