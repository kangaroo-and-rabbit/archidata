package test.kar.archidata.dataAccess.model;

import java.util.UUID;

import org.kar.archidata.model.UUIDGenericData;

import dev.morphia.annotations.Entity;
import jakarta.persistence.Column;
import jakarta.persistence.ManyToOne;

@Entity
public class TypeManyToOneUUIDRoot extends UUIDGenericData {

	public String otherData;

	@ManyToOne(targetEntity = TypeManyToOneUUIDRemote.class)
	@Column(nullable = false)
	public UUID remoteUuid;
}