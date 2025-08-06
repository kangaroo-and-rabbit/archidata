package test.atriasoft.archidata.externalRestApi;

import java.util.List;

import org.atriasoft.archidata.externalRestApi.AnalyzeApi;
import org.atriasoft.archidata.externalRestApi.model.ApiModel;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

public class TestAnalyzeApiPath {
	final static private Logger LOGGER = LoggerFactory.getLogger(TestAnalyzeApiPath.class);

	public class NoPath {
		@GET
		public void noPath() {

		}

		@GET
		@Path("/")
		public void pathWithStartSlash() {

		}

		@GET
		@Path("plop")
		public void withPath() {

		}

		@GET
		@Path("/plop")
		public void withPath2() {

		}

		@GET
		@Path("/plop/")
		public void startAndEndWithSlash() {

		}

		@GET
		@Path("plop/{variable}")
		public void withVariable() {

		}

		@GET
		@Path("plop/{variable: [a-zA-Z]{5-88}")
		public void withVariableRegEx() {

		}
	}

	@Test
	public void testNoPath() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.addAllApi(List.of(NoPath.class));

		Assertions.assertEquals(1, api.getAllApi().size());
		Assertions.assertEquals("", api.getAllApi().get(0).restEndPoint);
		Assertions.assertEquals(7, api.getAllApi().get(0).interfaces.size());
		{
			final ApiModel model = api.getAllApi().get(0).getInterfaceNamed("noPath");
			Assertions.assertNotNull(model);
			Assertions.assertEquals("/", model.restEndPoint);
		}
		{
			final ApiModel model = api.getAllApi().get(0).getInterfaceNamed("pathWithStartSlash");
			Assertions.assertNotNull(model);
			Assertions.assertEquals("/", model.restEndPoint);
		}
		{
			final ApiModel model = api.getAllApi().get(0).getInterfaceNamed("withPath");
			Assertions.assertNotNull(model);
			Assertions.assertEquals("/plop", model.restEndPoint);
		}
		{
			final ApiModel model = api.getAllApi().get(0).getInterfaceNamed("withPath2");
			Assertions.assertNotNull(model);
			Assertions.assertEquals("//plop", model.restEndPoint);
		}
		{
			final ApiModel model = api.getAllApi().get(0).getInterfaceNamed("startAndEndWithSlash");
			Assertions.assertNotNull(model);
			Assertions.assertEquals("//plop/", model.restEndPoint);
		}
		{
			final ApiModel model = api.getAllApi().get(0).getInterfaceNamed("withVariable");
			Assertions.assertNotNull(model);
			Assertions.assertEquals("/plop/{variable}", model.restEndPoint);
		}
		{
			final ApiModel model = api.getAllApi().get(0).getInterfaceNamed("withVariableRegEx");
			Assertions.assertNotNull(model);
			Assertions.assertEquals("/plop/{variable}", model.restEndPoint);
		}
	}

	@Path("/kaboom")
	public class WithPath {
		@GET
		public void noPath() {

		}

		@GET
		@Path("plop")
		public void withPath() {

		}

		@GET
		@Path("/plop")
		public void withPath2() {

		}
	}

	@Test
	public void testWithPath() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.addAllApi(List.of(WithPath.class));

		Assertions.assertEquals(1, api.getAllApi().size());
		Assertions.assertEquals("/kaboom", api.getAllApi().get(0).restEndPoint);
		Assertions.assertEquals(3, api.getAllApi().get(0).interfaces.size());
		{
			final ApiModel model = api.getAllApi().get(0).getInterfaceNamed("noPath");
			Assertions.assertNotNull(model);
			Assertions.assertEquals("/kaboom/", model.restEndPoint);
		}
		{
			final ApiModel model = api.getAllApi().get(0).getInterfaceNamed("withPath");
			Assertions.assertNotNull(model);
			Assertions.assertEquals("/kaboom/plop", model.restEndPoint);
		}
		{
			final ApiModel model = api.getAllApi().get(0).getInterfaceNamed("withPath2");
			Assertions.assertNotNull(model);
			Assertions.assertEquals("/kaboom//plop", model.restEndPoint);
		}
	}

}
