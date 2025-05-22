package test.atriasoft.archidata.dataAccess.model;

import java.util.List;

import org.atriasoft.archidata.annotation.DataJson;
import org.atriasoft.archidata.model.GenericData;

public class SerializeListAsJson extends GenericData {

	@DataJson
	public List<Integer> data;

}