package test.atriasoft.archidata.api;

import org.atriasoft.archidata.api.SwaggerUiExternal;
import org.atriasoft.archidata.api.openApiResource;
import org.atriasoft.archidata.tools.ConfigBaseVariable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.NotFoundException;

/** The OpenAPI specification and Swagger UI answer 404 unless API_DOC_ENABLED=true (W-18). */
class TestApiDocSwitch {

	@AfterEach
	void reset() {
		ConfigBaseVariable.setApiDocEnabled(null);
	}

	@Test
	void disabledByDefault() throws Exception {
		ConfigBaseVariable.setApiDocEnabled(null);
		Assertions.assertEquals(404, new openApiResource().getDescription().getStatus());
		Assertions.assertThrows(NotFoundException.class, () -> new SwaggerUiExternal().getSwaggerUI());
	}

	@Test
	void enabledByConfiguration() throws Exception {
		ConfigBaseVariable.setApiDocEnabled("true");
		// not configured with an analyzed API: 503, but no longer 404
		Assertions.assertNotEquals(404, new openApiResource().getDescription().getStatus());
		final String page = new SwaggerUiExternal().getSwaggerUI();
		Assertions.assertEquals(3, page.split("integrity=\"sha512-", -1).length - 1);
		Assertions.assertEquals(3, page.split("crossorigin=\"anonymous\"", -1).length - 1);
	}
}
