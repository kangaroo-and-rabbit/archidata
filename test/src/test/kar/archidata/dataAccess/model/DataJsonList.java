package test.kar.archidata.dataAccess.model;

import java.util.List;

import org.bson.types.ObjectId;
import org.kar.archidata.annotation.DataJson;
import org.kar.archidata.model.GenericData;

public class DataJsonList extends GenericData {
	@DataJson()
	public List<Long> covers;
	@DataJson()
	public List<ObjectId> coversObjectId;
}
