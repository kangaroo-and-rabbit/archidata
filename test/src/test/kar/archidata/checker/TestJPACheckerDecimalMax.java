package test.kar.archidata.checker;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.kar.archidata.exception.InputException;

import test.kar.archidata.checker.model.JpaBaseModel;
import test.kar.archidata.checker.model.JpaBaseModel.JpaBaseModelChecker;

public class TestJPACheckerDecimalMax {

	@Test
	public void testDecimalMaxIncludeInteger() throws Exception {
		final JpaBaseModelChecker checker = new JpaBaseModelChecker();
		final JpaBaseModel data = new JpaBaseModel();
		data.testDecimalMaxIncludeInteger = 75;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testDecimalMaxIncludeInteger = 74;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testDecimalMaxIncludeInteger = 76;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testDecimalMaxIncludeInteger = 100;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
	}

	@Test
	public void testDecimalMaxIncludeIntegerObject() throws Exception {
		final JpaBaseModelChecker checker = new JpaBaseModelChecker();
		final JpaBaseModel data = new JpaBaseModel();
		data.testDecimalMaxIncludeIntegerObject = 75;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testDecimalMaxIncludeIntegerObject = 74;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testDecimalMaxIncludeIntegerObject = 76;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testDecimalMaxIncludeIntegerObject = 100;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
	}

	@Test
	public void testDecimalMaxIncludeLong() throws Exception {
		final JpaBaseModelChecker checker = new JpaBaseModelChecker();
		final JpaBaseModel data = new JpaBaseModel();
		data.testDecimalMaxIncludeLong = 75;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testDecimalMaxIncludeLong = 74;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testDecimalMaxIncludeLong = 76;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testDecimalMaxIncludeLong = 100;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
	}

	@Test
	public void testDecimalMaxIncludeLongObject() throws Exception {
		final JpaBaseModelChecker checker = new JpaBaseModelChecker();
		final JpaBaseModel data = new JpaBaseModel();
		data.testDecimalMaxIncludeLongObject = 75L;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testDecimalMaxIncludeLongObject = 74L;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testDecimalMaxIncludeLongObject = 76L;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testDecimalMaxIncludeLongObject = 100L;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
	}

	@Test
	public void testDecimalMaxIncludeFloat() throws Exception {
		final JpaBaseModelChecker checker = new JpaBaseModelChecker();
		final JpaBaseModel data = new JpaBaseModel();
		// can not be tested
		//data.testDecimalMaxIncludeFloat = 75.56f;
		//Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testDecimalMaxIncludeFloat = 75.5599f;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testDecimalMaxIncludeFloat = 75.5601f;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testDecimalMaxIncludeFloat = 100f;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
	}

	@Test
	public void testDecimalMaxIncludeFloatObject() throws Exception {
		final JpaBaseModelChecker checker = new JpaBaseModelChecker();
		final JpaBaseModel data = new JpaBaseModel();
		data.testDecimalMaxIncludeFloatObject = 75.56f;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testDecimalMaxIncludeFloatObject = 75.5599f;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testDecimalMaxIncludeFloatObject = 75.5601f;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testDecimalMaxIncludeFloatObject = 100f;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
	}

	@Test
	public void testDecimalMaxIncludeDouble() throws Exception {
		final JpaBaseModelChecker checker = new JpaBaseModelChecker();
		final JpaBaseModel data = new JpaBaseModel();
		// can not be tested
		//data.testDecimalMaxIncludeDouble = 75.56d;
		//Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testDecimalMaxIncludeDouble = 75.5599d;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testDecimalMaxIncludeDouble = 75.5601d;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testDecimalMaxIncludeDouble = 100d;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
	}

	@Test
	public void testDecimalMaxIncludeDoubleObject() throws Exception {
		final JpaBaseModelChecker checker = new JpaBaseModelChecker();
		final JpaBaseModel data = new JpaBaseModel();
		//data.testDecimalMaxIncludeDoubleObject = 75.56d;
		//Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testDecimalMaxIncludeDoubleObject = 75.5599d;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testDecimalMaxIncludeDoubleObject = 75.5601d;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testDecimalMaxIncludeDoubleObject = 100d;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
	}

	// exclude

	@Test
	public void testDecimalMaxExcludeInteger() throws Exception {
		final JpaBaseModelChecker checker = new JpaBaseModelChecker();
		final JpaBaseModel data = new JpaBaseModel();
		data.testDecimalMaxExcludeInteger = 75;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testDecimalMaxExcludeInteger = 74;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testDecimalMaxExcludeInteger = 76;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testDecimalMaxExcludeInteger = 100;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
	}

	@Test
	public void testDecimalMaxExcludeIntegerObject() throws Exception {
		final JpaBaseModelChecker checker = new JpaBaseModelChecker();
		final JpaBaseModel data = new JpaBaseModel();
		data.testDecimalMaxExcludeIntegerObject = 75;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testDecimalMaxExcludeIntegerObject = 74;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testDecimalMaxExcludeIntegerObject = 76;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testDecimalMaxExcludeIntegerObject = 100;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
	}

	@Test
	public void testDecimalMaxExcludeLong() throws Exception {
		final JpaBaseModelChecker checker = new JpaBaseModelChecker();
		final JpaBaseModel data = new JpaBaseModel();
		data.testDecimalMaxExcludeLong = 75;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testDecimalMaxExcludeLong = 74;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testDecimalMaxExcludeLong = 76;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testDecimalMaxExcludeLong = 100;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
	}

	@Test
	public void testDecimalMaxExcludeLongObject() throws Exception {
		final JpaBaseModelChecker checker = new JpaBaseModelChecker();
		final JpaBaseModel data = new JpaBaseModel();
		data.testDecimalMaxExcludeLongObject = 75L;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testDecimalMaxExcludeLongObject = 74L;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testDecimalMaxExcludeLongObject = 76L;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testDecimalMaxExcludeLongObject = 100L;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
	}

	@Test
	public void testDecimalMaxExcludeFloat() throws Exception {
		final JpaBaseModelChecker checker = new JpaBaseModelChecker();
		final JpaBaseModel data = new JpaBaseModel();
		data.testDecimalMaxExcludeFloat = 75.56f;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testDecimalMaxExcludeFloat = 75.5599f;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testDecimalMaxExcludeFloat = 75.5601f;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testDecimalMaxExcludeFloat = 100f;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
	}

	@Test
	public void testDecimalMaxExcludeFloatObject() throws Exception {
		final JpaBaseModelChecker checker = new JpaBaseModelChecker();
		final JpaBaseModel data = new JpaBaseModel();
		data.testDecimalMaxExcludeFloatObject = 75.56f;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testDecimalMaxExcludeFloatObject = 75.5599f;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testDecimalMaxExcludeFloatObject = 75.5601f;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testDecimalMaxExcludeFloatObject = 100f;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
	}

	@Test
	public void testDecimalMaxExcludeDouble() throws Exception {
		final JpaBaseModelChecker checker = new JpaBaseModelChecker();
		final JpaBaseModel data = new JpaBaseModel();
		data.testDecimalMaxExcludeDouble = 75.56d;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testDecimalMaxExcludeDouble = 75.5599d;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testDecimalMaxExcludeDouble = 75.5601d;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testDecimalMaxExcludeDouble = 100d;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
	}

	@Test
	public void testDecimalMaxExcludeDoubleObject() throws Exception {
		final JpaBaseModelChecker checker = new JpaBaseModelChecker();
		final JpaBaseModel data = new JpaBaseModel();
		data.testDecimalMaxExcludeDoubleObject = 75.56d;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testDecimalMaxExcludeDoubleObject = 75.5599d;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testDecimalMaxExcludeDoubleObject = 75.5601d;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testDecimalMaxExcludeDoubleObject = 100d;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
	}

}
