package test.atriasoft.archidata.dataAccess.model;

import org.atriasoft.archidata.model.OIDGenericData;
import org.bson.types.ObjectId;

import jakarta.persistence.Column;
import jakarta.persistence.ManyToOne;

public class TypeManyToOneOIDRoot extends OIDGenericData {

	public String otherData;

	@ManyToOne(targetEntity = TypeManyToOneOIDRemote.class)
	@Column(nullable = false)
	public ObjectId remoteOid;
}