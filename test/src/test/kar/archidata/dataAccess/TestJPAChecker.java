package test.kar.archidata.dataAccess;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kar.archidata.dataAccess.DBAccess;
import org.kar.archidata.dataAccess.QueryOptions;
import org.kar.archidata.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import test.kar.archidata.StepwiseExtension;
import test.kar.archidata.dataAccess.model.DataInJson;
import test.kar.archidata.dataAccess.model.DataInJson.DataInJsonChecker;

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
