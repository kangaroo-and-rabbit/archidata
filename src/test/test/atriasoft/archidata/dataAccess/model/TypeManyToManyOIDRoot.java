package test.atriasoft.archidata.dataAccess.model;

import java.util.List;

import org.atriasoft.archidata.model.OIDGenericData;
import org.bson.types.ObjectId;

import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;

public class TypeManyToManyOIDRoot extends OIDGenericData {

	public String otherData;

	@ManyToMany(fetch = FetchType.LAZY, targetEntity = TypeManyToManyOIDRemote.class, mappedBy = "remoteToParent")
	public List<ObjectId> remote;
}
