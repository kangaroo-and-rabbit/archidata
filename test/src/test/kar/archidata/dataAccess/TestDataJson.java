package test.kar.archidata.dataAccess;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kar.archidata.exception.InputException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import test.kar.archidata.StepwiseExtension;
import test.kar.archidata.dataAccess.model.DataInJson;
import test.kar.archidata.dataAccess.model.DataWithSubJson;
import test.kar.archidata.dataAccess.model.DataWithSubJson.DataWithSubJsonChecker;

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
