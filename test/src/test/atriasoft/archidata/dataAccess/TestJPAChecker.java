package test.atriasoft.archidata.dataAccess;

import java.util.List;

import org.atriasoft.archidata.dataAccess.DBAccess;
import org.atriasoft.archidata.dataAccess.QueryOptions;
import org.atriasoft.archidata.exception.DataAccessException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import test.atriasoft.archidata.StepwiseExtension;
import test.atriasoft.archidata.dataAccess.model.DataInJson;
import test.atriasoft.archidata.dataAccess.model.DataInJson.DataInJsonChecker;

@ExtendWith(StepwiseExtension.class)
public class TestJPAChecker {
	final static private Logger LOGGER = LoggerFactory.getLogger(TestJPAChecker.class);

	class DataInJsonCheckerTest extends DataInJsonChecker {

		public void testWithCorrectFieldName() throws Exception {
			initialize();
			add("data",
					(
							final DBAccess ioDb,
							final String baseName,
							final DataInJson data,
							final List<String> modifiedValue,
							final QueryOptions options) -> {
						//  nothing to do...
					});
		}

		public void testWithWrongFieldName() throws Exception {
			initialize();
			add("dqsdfqsdfqsdfqsdfata",
					(
							final DBAccess ioDb,
							final String baseName,
							final DataInJson data,
							final List<String> modifiedValue,
							final QueryOptions options) -> {
						//  nothing to do...
					});
		}
	}

	@Test
	public void testThrowWhenFieldDoesNotExist() throws Exception {
		final DataInJsonCheckerTest checker = new DataInJsonCheckerTest();
		final DataAccessException res = Assertions.assertThrows(DataAccessException.class, () -> {
			checker.testWithWrongFieldName();
		});
		Assertions.assertEquals(res.getMessage(),
				"Try to add a JPA Filter on an inexistant Field: 'dqsdfqsdfqsdfqsdfata' not in [data]");
	}

	@Test
	public void testThrowWhenFieldThatExist() throws Exception {
		final DataInJsonCheckerTest checker = new DataInJsonCheckerTest();
		Assertions.assertDoesNotThrow(() -> {
			checker.testWithCorrectFieldName();
		});
	}

}
