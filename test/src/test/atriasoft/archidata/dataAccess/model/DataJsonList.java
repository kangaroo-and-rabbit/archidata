package test.atriasoft.archidata.dataAccess.model;

import java.util.List;

import org.atriasoft.archidata.annotation.DataJson;
import org.atriasoft.archidata.model.GenericData;
import org.bson.types.ObjectId;

public class DataJsonList extends GenericData {
	@DataJson()
	public List<Long> covers;
	@DataJson()
	public List<ObjectId> coversObjectId;
}
