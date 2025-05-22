package test.atriasoft.archidata.dataAccess.model;

import java.util.List;

import org.atriasoft.archidata.model.GenericData;

import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;

public class TypeManyToManyLongRemote extends GenericData {
	@ManyToMany(fetch = FetchType.LAZY, targetEntity = TypeManyToManyLongRoot.class, mappedBy = "remote")
	public List<Long> remoteToParent;
	public String data;

}