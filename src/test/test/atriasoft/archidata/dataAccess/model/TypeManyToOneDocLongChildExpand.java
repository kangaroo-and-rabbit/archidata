package test.atriasoft.archidata.dataAccess.model;

import org.atriasoft.archidata.annotation.Collection;
import org.atriasoft.archidata.annotation.Field;
import org.atriasoft.archidata.annotation.ManyToOneDoc;
import org.atriasoft.archidata.model.GenericData;

@Collection("TypeManyToOneDocLongChild")
public class TypeManyToOneDocLongChildExpand extends GenericData {
	public String otherData;

	@ManyToOneDoc(targetEntity = TypeManyToOneDocLongParentIgnore.class, remoteField = "childIds")
	@Field("parentId")
	public TypeManyToOneDocLongParentIgnore parent;
}
