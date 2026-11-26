package test.atriasoft.archidata.externalRestApi;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.atriasoft.archidata.dataAccess.model.Pagination;
import org.atriasoft.archidata.externalRestApi.AnalyzeApi;
import org.atriasoft.archidata.externalRestApi.DrawioGenerateApi;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * The Draw.io diagram has no box for a value type (date, time, UUID) and follows a {@code Pagination<T>}.
 */
public class TestDrawioValueTypesAndPagination {

	public static class Appointment {
		public UUID reference;
		public LocalDate day;
		public LocalTime hour;
		public Instant createdOn;
		public String title;
	}

	@Path("/appointments")
	@Produces({ MediaType.APPLICATION_JSON })
	public static class AppointmentResource {
		@GET
		public Pagination<Appointment> page() {
			return null;
		}
	}

	private static String generate() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		api.addAllApi(List.of(AppointmentResource.class));
		return DrawioGenerateApi.generateApiString(api);
	}

	/** The boxes of the diagram: the value of every cell that has a header (a parent {@code 1}). */
	private static List<String> boxTitles(final String xml) {
		final Matcher matcher = Pattern
				.compile("<mxCell id=\"\\d+\" value=\"([^\"]*)\" style=\"[^\"]*\" vertex=\"1\" parent=\"1\">")
				.matcher(xml);
		final List<String> titles = new java.util.ArrayList<>();
		while (matcher.find()) {
			titles.add(matcher.group(1));
		}
		return titles;
	}

	@Test
	public void testValueTypesHaveNoBox() throws Exception {
		final String xml = generate();
		final List<String> titles = boxTitles(xml);
		Assertions.assertTrue(titles.contains("Appointment"), titles.toString());
		for (final String valueType : List.of("LocalDate", "LocalTime", "UUID", "Instant")) {
			Assertions.assertFalse(titles.contains(valueType), valueType + " has a box: " + titles);
		}
		// the field keeps its type name
		Assertions.assertTrue(xml.contains("+ day: LocalDate"), xml);
		Assertions.assertTrue(xml.contains("+ reference: UUID"), xml);
	}

	@Test
	public void testPaginationEndpointShowsItsItemsAndLinksToThem() throws Exception {
		final String xml = generate();
		Assertions.assertTrue(Pattern.compile("/appointments \u2192 Pagination[^\"]*Appointment").matcher(xml).find(),
				xml);
		Assertions.assertFalse(xml.contains("\u2192 Object"), xml);
		// one edge from the REST box to the Appointment box
		final Matcher matcher = Pattern.compile(
				"<mxCell id=\"\\d+\" value=\"([^\"]*)\" style=\"[^\"]*\" edge=\"1\" parent=\"1\" source=\"(\\d+)\" target=\"(\\d+)\">")
				.matcher(xml);
		int edges = 0;
		while (matcher.find()) {
			edges++;
		}
		Assertions.assertEquals(1, edges, xml);
	}
}
