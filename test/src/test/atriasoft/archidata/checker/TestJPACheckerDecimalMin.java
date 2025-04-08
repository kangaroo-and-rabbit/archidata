package test.atriasoft.archidata.checker;

import org.atriasoft.archidata.exception.InputException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import test.atriasoft.archidata.checker.model.JpaBaseModel;
import test.atriasoft.archidata.checker.model.JpaBaseModel.JpaBaseModelChecker;

public class TestJPACheckerDecimalMin {

	@Test
	public void testDecimalMinIncludeInteger() throws Exception {
		final JpaBaseModelChecker checker = new JpaBaseModelChecker();
		final JpaBaseModel data = new JpaBaseModel();
		data.testDecimalMinIncludeInteger = -75;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testDecimalMinIncludeInteger = -74;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testDecimalMinIncludeInteger = -76;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testDecimalMinIncludeInteger = -100;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
	}

	@Test
	public void testDecimalMinIncludeIntegerObject() throws Exception {
		final JpaBaseModelChecker checker = new JpaBaseModelChecker();
		final JpaBaseModel data = new JpaBaseModel();
		data.testDecimalMinIncludeIntegerObject = -75;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testDecimalMinIncludeIntegerObject = -74;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testDecimalMinIncludeIntegerObject = -76;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testDecimalMinIncludeIntegerObject = -100;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
	}

	@Test
	public void testDecimalMinIncludeLong() throws Exception {
		final JpaBaseModelChecker checker = new JpaBaseModelChecker();
		final JpaBaseModel data = new JpaBaseModel();
		data.testDecimalMinIncludeLong = -75;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testDecimalMinIncludeLong = -74;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testDecimalMinIncludeLong = -76;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testDecimalMinIncludeLong = -100;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
	}

	@Test
	public void testDecimalMinIncludeLongObject() throws Exception {
		final JpaBaseModelChecker checker = new JpaBaseModelChecker();
		final JpaBaseModel data = new JpaBaseModel();
		data.testDecimalMinIncludeLongObject = -75L;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testDecimalMinIncludeLongObject = -74L;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testDecimalMinIncludeLongObject = -76L;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testDecimalMinIncludeLongObject = -100L;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
	}

	@Test
	public void testDecimalMinIncludeFloat() throws Exception {
		final JpaBaseModelChecker checker = new JpaBaseModelChecker();
		final JpaBaseModel data = new JpaBaseModel();
		data.testDecimalMinIncludeFloat = -75.56f;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testDecimalMinIncludeFloat = -75.5599f;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testDecimalMinIncludeFloat = -75.5601f;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testDecimalMinIncludeFloat = -100f;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
	}

	@Test
	public void testDecimalMinIncludeFloatObject() throws Exception {
		final JpaBaseModelChecker checker = new JpaBaseModelChecker();
		final JpaBaseModel data = new JpaBaseModel();
		data.testDecimalMinIncludeFloatObject = -75.56f;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testDecimalMinIncludeFloatObject = -75.5599f;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testDecimalMinIncludeFloatObject = -75.5601f;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testDecimalMinIncludeFloatObject = -100f;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
	}

	@Test
	public void testDecimalMinIncludeDouble() throws Exception {
		final JpaBaseModelChecker checker = new JpaBaseModelChecker();
		final JpaBaseModel data = new JpaBaseModel();
		// can not be tested
		//data.testDecimalMinIncludeDouble = -75.56d;
		//Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testDecimalMinIncludeDouble = -75.5599d;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testDecimalMinIncludeDouble = -75.5601d;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testDecimalMinIncludeDouble = -100d;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
	}

	@Test
	public void testDecimalMinIncludeDoubleObject() throws Exception {
		final JpaBaseModelChecker checker = new JpaBaseModelChecker();
		final JpaBaseModel data = new JpaBaseModel();
		// can not be tested
		//data.testDecimalMinIncludeDoubleObject = -75.56d;
		//Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testDecimalMinIncludeDoubleObject = -75.5599d;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testDecimalMinIncludeDoubleObject = -75.5601d;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testDecimalMinIncludeDoubleObject = -100d;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
	}

	// exclude

	@Test
	public void testDecimalMinExcludeInteger() throws Exception {
		final JpaBaseModelChecker checker = new JpaBaseModelChecker();
		final JpaBaseModel data = new JpaBaseModel();
		data.testDecimalMinExcludeInteger = -75;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testDecimalMinExcludeInteger = -74;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testDecimalMinExcludeInteger = -76;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testDecimalMinExcludeInteger = -100;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
	}

	@Test
	public void testDecimalMinExcludeIntegerObject() throws Exception {
		final JpaBaseModelChecker checker = new JpaBaseModelChecker();
		final JpaBaseModel data = new JpaBaseModel();
		data.testDecimalMinExcludeIntegerObject = -75;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testDecimalMinExcludeIntegerObject = -74;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testDecimalMinExcludeIntegerObject = -76;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testDecimalMinExcludeIntegerObject = -100;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
	}

	@Test
	public void testDecimalMinExcludeLong() throws Exception {
		final JpaBaseModelChecker checker = new JpaBaseModelChecker();
		final JpaBaseModel data = new JpaBaseModel();
		data.testDecimalMinExcludeLong = -75;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testDecimalMinExcludeLong = -74;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testDecimalMinExcludeLong = -76;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testDecimalMinExcludeLong = -100;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
	}

	@Test
	public void testDecimalMinExcludeLongObject() throws Exception {
		final JpaBaseModelChecker checker = new JpaBaseModelChecker();
		final JpaBaseModel data = new JpaBaseModel();
		data.testDecimalMinExcludeLongObject = -75L;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testDecimalMinExcludeLongObject = -74L;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testDecimalMinExcludeLongObject = -76L;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testDecimalMinExcludeLongObject = -100L;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
	}

	@Test
	public void testDecimalMinExcludeFloat() throws Exception {
		final JpaBaseModelChecker checker = new JpaBaseModelChecker();
		final JpaBaseModel data = new JpaBaseModel();
		data.testDecimalMinExcludeFloat = -75.56f;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testDecimalMinExcludeFloat = -75.5599f;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testDecimalMinExcludeFloat = -75.5601f;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testDecimalMinExcludeFloat = -100f;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
	}

	@Test
	public void testDecimalMinExcludeFloatObject() throws Exception {
		final JpaBaseModelChecker checker = new JpaBaseModelChecker();
		final JpaBaseModel data = new JpaBaseModel();
		data.testDecimalMinExcludeFloatObject = -75.56f;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testDecimalMinExcludeFloatObject = -75.5599f;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testDecimalMinExcludeFloatObject = -75.5601f;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testDecimalMinExcludeFloatObject = -100f;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
	}

	@Test
	public void testDecimalMinExcludeDouble() throws Exception {
		final JpaBaseModelChecker checker = new JpaBaseModelChecker();
		final JpaBaseModel data = new JpaBaseModel();
		data.testDecimalMinExcludeDouble = -75.56d;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testDecimalMinExcludeDouble = -75.5599d;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testDecimalMinExcludeDouble = -75.5601d;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testDecimalMinExcludeDouble = -100d;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
	}

	@Test
	public void testDecimalMinExcludeDoubleObject() throws Exception {
		final JpaBaseModelChecker checker = new JpaBaseModelChecker();
		final JpaBaseModel data = new JpaBaseModel();
		data.testDecimalMinExcludeDoubleObject = -75.56d;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testDecimalMinExcludeDoubleObject = -75.5599d;
		Assertions.assertDoesNotThrow(() -> checker.check(data));
		data.testDecimalMinExcludeDoubleObject = -75.5601d;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
		data.testDecimalMinExcludeDoubleObject = -100d;
		Assertions.assertThrows(InputException.class, () -> checker.check(data));
	}

}
