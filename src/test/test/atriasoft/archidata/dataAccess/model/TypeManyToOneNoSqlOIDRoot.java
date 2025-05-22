package test.atriasoft.archidata.dataAccess.model;

import org.atriasoft.archidata.annotation.ManyToOneNoSQL;
import org.atriasoft.archidata.model.OIDGenericData;
import org.bson.types.ObjectId;

import jakarta.persistence.Column;

public class TypeManyToOneNoSqlOIDRoot extends OIDGenericData {

	public String otherData;

	@ManyToOneNoSQL(targetEntity = TypeManyToOneNoSqlOIDRemote.class, remoteField = "remoteOids")
	@Column(nullable = false)
	public ObjectId remoteOid;
}