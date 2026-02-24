package test.atriasoft.archidata.dataAccess.model;

import java.util.Map;

import org.atriasoft.archidata.model.OIDGenericData;
import org.bson.types.ObjectId;

public class DataWithSubJsonMap extends OIDGenericData {
	public Map<String, Integer> mapIntegerData;
	public Map<String, Long> mapLongData;
	public Map<String, DataInJson> mapObjectData;
	public Map<String, Enum2ForTest> mapEnumData;
	public Map<String, Map<String, Enum2ForTest>> mapMapEnumData;
	public Map<ObjectId, Long> mapMapKeyModifiedObjectData;
	public Map<Integer, Long> mapMapKeyModifiedIntegerData;
}
