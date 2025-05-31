package test.atriasoft.archidata.dataAccess.model;

import java.util.List;

import org.atriasoft.archidata.annotation.DataJson;
import org.atriasoft.archidata.model.OIDGenericData;

public class DataWithSubJsonList extends OIDGenericData {

	@DataJson
	public List<Integer> listIntegerData;
	@DataJson
	public List<Long> listLongData;
	@DataJson
	public List<DataInJson> listObjectData;
	@DataJson
	public List<Enum2ForTest> listEnumData;
}
