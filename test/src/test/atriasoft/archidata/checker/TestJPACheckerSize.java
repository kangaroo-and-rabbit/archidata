package test.atriasoft.archidata.checker;

import org.atriasoft.archidata.exception.InputException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import test.atriasoft.archidata.checker.model.JpaBaseModel;
import test.atriasoft.archidata.checker.model.JpaBaseModel.JpaBaseModelChecker;

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
