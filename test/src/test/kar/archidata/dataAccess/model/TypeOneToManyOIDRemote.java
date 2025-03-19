package test.kar.archidata.dataAccess.model;

import org.bson.types.ObjectId;
import org.kar.archidata.model.OIDGenericData;

import dev.morphia.annotations.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;

@Entity
public class TypeOneToManyOIDRemote extends OIDGenericData {

	@ManyToOne(fetch = FetchType.LAZY, targetEntity = TypeOneToManyOIDRoot.class)
	public ObjectId rootOid;

	public String data;

}
