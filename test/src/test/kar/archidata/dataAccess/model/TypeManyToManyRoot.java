package test.kar.archidata.dataAccess.model;

import java.util.List;

import org.kar.archidata.model.GenericData;

import dev.morphia.annotations.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;

@Entity
public class TypeManyToManyRoot extends GenericData {

	public String otherData;

	@ManyToMany(fetch = FetchType.LAZY, targetEntity = TypeManyToManyRemote.class)
	public List<Long> remote;
}
