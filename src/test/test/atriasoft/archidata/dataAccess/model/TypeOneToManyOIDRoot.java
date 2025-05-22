package test.atriasoft.archidata.dataAccess.model;

import java.util.List;

import org.atriasoft.archidata.model.OIDGenericData;
import org.bson.types.ObjectId;

import jakarta.persistence.Column;
import jakarta.persistence.OneToMany;

public class TypeOneToManyOIDRoot extends OIDGenericData {

	public String otherData;

	@OneToMany(targetEntity = TypeOneToManyOIDRemote.class, mappedBy = "rootOid")
	@Column(nullable = false)
	public List<ObjectId> remoteIds;
}