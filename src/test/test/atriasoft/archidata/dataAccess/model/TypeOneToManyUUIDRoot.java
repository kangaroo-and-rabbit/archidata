package test.atriasoft.archidata.dataAccess.model;

import java.util.List;
import java.util.UUID;

import org.atriasoft.archidata.model.UUIDGenericData;

import jakarta.persistence.Column;
import jakarta.persistence.OneToMany;

public class TypeOneToManyUUIDRoot extends UUIDGenericData {

	public String otherData;

	@OneToMany(targetEntity = TypeOneToManyUUIDRemote.class, mappedBy = "rootUuid")
	@Column(nullable = false)
	public List<UUID> remoteIds;
}