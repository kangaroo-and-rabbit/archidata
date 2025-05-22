package test.atriasoft.archidata.dataAccess.model;

import org.atriasoft.archidata.model.GenericData;

public class TypeManyToOneLongRemote extends GenericData {

	public String data;

	@Override
	public String toString() {
		return "TypeManyToOneRemote [data=" + this.data + ", id=" + this.id + "]";
	}

}