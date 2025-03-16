package test.kar.archidata.hybernateValidator;

import java.util.ArrayList;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kar.archidata.exception.RESTErrorResponseException;
import org.kar.archidata.tools.ConfigBaseVariable;
import org.kar.archidata.tools.RESTApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import test.kar.archidata.ConfigureDb;
import test.kar.archidata.StepwiseExtension;
import test.kar.archidata.apiExtern.Common;
import test.kar.archidata.hybernateValidator.model.ValidatorModel;
import test.kar.archidata.hybernateValidator.model.ValidatorSubModel;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestValidator {
	private final static Logger LOGGER = LoggerFactory.getLogger(TestValidator.class);
	public final static String ENDPOINT_NAME = "TestResourceValidator";

	static WebLauncherTest webInterface = null;
	static RESTApi api = null;

	@BeforeAll
	public static void configureWebServer() throws Exception {
		ConfigureDb.configure();
		LOGGER.info("configure server ...");
		webInterface = new WebLauncherTest();
		LOGGER.info("Clean previous table");

		LOGGER.info("Start REST (BEGIN)");
		webInterface.process();
		LOGGER.info("Start REST (DONE)");
		api = new RESTApi(ConfigBaseVariable.apiAdress);
		api.setToken(Common.ADMIN_TOKEN);
	}

	@AfterAll
	public static void stopWebServer() throws Exception {
		LOGGER.info("Kill the web server");
		webInterface.stop();
		webInterface = null;
		ConfigureDb.clear();

	}

	//	@Order(1)
	//	@Test
	//	public void noError() throws Exception {
	//		final ObjectMapper mapper = ContextGenericTools.createObjectMapper();
	//		final String data = """
	//				{"oid":"67d5686ad03cee1d9bacd7c2","name":"Constraint Violation","message":"post.arg0.data: size must be between 0 and 5","time":"2025-03-15T11:45:46.975079059Z","status":400,"statusMessage":"Bad Request","inputError":[{"path":"post.arg0.data","message":"size must be between 0 and 5"}]}
	//				""";
	//		try {
	//			final RESTErrorResponseException out = mapper.readValue(data, RESTErrorResponseException.class);
	//			LOGGER.error("Fsdfsdfsdf {}", out);
	//		} catch (final Exception ex) {
	//			LOGGER.error("Fail to load data: {}", ex.getMessage());
	//			LOGGER.error("Fail to load data: {}", ex.getMessage());
	//		}
	//		LOGGER.error("Fail to load data: plop...");
	//	}
	//	@Order(1)
	//	@Test
	//	public void noError() throws Exception {
	//		final ValidatorModel data = new ValidatorModel();
	//		data.data = "klj";
	//		Assertions.assertDoesNotThrow(() -> api.post(void.class, TestValidator.ENDPOINT_NAME, data));
	//	}

	@Order(2)
	@Test
	public void DetectGenericError() throws Exception {
		final ValidatorModel data = new ValidatorModel();
		data.value = "plop";
		data.data = "klsdfsdfsdfsdfj";
		data.multipleElement = new ArrayList<>();
		ValidatorSubModel tmp = new ValidatorSubModel();
		tmp.data = "lkmkmlkmlklm";
		data.multipleElement.add(tmp);
		tmp = new ValidatorSubModel();
		tmp.data = "1";
		data.multipleElement.add(tmp);
		data.subElement = new ValidatorSubModel();
		data.subElement.data = "k";
		final RESTErrorResponseException exception = Assertions.assertThrows(RESTErrorResponseException.class,
				() -> api.post(void.class, TestValidator.ENDPOINT_NAME + "?queryParametersName=2", data));
		Assertions.assertNotNull(exception);
		LOGGER.debug("error on input:{}", exception);
		Assertions.assertNull(exception.getMessage());
		Assertions.assertNotNull(exception.inputError);
		Assertions.assertEquals(1, exception.inputError.size());
		Assertions.assertEquals("data", exception.inputError.get(0).path);
		Assertions.assertEquals("size must be between 0 and 5", exception.inputError.get(0).message);
	}
}
