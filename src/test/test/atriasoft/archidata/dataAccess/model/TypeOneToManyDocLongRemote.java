package test.atriasoft.archidata.dataAccess.model;

import org.atriasoft.archidata.model.GenericData;

public class TypeOneToManyDocLongRemote extends GenericData {
	public String data;
	public Long parentId;

	public TypeOneToManyDocLongRemote() {}

	public TypeOneToManyDocLongRemote(String data, Long parentId) {
		this.data = data;
		this.parentId = parentId;
	}
}
