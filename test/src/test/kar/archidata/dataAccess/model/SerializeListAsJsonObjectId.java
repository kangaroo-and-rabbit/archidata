package test.kar.archidata.dataAccess.model;

import java.util.List;

import org.bson.types.ObjectId;
import org.kar.archidata.annotation.DataJson;
import org.kar.archidata.model.OIDGenericData;

import dev.morphia.annotations.Entity;

@Entity
public class SerializeListAsJsonObjectId extends OIDGenericData {

	@DataJson
	public List<ObjectId> data;

}