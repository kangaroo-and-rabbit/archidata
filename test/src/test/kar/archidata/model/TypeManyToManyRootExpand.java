package test.kar.archidata.model;

import java.util.List;

import org.kar.archidata.model.GenericData;

import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Table(name = "TypeManyToManyRoot")
public class TypeManyToManyRootExpand extends GenericData {

	public String otherData;

	@ManyToMany(fetch = FetchType.LAZY, targetEntity = TypeManyToManyRemote.class)
	public List<TypeManyToManyRemote> remote;
}
