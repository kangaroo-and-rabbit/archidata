package test.atriasoft.archidata.dataAccess.model;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

public class TypesTable {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(nullable = false, unique = true)
	public Long id = null;

	public Double doubleData;
	public Float floatData;
	public Integer intData;
	public Long longData;
	public String textData;
	@Column(length = 25)
	public String varcharData;
	public Boolean booleanData;
	public Timestamp timeStampData; // THIS is SQL we do not need to be link with it ...
	public Date dateFullData; // same as SQL time-stamp
	public LocalDate dateData; // just the Date YYYY-MM-DD
	public LocalTime timeData; // just the time HH:MM:SS.mmm
}