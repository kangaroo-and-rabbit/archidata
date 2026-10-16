package test.atriasoft.archidata.hybernateValidator;

import java.util.Date;
import java.util.List;

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

import test.atriasoft.archidata.ConfigureDb;
import test.atriasoft.archidata.StepwiseExtension;
import test.atriasoft.archidata.apiExtern.Common;
import test.atriasoft.archidata.dataAccess.model.ApiReadOnlyModel;
import test.atriasoft.archidata.dataAccess.model.ApiReadOnlySubModel;

/**
 * W-08: the server request body mapper drops the {@code @ApiReadOnly} properties (mass assignment), while
 * server-side inserts and the client mapper keep them.
 */
@ExtendWith(StepwiseExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class TestApiReadOnlyInput {
	private static final String ENDPOINT_NAME = "TestResourceApiReadOnly";

	static WebLauncherTest webInterface = null;
	static RESTApi api = null;
	private static Long serverInsertedId = null;

	@BeforeAll
	public static void configureWebServer() throws Exception {
		serverInsertedId = null;
		ConfigureDb.configure();
		webInterface = new WebLauncherTest();
		webInterface.process();
		api = new RESTApi(ConfigBaseVariable.getLocalAddress());
		api.setToken(Common.ADMIN_TOKEN);
	}

	@AfterAll
	public static void stopWebServer() throws Exception {
		webInterface.stop();
		webInterface = null;
		ConfigureDb.clear();
	}

	private static ApiReadOnlyModel clientPayload() {
		final ApiReadOnlyModel data = new ApiReadOnlyModel();
		data.setId(424242L);
		data.valueUpdatable = "client-updatable";
		data.valueNotUpdate = "client-readonly";
		data.dateNotUpdate = new Date(2000000000L);
		data.listNotUpdate = List.of("x");
		data.objectNotUpdate = new ApiReadOnlySubModel();
		data.objectNotUpdate.data = "client-sub";
		return data;
	}

	@Order(1)
	@Test
	public void testReadOnlyPropertiesDroppedFromBody() throws Exception {
		final ApiReadOnlyModel echo = api.request(ENDPOINT_NAME, "echo").post().bodyJson(clientPayload())
				.fetch(ApiReadOnlyModel.class);
		Assertions.assertEquals("client-updatable", echo.valueUpdatable);
		Assertions.assertNull(echo.getId());
		Assertions.assertNull(echo.valueNotUpdate);
		Assertions.assertNull(echo.dateNotUpdate);
		Assertions.assertNull(echo.listNotUpdate);
		Assertions.assertNull(echo.objectNotUpdate);
	}

	@Order(2)
	@Test
	public void testPostWithReadOnlyPropertiesIsAcceptedAndIgnored() throws Exception {
		// Even inserted with ForceReadOnlyField, nothing the client sent for a read-only field reaches the DB.
		final ApiReadOnlyModel inserted = api.request(ENDPOINT_NAME, "insert").post().bodyJson(clientPayload())
				.fetch(ApiReadOnlyModel.class);
		Assertions.assertNotNull(inserted.getId());
		Assertions.assertNotEquals(424242L, inserted.getId());
		Assertions.assertEquals("client-updatable", inserted.valueUpdatable);
		final ApiReadOnlyModel stored = ConfigureDb.da.getById(ApiReadOnlyModel.class, inserted.getId());
		Assertions.assertEquals("client-updatable", stored.valueUpdatable);
		Assertions.assertNull(stored.valueNotUpdate);
		Assertions.assertNull(stored.dateNotUpdate);
		Assertions.assertNull(stored.listNotUpdate);
		Assertions.assertNull(stored.objectNotUpdate);
	}

	@Order(3)
	@Test
	public void testServerSideInsertKeepsReadOnlyFields() throws Exception {
		final ApiReadOnlyModel data = clientPayload();
		data.setId(null);
		final ApiReadOnlyModel inserted = ConfigureDb.da.insert(data);
		serverInsertedId = inserted.getId();
		final ApiReadOnlyModel stored = ConfigureDb.da.getById(ApiReadOnlyModel.class, serverInsertedId);
		Assertions.assertEquals("client-readonly", stored.valueNotUpdate);
		Assertions.assertEquals("client-sub", stored.objectNotUpdate.data);
	}

	@Order(4)
	@Test
	public void testClientReadsReadOnlyFieldsOfResponse() throws Exception {
		final ApiReadOnlyModel read = api.request(ENDPOINT_NAME, String.valueOf(serverInsertedId)).get()
				.fetch(ApiReadOnlyModel.class);
		Assertions.assertEquals(serverInsertedId, read.getId());
		Assertions.assertEquals("client-readonly", read.valueNotUpdate);
		Assertions.assertEquals(new Date(2000000000L), read.dateNotUpdate);
		Assertions.assertEquals(List.of("x"), read.listNotUpdate);
		Assertions.assertEquals("client-sub", read.objectNotUpdate.data);
	}
}
