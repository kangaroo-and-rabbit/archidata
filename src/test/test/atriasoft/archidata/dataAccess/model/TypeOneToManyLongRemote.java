package test.atriasoft.archidata.dataAccess.model;

import org.atriasoft.archidata.model.GenericData;

import dev.morphia.annotations.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;

@Entity
public class TypeOneToManyLongRemote extends GenericData {

	@ManyToOne(fetch = FetchType.LAZY, targetEntity = TypeOneToManyLongRoot.class)
	public Long rootId;

	public String data;

}
