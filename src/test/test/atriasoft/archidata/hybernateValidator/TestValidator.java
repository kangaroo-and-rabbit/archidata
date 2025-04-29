package test.atriasoft.archidata.hybernateValidator;

import java.util.ArrayList;

import org.atriasoft.archidata.exception.RESTErrorResponseException;
import org.atriasoft.archidata.tools.ConfigBaseVariable;
import org.atriasoft.archidata.tools.RESTApi;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import test.atriasoft.archidata.ConfigureDb;
import test.atriasoft.archidata.StepwiseExtension;
import test.atriasoft.archidata.apiExtern.Common;
import test.atriasoft.archidata.hybernateValidator.model.ValidatorModel;
import test.atriasoft.archidata.hybernateValidator.model.ValidatorSubModel;

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
				() -> api.request(TestValidator.ENDPOINT_NAME).post().queryParam("queryParametersName", "2")
						.bodyJson(data).fetch());
		Assertions.assertNotNull(exception);
		LOGGER.debug("error on input:{}", exception);
		Assertions.assertNull(exception.getMessage());
		Assertions.assertNotNull(exception.inputError);
		Assertions.assertEquals(5, exception.inputError.size());
		Assertions.assertEquals("arg0", exception.inputError.get(0).argument);
		Assertions.assertEquals(null, exception.inputError.get(0).path);
		Assertions.assertEquals("must be greater than or equal to 5", exception.inputError.get(0).message);
		Assertions.assertEquals("arg1", exception.inputError.get(1).argument);
		Assertions.assertEquals("data", exception.inputError.get(1).path);
		Assertions.assertEquals("size must be between 0 and 5", exception.inputError.get(1).message);
		Assertions.assertEquals("arg1", exception.inputError.get(2).argument);
		Assertions.assertEquals("multipleElement[1].data", exception.inputError.get(2).path);
		Assertions.assertEquals("size must be between 2 and 2147483647", exception.inputError.get(2).message);
		Assertions.assertEquals("arg1", exception.inputError.get(3).argument);
		Assertions.assertEquals("subElement.data", exception.inputError.get(3).path);
		Assertions.assertEquals("size must be between 2 and 2147483647", exception.inputError.get(3).message);
		Assertions.assertEquals("arg1", exception.inputError.get(4).argument);
		Assertions.assertEquals("value", exception.inputError.get(4).path);
		Assertions.assertEquals("Field can not be set, it is a read-only field.", exception.inputError.get(4).message);
	}
}
