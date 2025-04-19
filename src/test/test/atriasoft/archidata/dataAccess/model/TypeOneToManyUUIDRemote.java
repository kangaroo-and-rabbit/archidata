package test.atriasoft.archidata.dataAccess.model;

import java.util.UUID;

import org.atriasoft.archidata.model.UUIDGenericData;

import dev.morphia.annotations.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;

@Entity
public class TypeOneToManyUUIDRemote extends UUIDGenericData {

	@ManyToOne(fetch = FetchType.LAZY, targetEntity = TypeOneToManyUUIDRoot.class)
	public UUID rootUuid;

	public String data;

}
