package test.atriasoft.archidata.dataAccess.model;

import org.atriasoft.archidata.annotation.ManyToOneDoc;
import org.atriasoft.archidata.model.OIDGenericData;

import jakarta.persistence.Column;
import jakarta.persistence.Table;

@Table(name = "TypeManyToOneDocOIDRoot")
public class TypeManyToOneDocOIDRootExpand extends OIDGenericData {

	public String otherData;

	@ManyToOneDoc(targetEntity = TypeManyToOneDocOIDRemote.class, remoteField = "remoteOids")
	@Column(name = "remoteOid", nullable = false)
	public TypeManyToOneDocOIDRemote remote;
}