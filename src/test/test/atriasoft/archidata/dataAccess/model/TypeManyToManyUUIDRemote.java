package test.atriasoft.archidata.dataAccess.model;

import java.util.List;
import java.util.UUID;

import org.atriasoft.archidata.model.UUIDGenericData;

import dev.morphia.annotations.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;

@Entity
public class TypeManyToManyUUIDRemote extends UUIDGenericData {
	@ManyToMany(fetch = FetchType.LAZY, targetEntity = TypeManyToManyUUIDRoot.class, mappedBy = "remote")
	public List<UUID> remoteToParent;
	public String data;

}