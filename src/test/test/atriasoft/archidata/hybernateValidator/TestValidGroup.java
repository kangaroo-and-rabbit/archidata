package test.atriasoft.archidata.hybernateValidator;

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
import test.atriasoft.archidata.hybernateValidator.model.ValidatorModelGroup;

@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestValidGroup {
	private final static Logger LOGGER = LoggerFactory.getLogger(TestValidGroup.class);
	public final static String ENDPOINT_NAME = "TestResourceValidGroup";

	static WebLauncherTest webInterface = null;
	static RESTApi api = null;

	@BeforeAll
	public static void configureWebServer() throws Exception {
		ConfigureDb.configure();
		webInterface = new WebLauncherTest();
		webInterface.process();
		api = new RESTApi(ConfigBaseVariable.apiAdress);
		api.setToken(Common.ADMIN_TOKEN);
	}

	@AfterAll
	public static void stopWebServer() throws Exception {
		webInterface.stop();
		webInterface = null;
		ConfigureDb.clear();
	}

	/*
		public class ValidatorModelGroup {
			@Size(max = 5)
			public String valueNoGroup;
			@Size(max = 5, groups = GroupUpdate.class)
			public String valueUpdate;
			@Size(max = 5, groups = GroupCreate.class)
			public String valueCreate;
			@Size(max = 5, groups = { GroupCreate.class, GroupUpdate.class })
			public String valueUpdateCreate;
		}
	 */
	@Order(2)
	@Test
	public void DetectGenericValidNoError() throws Exception {
		final ValidatorModelGroup data = new ValidatorModelGroup();
		data.valueNoGroup = "plop1";
		data.valueUpdate = "plop2";
		data.valueCreate = "plop3";
		data.valueUpdateCreate = "plop4";
		Assertions.assertDoesNotThrow(
				() -> api.request(TestValidGroup.ENDPOINT_NAME, "valid").post().bodyJson(data).fetch());
	}

	@Order(3)
	@Test
	public void DetectGenericValidError() throws Exception {
		final ValidatorModelGroup data = new ValidatorModelGroup();
		data.valueNoGroup = "plop1__";
		data.valueUpdate = "plop2__";
		data.valueCreate = "plop3__";
		data.valueUpdateCreate = "plop4__";
		final RESTErrorResponseException exception = Assertions.assertThrows(RESTErrorResponseException.class,
				() -> api.request(TestValidGroup.ENDPOINT_NAME, "valid").post().bodyJson(data).fetch());
		Assertions.assertNotNull(exception);
		LOGGER.debug("error on input:{}", exception);
		Assertions.assertNull(exception.getMessage());
		Assertions.assertNotNull(exception.inputError);
		Assertions.assertEquals(1, exception.inputError.size());
		Assertions.assertEquals("arg0", exception.inputError.get(0).argument);
		Assertions.assertEquals("valueNoGroup", exception.inputError.get(0).path);
		Assertions.assertEquals("size must be between 0 and 5", exception.inputError.get(0).message);
	}

	@Order(10)
	@Test
	public void DetectGenericValidGroupCreate() throws Exception {
		final ValidatorModelGroup data = new ValidatorModelGroup();
		data.valueNoGroup = "plop1";
		data.valueUpdate = "plop2";
		data.valueCreate = "plop3";
		data.valueUpdateCreate = "plop4";
		Assertions.assertDoesNotThrow(
				() -> api.request(TestValidGroup.ENDPOINT_NAME, "create").post().bodyJson(data).fetch());
	}

	@Order(11)
	@Test
	public void DetectGenericValidGroupCreateError() throws Exception {
		final ValidatorModelGroup data = new ValidatorModelGroup();
		data.valueNoGroup = "plop1__";
		data.valueUpdate = "plop2__";
		data.valueCreate = "plop3__";
		data.valueUpdateCreate = "plop4__";
		final RESTErrorResponseException exception = Assertions.assertThrows(RESTErrorResponseException.class,
				() -> api.request(TestValidGroup.ENDPOINT_NAME, "create").post().bodyJson(data).fetch());
		Assertions.assertNotNull(exception);
		LOGGER.debug("error on input:{}", exception);
		Assertions.assertNull(exception.getMessage());
		Assertions.assertNotNull(exception.inputError);
		Assertions.assertEquals(2, exception.inputError.size());
		Assertions.assertEquals(null, exception.inputError.get(0).argument);
		Assertions.assertEquals("valueCreate", exception.inputError.get(0).path);
		Assertions.assertEquals("size must be between 0 and 5", exception.inputError.get(0).message);
		Assertions.assertEquals(null, exception.inputError.get(1).argument);
		Assertions.assertEquals("valueUpdateCreate", exception.inputError.get(1).path);
		Assertions.assertEquals("size must be between 0 and 5", exception.inputError.get(1).message);
	}

	@Order(20)
	@Test
	public void DetectGenericValidGroupUpdate() throws Exception {
		final ValidatorModelGroup data = new ValidatorModelGroup();
		data.valueNoGroup = "plop1";
		data.valueUpdate = "plop2";
		data.valueCreate = "plop3";
		data.valueUpdateCreate = "plop4";
		Assertions.assertDoesNotThrow(
				() -> api.request(TestValidGroup.ENDPOINT_NAME, "update").post().bodyJson(data).fetch());
	}

	@Order(21)
	@Test
	public void DetectGenericValidGroupUpdateError() throws Exception {
		final ValidatorModelGroup data = new ValidatorModelGroup();
		data.valueNoGroup = "plop1__";
		data.valueUpdate = "plop2__";
		data.valueCreate = "plop3__";
		data.valueUpdateCreate = "plop4__";
		final RESTErrorResponseException exception = Assertions.assertThrows(RESTErrorResponseException.class,
				() -> api.request(TestValidGroup.ENDPOINT_NAME, "update").post().bodyJson(data).fetch());
		Assertions.assertNotNull(exception);
		LOGGER.debug("error on input:{}", exception);
		Assertions.assertNull(exception.getMessage());
		Assertions.assertNotNull(exception.inputError);
		Assertions.assertEquals(2, exception.inputError.size());
		Assertions.assertEquals(null, exception.inputError.get(0).argument);
		Assertions.assertEquals("valueUpdate", exception.inputError.get(0).path);
		Assertions.assertEquals("size must be between 0 and 5", exception.inputError.get(0).message);
		Assertions.assertEquals(null, exception.inputError.get(1).argument);
		Assertions.assertEquals("valueUpdateCreate", exception.inputError.get(1).path);
		Assertions.assertEquals("size must be between 0 and 5", exception.inputError.get(1).message);
	}

	@Order(30)
	@Test
	public void DetectGenericValidGroupUpdateCreate() throws Exception {
		final ValidatorModelGroup data = new ValidatorModelGroup();
		data.valueNoGroup = "plop1";
		data.valueUpdate = "plop2";
		data.valueCreate = "plop3";
		data.valueUpdateCreate = "plop4";
		Assertions.assertDoesNotThrow(
				() -> api.request(TestValidGroup.ENDPOINT_NAME, "update-create").post().bodyJson(data).fetch());
	}

	@Order(31)
	@Test
	public void DetectGenericValidGroupUpdateCreateError() throws Exception {
		final ValidatorModelGroup data = new ValidatorModelGroup();
		data.valueNoGroup = "plop1__";
		data.valueUpdate = "plop2__";
		data.valueCreate = "plop3__";
		data.valueUpdateCreate = "plop4__";
		final RESTErrorResponseException exception = Assertions.assertThrows(RESTErrorResponseException.class,
				() -> api.request(TestValidGroup.ENDPOINT_NAME, "update-create").post().bodyJson(data).fetch());
		Assertions.assertNotNull(exception);
		LOGGER.debug("error on input:{}", exception);
		Assertions.assertNull(exception.getMessage());
		Assertions.assertNotNull(exception.inputError);
		Assertions.assertEquals(3, exception.inputError.size());
		Assertions.assertEquals(null, exception.inputError.get(0).argument);
		Assertions.assertEquals("valueCreate", exception.inputError.get(0).path);
		Assertions.assertEquals("size must be between 0 and 5", exception.inputError.get(0).message);
		Assertions.assertEquals(null, exception.inputError.get(1).argument);
		Assertions.assertEquals("valueUpdate", exception.inputError.get(1).path);
		Assertions.assertEquals("size must be between 0 and 5", exception.inputError.get(1).message);
		Assertions.assertEquals(null, exception.inputError.get(2).argument);
		Assertions.assertEquals("valueUpdateCreate", exception.inputError.get(2).path);
		Assertions.assertEquals("size must be between 0 and 5", exception.inputError.get(2).message);
	}

	@Order(40)
	@Test
	public void DetectGenericValidGroupFull() throws Exception {
		final ValidatorModelGroup data = new ValidatorModelGroup();
		data.valueNoGroup = "plop1";
		data.valueUpdate = "plop2";
		data.valueCreate = "plop3";
		data.valueUpdateCreate = "plop4";
		Assertions.assertDoesNotThrow(
				() -> api.request(TestValidGroup.ENDPOINT_NAME, "full").post().bodyJson(data).fetch());
	}

	@Order(41)
	@Test
	public void DetectGenericValidGroupFullError() throws Exception {
		final ValidatorModelGroup data = new ValidatorModelGroup();
		data.valueNoGroup = "plop1__";
		data.valueUpdate = "plop2__";
		data.valueCreate = "plop3__";
		data.valueUpdateCreate = "plop4__";
		RESTErrorResponseException exception = Assertions.assertThrows(RESTErrorResponseException.class,
				() -> api.request(TestValidGroup.ENDPOINT_NAME, "full").post().bodyJson(data).fetch());
		Assertions.assertNotNull(exception);
		LOGGER.debug("error on input:{}", exception);
		Assertions.assertNull(exception.getMessage());
		Assertions.assertNotNull(exception.inputError);
		Assertions.assertEquals(3, exception.inputError.size());
		Assertions.assertEquals(null, exception.inputError.get(0).argument);
		Assertions.assertEquals("valueCreate", exception.inputError.get(0).path);
		Assertions.assertEquals("size must be between 0 and 5", exception.inputError.get(0).message);
		Assertions.assertEquals(null, exception.inputError.get(1).argument);
		Assertions.assertEquals("valueUpdate", exception.inputError.get(1).path);
		Assertions.assertEquals("size must be between 0 and 5", exception.inputError.get(1).message);
		Assertions.assertEquals(null, exception.inputError.get(2).argument);
		Assertions.assertEquals("valueUpdateCreate", exception.inputError.get(2).path);
		Assertions.assertEquals("size must be between 0 and 5", exception.inputError.get(2).message);
		data.valueUpdate = "plop2";
		data.valueCreate = "plop3";
		data.valueUpdateCreate = "plop4";
		exception = Assertions.assertThrows(RESTErrorResponseException.class,
				() -> api.request(TestValidGroup.ENDPOINT_NAME, "full").post().bodyJson(data).fetch());
		Assertions.assertNotNull(exception);
		LOGGER.debug("error on input:{}", exception);
		Assertions.assertNull(exception.getMessage());
		Assertions.assertNotNull(exception.inputError);
		Assertions.assertEquals(1, exception.inputError.size());
		Assertions.assertEquals("arg0", exception.inputError.get(0).argument);
		Assertions.assertEquals("valueNoGroup", exception.inputError.get(0).path);
		Assertions.assertEquals("size must be between 0 and 5", exception.inputError.get(0).message);
	}
}
