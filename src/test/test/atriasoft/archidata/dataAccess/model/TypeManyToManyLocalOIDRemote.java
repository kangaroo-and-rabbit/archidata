package test.atriasoft.archidata.dataAccess.model;

import java.util.List;

import org.atriasoft.archidata.annotation.ManyToManyLocal;
import org.atriasoft.archidata.model.OIDGenericData;
import org.bson.types.ObjectId;

import dev.morphia.annotations.Entity;

@Entity
public class TypeManyToManyLocalOIDRemote extends OIDGenericData {
	@ManyToManyLocal(targetEntity = TypeManyToManyLocalOIDRoot.class, remoteField = "remote")
	public List<ObjectId> remoteToParent;
	public String data;

}