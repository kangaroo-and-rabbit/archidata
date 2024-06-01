package test.kar.archidata.model;

import java.util.UUID;

import org.kar.archidata.model.UUIDGenericData;

import jakarta.persistence.Column;
import jakarta.persistence.ManyToOne;

public class TypeManyToOneUUIDRoot extends UUIDGenericData {

	public String otherData;

	@ManyToOne(targetEntity = TypeManyToOneUUIDRemote.class)
	@Column(nullable = false)
	public UUID remoteUuid;
}