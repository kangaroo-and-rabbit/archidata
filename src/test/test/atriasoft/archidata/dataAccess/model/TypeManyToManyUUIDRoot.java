package test.atriasoft.archidata.dataAccess.model;

import java.util.List;
import java.util.UUID;

import org.atriasoft.archidata.model.UUIDGenericData;

import dev.morphia.annotations.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;

@Entity
public class TypeManyToManyUUIDRoot extends UUIDGenericData {

	public String otherData;

	@ManyToMany(fetch = FetchType.LAZY, targetEntity = TypeManyToManyUUIDRemote.class, mappedBy = "remoteToParent")
	public List<UUID> remote;
}
