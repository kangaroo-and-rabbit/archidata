package test.atriasoft.archidata.dataAccess.model;

import java.util.List;

import org.atriasoft.archidata.annotation.ManyToManyLocal;
import org.atriasoft.archidata.model.OIDGenericData;

import dev.morphia.annotations.Entity;
import jakarta.persistence.Table;

@Table(name = "TypeManyToManyLocalOIDRoot")
// for Mongo
@Entity(value = "TypeManyToManyLocalOIDRoot")
public class TypeManyToManyLocalOIDRootExpand extends OIDGenericData {

	public String otherData;

	@ManyToManyLocal(targetEntity = TypeManyToManyLocalOIDRemote.class, remoteField = "remoteToParent")
	public List<TypeManyToManyLocalOIDRemote> remote;
}
