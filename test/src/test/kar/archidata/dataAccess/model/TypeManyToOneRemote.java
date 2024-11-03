package test.kar.archidata.dataAccess.model;

import org.kar.archidata.model.GenericData;

import dev.morphia.annotations.Entity;

@Entity
public class TypeManyToOneRemote extends GenericData {

	public String data;

	@Override
	public String toString() {
		return "TypeManyToOneRemote [data=" + this.data + ", id=" + this.id + "]";
	}

}