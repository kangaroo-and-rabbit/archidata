package test.kar.archidata.dataAccess.model;

import java.util.List;

import org.kar.archidata.model.GenericData;

import dev.morphia.annotations.Entity;
import jakarta.persistence.OneToMany;

@Entity
public class TypeOneToManyRoot extends GenericData {

	public String otherData;

	@OneToMany(targetEntity = TypeOneToManyRemote.class, mappedBy = "rootId")
	public List<Long> remoteIds;
}