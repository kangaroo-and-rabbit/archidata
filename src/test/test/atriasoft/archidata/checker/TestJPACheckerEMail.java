package test.atriasoft.archidata.checker;

import org.atriasoft.archidata.exception.InputException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import test.atriasoft.archidata.checker.model.JpaBaseModel;
import test.atriasoft.archidata.checker.model.JpaBaseModel.JpaBaseModelChecker;

public class TestJPACheckerEMail {

	@Test
	public void testEMail() throws Exception {
		final JpaBaseModelChecker checker = new JpaBaseModelChecker();
		final JpaBaseModel data = new JpaBaseModel();
		data.testEMail = "s@s.ds";
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testEMail = "yuio.sdf@sqdf.com";
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testEMail = "s@s.s";
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testEMail = "sq@qsd";
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testEMail = "sqsdfsdf";
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testEMail = "56465456";
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
	}

}
