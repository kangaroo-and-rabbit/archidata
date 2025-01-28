package test.kar.archidata.dataAccess.model;

import org.kar.archidata.annotation.DataJson;
import org.kar.archidata.annotation.checker.Checker;
import org.kar.archidata.checker.CheckJPA;

public class DataWithSubJson {
	// Simple checker declaration
	public static class DataWithSubJsonChecker extends CheckJPA<DataWithSubJson> {
		public DataWithSubJsonChecker() {
			super(DataWithSubJson.class);
		}
	}

	@DataJson()
	@Checker(DataInJson.DataInJsonChecker.class)
	public DataInJson dataSerialized;
}
