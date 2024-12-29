package test.kar.archidata.dataAccess.model;

import java.util.List;
import java.util.UUID;

import org.kar.archidata.model.UUIDGenericData;

import dev.morphia.annotations.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.OneToMany;

@Entity
public class TypeOneToManyUUIDRoot extends UUIDGenericData {

	public String otherData;

	@OneToMany(targetEntity = TypeOneToManyUUIDRemote.class, mappedBy = "rootUuid")
	@Column(nullable = false)
	public List<UUID> remoteIds;
}