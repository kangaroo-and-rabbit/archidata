package test.kar.archidata.dataAccess.model;

import org.kar.archidata.model.GenericData;

import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;

public class TypeOneToManyRemote extends GenericData {

	@ManyToOne(fetch = FetchType.LAZY, targetEntity = TypeOneToManyRoot.class)
	public Long rootId;

	public String data;

}
