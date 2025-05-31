package test.atriasoft.archidata.dataAccess.model;

import java.util.List;

import org.atriasoft.archidata.annotation.DataJson;
import org.atriasoft.archidata.model.OIDGenericData;

public class SerializeListAsJson extends OIDGenericData {

	@DataJson
	public List<Integer> data;

}