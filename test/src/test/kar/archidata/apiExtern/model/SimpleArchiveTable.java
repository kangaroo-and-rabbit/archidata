package test.kar.archidata.apiExtern.model;

import java.util.Date;

import org.kar.archidata.model.GenericData;

import com.fasterxml.jackson.annotation.JsonFormat;

public class SimpleArchiveTable extends GenericData {
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
	public Date archive;
	public String name;

}