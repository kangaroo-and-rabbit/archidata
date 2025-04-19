package test.atriasoft.archidata.dataAccess.model;

import org.atriasoft.archidata.checker.CheckJPA;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class DataInJson {
	// Simple checker declaration
	public static class DataInJsonChecker extends CheckJPA<DataInJson> {
		public DataInJsonChecker() {
			super(DataInJson.class);
		}
	}

	// Simple data to verify if the checker is active
	@Size(min = 3, max = 128)
	@Pattern(regexp = "^[0-9]+$")
	public String data;
}
