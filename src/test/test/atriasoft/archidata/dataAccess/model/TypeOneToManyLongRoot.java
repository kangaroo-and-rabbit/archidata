package test.atriasoft.archidata.dataAccess.model;

import java.util.List;

import org.atriasoft.archidata.model.GenericData;

import dev.morphia.annotations.Entity;
import jakarta.persistence.OneToMany;

@Entity
public class TypeOneToManyLongRoot extends GenericData {

	public String otherData;

	@OneToMany(targetEntity = TypeOneToManyLongRemote.class, mappedBy = "rootId")
	public List<Long> remoteIds;
}