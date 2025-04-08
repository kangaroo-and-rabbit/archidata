package test.atriasoft.archidata.checker;

import org.atriasoft.archidata.exception.InputException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import test.atriasoft.archidata.checker.model.JpaBaseModel;
import test.atriasoft.archidata.checker.model.JpaBaseModel.JpaBaseModelChecker;

public class TestJPACheckerMax {

	@Test
	public void testMaxInteger() throws Exception {
		final JpaBaseModelChecker checker = new JpaBaseModelChecker();
		final JpaBaseModel data = new JpaBaseModel();
		data.testMaxInteger = 75;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testMaxInteger = 74;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testMaxInteger = 76;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testMaxInteger = 100;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
	}

	@Test
	public void testMaxIntegerObject() throws Exception {
		final JpaBaseModelChecker checker = new JpaBaseModelChecker();
		final JpaBaseModel data = new JpaBaseModel();
		data.testMaxIntegerObject = 75;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testMaxIntegerObject = 74;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testMaxIntegerObject = 76;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testMaxIntegerObject = 100;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
	}

	@Test
	public void testMaxLong() throws Exception {
		final JpaBaseModelChecker checker = new JpaBaseModelChecker();
		final JpaBaseModel data = new JpaBaseModel();
		data.testMaxLong = 75;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testMaxLong = 74;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testMaxLong = 76;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testMaxLong = 100;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
	}

	@Test
	public void testMaxLongObject() throws Exception {
		final JpaBaseModelChecker checker = new JpaBaseModelChecker();
		final JpaBaseModel data = new JpaBaseModel();
		data.testMaxLongObject = 75L;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testMaxLongObject = 74L;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testMaxLongObject = 76L;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testMaxLongObject = 100L;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
	}

	@Test
	public void testMaxFloat() throws Exception {
		final JpaBaseModelChecker checker = new JpaBaseModelChecker();
		final JpaBaseModel data = new JpaBaseModel();
		data.testMaxFloat = 75f;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testMaxFloat = 74.99f;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testMaxFloat = 75.01f;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testMaxFloat = 100f;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
	}

	@Test
	public void testMaxFloatObject() throws Exception {
		final JpaBaseModelChecker checker = new JpaBaseModelChecker();
		final JpaBaseModel data = new JpaBaseModel();
		data.testMaxFloatObject = 75f;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testMaxFloatObject = 74.99f;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testMaxFloatObject = 75.01f;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testMaxFloatObject = 100f;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
	}

	@Test
	public void testMaxDouble() throws Exception {
		final JpaBaseModelChecker checker = new JpaBaseModelChecker();
		final JpaBaseModel data = new JpaBaseModel();
		data.testMaxDouble = 75d;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testMaxDouble = 74.99d;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testMaxDouble = 75.01d;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testMaxDouble = 100d;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
	}

	@Test
	public void testMaxDoubleObject() throws Exception {
		final JpaBaseModelChecker checker = new JpaBaseModelChecker();
		final JpaBaseModel data = new JpaBaseModel();
		data.testMaxDoubleObject = 75d;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testMaxDoubleObject = 74.99d;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testMaxDoubleObject = 75.01d;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testMaxDoubleObject = 100d;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
	}
}
