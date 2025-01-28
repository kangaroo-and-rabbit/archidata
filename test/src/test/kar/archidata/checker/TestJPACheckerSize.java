package test.kar.archidata.checker;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.kar.archidata.exception.InputException;

import test.kar.archidata.checker.model.JpaBaseModel;
import test.kar.archidata.checker.model.JpaBaseModel.JpaBaseModelChecker;

public class TestJPACheckerSize {

	@Test
	public void testSize() throws Exception {
		final JpaBaseModelChecker checker = new JpaBaseModelChecker();
		final JpaBaseModel data = new JpaBaseModel();
		data.testSize = "000";
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testSize = "00000000";
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testSize = "00";
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testSize = "000000000";
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
	}

}
