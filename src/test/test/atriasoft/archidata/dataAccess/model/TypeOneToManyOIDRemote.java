package test.atriasoft.archidata.dataAccess.model;

import org.atriasoft.archidata.model.OIDGenericData;
import org.bson.types.ObjectId;

import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;

public class TypeOneToManyOIDRemote extends OIDGenericData {

	@ManyToOne(fetch = FetchType.LAZY, targetEntity = TypeOneToManyOIDRoot.class)
	public ObjectId rootOid;

	public String data;

}
