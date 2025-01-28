package test.kar.archidata.checker;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.kar.archidata.exception.InputException;

import test.kar.archidata.checker.model.JpaBaseModel;
import test.kar.archidata.checker.model.JpaBaseModel.JpaBaseModelChecker;

public class TestJPACheckerMin {

	@Test
	public void testMinInteger() throws Exception {
		final JpaBaseModelChecker checker = new JpaBaseModelChecker();
		final JpaBaseModel data = new JpaBaseModel();
		data.testMinInteger = -75;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testMinInteger = -74;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testMinInteger = -76;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testMinInteger = -100;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
	}

	@Test
	public void testMinIntegerObject() throws Exception {
		final JpaBaseModelChecker checker = new JpaBaseModelChecker();
		final JpaBaseModel data = new JpaBaseModel();
		data.testMinIntegerObject = -75;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testMinIntegerObject = -74;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testMinIntegerObject = -76;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testMinIntegerObject = -100;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
	}

	@Test
	public void testMinLong() throws Exception {
		final JpaBaseModelChecker checker = new JpaBaseModelChecker();
		final JpaBaseModel data = new JpaBaseModel();
		data.testMinLong = -75;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testMinLong = -74;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testMinLong = -76;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testMinLong = -100;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
	}

	@Test
	public void testMinLongObject() throws Exception {
		final JpaBaseModelChecker checker = new JpaBaseModelChecker();
		final JpaBaseModel data = new JpaBaseModel();
		data.testMinLongObject = -75L;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testMinLongObject = -74L;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testMinLongObject = -76L;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testMinLongObject = -100L;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
	}

	@Test
	public void testMinFloat() throws Exception {
		final JpaBaseModelChecker checker = new JpaBaseModelChecker();
		final JpaBaseModel data = new JpaBaseModel();
		data.testMinFloat = -75f;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testMinFloat = -74.99f;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testMinFloat = -75.01f;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testMinFloat = -100f;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
	}

	@Test
	public void testMinFloatObject() throws Exception {
		final JpaBaseModelChecker checker = new JpaBaseModelChecker();
		final JpaBaseModel data = new JpaBaseModel();
		data.testMinFloatObject = -75f;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testMinFloatObject = -74.99f;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testMinFloatObject = -75.01f;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testMinFloatObject = -100f;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
	}

	@Test
	public void testMinDouble() throws Exception {
		final JpaBaseModelChecker checker = new JpaBaseModelChecker();
		final JpaBaseModel data = new JpaBaseModel();
		data.testMinDouble = -75d;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testMinDouble = -74.99d;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testMinDouble = -75.01d;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testMinDouble = -100d;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
	}

	@Test
	public void testMinDoubleObject() throws Exception {
		final JpaBaseModelChecker checker = new JpaBaseModelChecker();
		final JpaBaseModel data = new JpaBaseModel();
		data.testMinDoubleObject = -75d;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testMinDoubleObject = -74.99d;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testMinDoubleObject = -75.01d;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testMinDoubleObject = -100d;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
	}
}
