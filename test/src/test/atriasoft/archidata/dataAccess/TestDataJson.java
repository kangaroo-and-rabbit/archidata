package test.atriasoft.archidata.dataAccess;

import org.atriasoft.archidata.exception.InputException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import test.atriasoft.archidata.StepwiseExtension;
import test.atriasoft.archidata.dataAccess.model.DataInJson;
import test.atriasoft.archidata.dataAccess.model.DataWithSubJson;
import test.atriasoft.archidata.dataAccess.model.DataWithSubJson.DataWithSubJsonChecker;

@ExtendWith(StepwiseExtension.class)
public class TestDataJson {
	final static private Logger LOGGER = LoggerFactory.getLogger(TestDataJson.class);

	@Test
	public void testCheckerDoesNotThrow() throws Exception {
		final DataWithSubJson data = new DataWithSubJson();
		data.dataSerialized = new DataInJson();
		data.dataSerialized.data = "65454";
		final DataWithSubJsonChecker checker = new DataWithSubJsonChecker();
		Assertions.assertDoesNotThrow(() -> {
			checker.check(null, "", data);
		});
	}

	@Test
	public void testCheckerDoesThrow() throws Exception {
		final DataWithSubJson data = new DataWithSubJson();
		data.dataSerialized = new DataInJson();
		data.dataSerialized.data = "lqksjdflkjqsdf";
		final DataWithSubJsonChecker checker = new DataWithSubJsonChecker();
		final InputException res = Assertions.assertThrows(InputException.class, () -> {
			checker.check(null, "", data);
		});
		Assertions.assertEquals(res.getMessage(),
				"does not match the required pattern (constraints) must be '^[0-9]+$'");

	}

}
