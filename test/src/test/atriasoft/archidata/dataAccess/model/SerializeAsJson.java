package test.atriasoft.archidata.dataAccess.model;

import org.atriasoft.archidata.annotation.DataJson;
import org.atriasoft.archidata.model.GenericData;

import dev.morphia.annotations.Entity;

@Entity
public class SerializeAsJson extends GenericData {

	@DataJson
	public SimpleTable data;

}