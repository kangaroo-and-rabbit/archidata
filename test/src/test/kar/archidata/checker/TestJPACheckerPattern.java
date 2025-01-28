package test.kar.archidata.checker;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.kar.archidata.exception.InputException;

import test.kar.archidata.checker.model.JpaBaseModel;
import test.kar.archidata.checker.model.JpaBaseModel.JpaBaseModelChecker;

public class TestJPACheckerPattern {

	@Test
	public void testPattern() throws Exception {
		final JpaBaseModelChecker checker = new JpaBaseModelChecker();
		final JpaBaseModel data = new JpaBaseModel();
		data.testPattern = "0";
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testPattern = "1234567890";
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testPattern = "q";
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testPattern = "qsdf4653";
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
	}

}
