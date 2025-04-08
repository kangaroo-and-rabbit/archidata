package test.atriasoft.archidata.dataAccess.model;

import org.atriasoft.archidata.annotation.DataJson;
import org.atriasoft.archidata.annotation.checker.Checker;
import org.atriasoft.archidata.checker.CheckJPA;

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
