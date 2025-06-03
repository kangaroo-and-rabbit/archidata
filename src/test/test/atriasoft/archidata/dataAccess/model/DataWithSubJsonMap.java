package test.atriasoft.archidata.dataAccess.model;

import java.util.Map;

import org.atriasoft.archidata.annotation.DataJson;
import org.atriasoft.archidata.model.OIDGenericData;
import org.bson.types.ObjectId;

public class DataWithSubJsonMap extends OIDGenericData {
	@DataJson
	public Map<String, Integer> mapIntegerData;
	@DataJson
	public Map<String, Long> mapLongData;
	@DataJson
	public Map<String, DataInJson> mapObjectData;
	@DataJson
	public Map<String, Enum2ForTest> mapEnumData;
	@DataJson
	public Map<String, Map<String, Enum2ForTest>> mapMapEnumData;
	@DataJson
	public Map<ObjectId, Long> mapMapKeyModifiedObjectData;
	@DataJson
	public Map<Integer, Long> mapMapKeyModifiedIntegerData;
}
