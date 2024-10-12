package test.kar.archidata.dataAccess.model;

import org.kar.archidata.model.UUIDGenericData;

import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Table(name = "TypeManyToOneUUIDRoot")
public class TypeManyToOneUUIDRootExpand extends UUIDGenericData {

	public String otherData;

	@ManyToOne(fetch = FetchType.LAZY, targetEntity = TypeManyToOneUUIDRemote.class)
	@Column(name = "remoteUuid", nullable = false)
	public TypeManyToOneUUIDRemote remote;
}