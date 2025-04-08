package test.atriasoft.archidata.externalRestApi;

import java.util.List;

import org.atriasoft.archidata.externalRestApi.AnalyzeApi;
import org.atriasoft.archidata.externalRestApi.model.ApiModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.GET;

public class TestAnalyzeApiName {
	final static private Logger LOGGER = LoggerFactory.getLogger(TestAnalyzeApiName.class);

	public class ApiName {
		@GET
		public void firstName() {

		}

		@GET
		public void SecondName() {

		}
	}

	@Test
	public void testNames() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.addAllApi(List.of(ApiName.class));

		Assertions.assertEquals(1, api.getAllApi().size());
		Assertions.assertEquals("ApiName", api.getAllApi().get(0).name);
		Assertions.assertEquals(2, api.getAllApi().get(0).interfaces.size());
		{
			final ApiModel model = api.getAllApi().get(0).getInterfaceNamed("firstName");
			Assertions.assertNotNull(model);
		}
		{
			final ApiModel model = api.getAllApi().get(0).getInterfaceNamed("SecondName");
			Assertions.assertNotNull(model);
		}
	}

}
