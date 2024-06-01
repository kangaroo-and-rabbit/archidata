package test.kar.archidata.model;

import org.kar.archidata.model.GenericData;

public class TypeManyToOneRemote extends GenericData {

	public String data;

	@Override
	public String toString() {
		return "TypeManyToOneRemote [data=" + this.data + ", id=" + this.id + "]";
	}

}