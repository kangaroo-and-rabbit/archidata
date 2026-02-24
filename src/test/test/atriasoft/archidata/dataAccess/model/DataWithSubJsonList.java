package test.atriasoft.archidata.dataAccess.model;

import java.util.List;

import org.atriasoft.archidata.model.OIDGenericData;

public class DataWithSubJsonList extends OIDGenericData {

	public List<Integer> listIntegerData;
	public List<Long> listLongData;
	public List<DataInJson> listObjectData;
	public List<Enum2ForTest> listEnumData;
}
