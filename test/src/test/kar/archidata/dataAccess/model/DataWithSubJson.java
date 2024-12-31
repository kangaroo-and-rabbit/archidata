package test.kar.archidata.dataAccess.model;

import org.kar.archidata.annotation.DataJson;
import org.kar.archidata.dataAccess.options.CheckJPA;

public class DataWithSubJson {
	// Simple checker declaration
	public static class DataWithSubJsonChecker extends CheckJPA<DataWithSubJson> {
		public DataWithSubJsonChecker() {
			super(DataWithSubJson.class);
		}
	}

	@DataJson(checker = DataInJson.DataInJsonChecker.class)
	public DataInJson dataSerialized;
}
