package test.atriasoft.archidata.dataAccess.model;

import org.atriasoft.archidata.annotation.ManyToOneNoSQL;
import org.atriasoft.archidata.model.OIDGenericData;

import jakarta.persistence.Column;
import jakarta.persistence.Table;

@Table(name = "TypeManyToOneNoSqlOIDRoot")
public class TypeManyToOneNoSqlOIDRootExpand extends OIDGenericData {

	public String otherData;

	@ManyToOneNoSQL(targetEntity = TypeManyToOneNoSqlOIDRemote.class, remoteField = "remoteOids")
	@Column(name = "remoteOid", nullable = false)
	public TypeManyToOneNoSqlOIDRemote remote;
}