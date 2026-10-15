package test.atriasoft.archidata.dataAccess.model;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;

import org.atriasoft.archidata.annotation.Id;

public class TypesTable {
	@Id
	public Long id = null;

	public Double doubleData;
	public Float floatData;
	public Integer intData;
	public Long longData;
	public String textData;
	public String varcharData;
	public Boolean booleanData;
	public Timestamp timeStampData; // THIS is SQL we do not need to be link with it ...
	public Date dateFullData; // same as SQL time-stamp
	public LocalDate dateData; // just the Date YYYY-MM-DD
	public LocalTime timeData; // just the time HH:MM:SS.mmm
}