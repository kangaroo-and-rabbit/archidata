package test.kar.archidata.model;

import java.util.List;

import org.kar.archidata.model.GenericData;

import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;

public class TypeManyToManyRemote extends GenericData {
	@ManyToMany(fetch = FetchType.LAZY, targetEntity = TypeManyToManyRoot.class, mappedBy = "remote")
	public List<Long> remoteToParent;
	public String data;

}