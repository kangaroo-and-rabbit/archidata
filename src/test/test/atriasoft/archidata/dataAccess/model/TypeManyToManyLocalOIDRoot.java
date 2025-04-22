package test.atriasoft.archidata.dataAccess.model;

import java.util.List;

import org.atriasoft.archidata.annotation.ManyToManyLocal;
import org.atriasoft.archidata.model.OIDGenericData;
import org.bson.types.ObjectId;

import dev.morphia.annotations.Entity;

@Entity
public class TypeManyToManyLocalOIDRoot extends OIDGenericData {

	public String otherData;

	@ManyToManyLocal(targetEntity = TypeManyToManyLocalOIDRemote.class, remoteField = "remoteToParent")
	public List<ObjectId> remote;
}
