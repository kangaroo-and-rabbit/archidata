package test.kar.archidata.model;

import org.kar.archidata.annotation.DataJson;
import org.kar.archidata.model.GenericData;

public class SerializeAsJson extends GenericData {

	@DataJson
	public SimpleTable data;

}