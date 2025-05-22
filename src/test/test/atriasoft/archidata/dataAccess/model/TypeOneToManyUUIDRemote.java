package test.atriasoft.archidata.dataAccess.model;

import java.util.UUID;

import org.atriasoft.archidata.model.UUIDGenericData;

import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;

public class TypeOneToManyUUIDRemote extends UUIDGenericData {

	@ManyToOne(fetch = FetchType.LAZY, targetEntity = TypeOneToManyUUIDRoot.class)
	public UUID rootUuid;

	public String data;

}
