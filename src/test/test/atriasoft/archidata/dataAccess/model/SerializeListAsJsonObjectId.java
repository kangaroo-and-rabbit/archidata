package test.atriasoft.archidata.dataAccess.model;

import java.util.List;

import org.atriasoft.archidata.model.OIDGenericData;
import org.bson.types.ObjectId;

public class SerializeListAsJsonObjectId extends OIDGenericData {

	public List<ObjectId> data;
}