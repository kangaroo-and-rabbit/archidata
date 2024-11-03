package test.kar.archidata.dataAccess.model;

import org.kar.archidata.model.UUIDGenericData;

import dev.morphia.annotations.Entity;

@Entity
public class TypeManyToOneUUIDRemote extends UUIDGenericData {

	public String data;

}