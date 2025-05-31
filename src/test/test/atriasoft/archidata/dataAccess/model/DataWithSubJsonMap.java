package test.atriasoft.archidata.dataAccess.model;

import java.util.Map;

import org.atriasoft.archidata.annotation.DataJson;
import org.atriasoft.archidata.model.OIDGenericData;

public class DataWithSubJsonMap extends OIDGenericData {
	@DataJson
	public Map<String, Integer> mapIntegerData;
	@DataJson
	public Map<String, Long> mapLongData;
	@DataJson
	public Map<String, DataInJson> mapObjectData;
	@DataJson
	public Map<String, Enum2ForTest> mapEnumData;
}
