package test.atriasoft.archidata.dataAccess.model;

import java.util.List;

import org.atriasoft.archidata.model.GenericData;

import dev.morphia.annotations.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Table(name = "TypeManyToManyLongRoot")
// for Mongo
@Entity(value = "TypeManyToManyLongRoot")
public class TypeManyToManyLongRootExpand extends GenericData {

	public String otherData;

	@ManyToMany(fetch = FetchType.LAZY, targetEntity = TypeManyToManyLongRemote.class)
	public List<TypeManyToManyLongRemote> remote;
}
