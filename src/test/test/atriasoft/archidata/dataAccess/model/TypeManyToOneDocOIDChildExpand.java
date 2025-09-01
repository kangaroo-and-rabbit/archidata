package test.atriasoft.archidata.dataAccess.model;

import org.atriasoft.archidata.annotation.ManyToOneDoc;
import org.atriasoft.archidata.model.OIDGenericData;

import jakarta.persistence.Column;
import jakarta.persistence.Table;

@Table(name = "TypeManyToOneDocOIDChild")
public class TypeManyToOneDocOIDChildExpand extends OIDGenericData {

	public String otherData;

	@ManyToOneDoc(targetEntity = TypeManyToOneDocOIDParentIgnore.class, remoteField = "childOids")
	@Column(name = "parentOid", nullable = false)
	public TypeManyToOneDocOIDParentIgnore parent;
}