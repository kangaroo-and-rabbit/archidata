package test.kar.archidata.dataAccess.model;

import java.util.List;

import org.kar.archidata.annotation.DataJson;
import org.kar.archidata.model.GenericData;

import dev.morphia.annotations.Entity;

@Entity
public class SerializeListAsJson extends GenericData {

	@DataJson
	public List<Integer> data;

}