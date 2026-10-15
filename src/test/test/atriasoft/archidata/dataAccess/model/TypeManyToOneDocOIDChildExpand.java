package test.atriasoft.archidata.dataAccess.model;

import org.atriasoft.archidata.annotation.Collection;
import org.atriasoft.archidata.annotation.Field;
import org.atriasoft.archidata.annotation.ManyToOneDoc;
import org.atriasoft.archidata.model.OIDGenericData;

@Collection("TypeManyToOneDocOIDChild")
public class TypeManyToOneDocOIDChildExpand extends OIDGenericData {

	public String otherData;

	@ManyToOneDoc(targetEntity = TypeManyToOneDocOIDParentIgnore.class, remoteField = "childOids")
	@Field("parentOid")
	public TypeManyToOneDocOIDParentIgnore parent;
}