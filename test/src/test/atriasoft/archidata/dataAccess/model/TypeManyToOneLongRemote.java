package test.atriasoft.archidata.dataAccess.model;

import org.atriasoft.archidata.model.GenericData;

import dev.morphia.annotations.Entity;

@Entity
public class TypeManyToOneLongRemote extends GenericData {

	public String data;

	@Override
	public String toString() {
		return "TypeManyToOneRemote [data=" + this.data + ", id=" + this.id + "]";
	}

}