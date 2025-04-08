package test.atriasoft.archidata.dataAccess.model;

import org.atriasoft.archidata.model.OIDGenericData;

import dev.morphia.annotations.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Table(name = "TypeManyToOneOIDRoot")
//for Mongo
@Entity(value = "TypeManyToOneOIDRoot")
public class TypeManyToOneOIDRootExpand extends OIDGenericData {

	public String otherData;

	@ManyToOne(fetch = FetchType.LAZY, targetEntity = TypeManyToOneOIDRemote.class)
	@Column(name = "remoteOid", nullable = false)
	public TypeManyToOneOIDRemote remote;
}