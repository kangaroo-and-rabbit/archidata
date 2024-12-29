package test.kar.archidata.dataAccess.model;

import org.kar.archidata.model.GenericData;

import dev.morphia.annotations.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;

@Entity
public class TypeOneToManyRemote extends GenericData {

	@ManyToOne(fetch = FetchType.LAZY, targetEntity = TypeOneToManyRoot.class)
	public Long rootId;

	public String data;

}
