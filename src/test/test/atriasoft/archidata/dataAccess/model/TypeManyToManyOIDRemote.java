package test.atriasoft.archidata.dataAccess.model;

import java.util.List;

import org.atriasoft.archidata.model.OIDGenericData;
import org.bson.types.ObjectId;

import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;

public class TypeManyToManyOIDRemote extends OIDGenericData {
	@ManyToMany(fetch = FetchType.LAZY, targetEntity = TypeManyToManyOIDRoot.class, mappedBy = "remote")
	public List<ObjectId> remoteToParent;
	public String data;

}