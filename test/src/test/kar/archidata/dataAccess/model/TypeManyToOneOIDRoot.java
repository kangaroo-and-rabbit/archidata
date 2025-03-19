package test.kar.archidata.dataAccess.model;

import org.bson.types.ObjectId;
import org.kar.archidata.model.OIDGenericData;

import dev.morphia.annotations.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.ManyToOne;

@Entity
public class TypeManyToOneOIDRoot extends OIDGenericData {

	public String otherData;

	@ManyToOne(targetEntity = TypeManyToOneOIDRemote.class)
	@Column(nullable = false)
	public ObjectId remoteOid;
}