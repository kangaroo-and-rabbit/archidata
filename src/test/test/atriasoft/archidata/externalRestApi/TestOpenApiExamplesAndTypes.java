package test.atriasoft.archidata.externalRestApi;

import java.util.List;
import java.util.Map;

import org.atriasoft.archidata.externalRestApi.AnalyzeApi;
import org.atriasoft.archidata.externalRestApi.OpenApiGenerateApi;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonValue;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * The examples and the types of the OpenAPI schemas agree with each other.
 */
public class TestOpenApiExamplesAndTypes {

	public enum Rank {
		FIRST(10), SECOND(20);

		private final int value;

		Rank(final int value) {
			this.value = value;
		}

		@JsonValue
		public int getValue() {
			return this.value;
		}
	}

	/** A {@code getValue()} without {@code @JsonValue}: Jackson sends the name. */
	public enum Legacy {
		OLD(1), NEWER(2);

		private final int value;

		Legacy(final int value) {
			this.value = value;
		}

		public int getValue() {
			return this.value;
		}
	}

	public enum Plain {
		ALPHA, BETA
	}

	public static class Sample {
		public ObjectId ownerId;
		public Long linkCount;
		public Integer tokenCount;
		public String security;
		public String shape;
		public String homeUrl;
		public String sha256;
		public Boolean deleted;
		public Double duration;
		public Character initial;
		public char letter;
		public Rank rank;
		public Plain plain;
		public Legacy legacy;
		public String callbackurl;
		public String permalink;
		public String webhookUrl;
		public String airmail;
	}

	@Path("samples")
	@Produces({ MediaType.APPLICATION_JSON })
	public static class SampleResource {
		@GET
		public Sample get() {
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	private static Map<String, Object> map(final Object value) {
		return (Map<String, Object>) value;
	}

	private static Map<String, Object> schemas() throws Exception {
		final AnalyzeApi api = new AnalyzeApi();
		// the enums are analyzed first: the example of a field is computed when the field is read
		api.addModel(Rank.class);
		api.addModel(Plain.class);
		api.addModel(Legacy.class);
		api.addAllApi(List.of(SampleResource.class));
		final Map<String, Object> spec = OpenApiGenerateApi.generate(api, "Test", "1.0");
		return map(map(spec.get("components")).get("schemas"));
	}

	private static Map<String, Object> property(final String name) throws Exception {
		return map(map(map(schemas().get("Sample")).get("properties")).get(name));
	}

	@Test
	public void testObjectIdExampleMatchesItsPattern() throws Exception {
		final Object example = property("ownerId").get("example");
		Assertions.assertEquals("507f1f77bcf86cd799439011", example);
		Assertions.assertTrue(((String) example).matches((String) property("ownerId").get("pattern")));
	}

	@Test
	public void testNumbersAndBooleansAreNotStrings() throws Exception {
		Assertions.assertEquals(0L, property("linkCount").get("example"));
		Assertions.assertEquals(0L, property("tokenCount").get("example"));
		Assertions.assertEquals(Boolean.FALSE, property("deleted").get("example"));
		Assertions.assertEquals(120.5, property("duration").get("example"));
	}

	@Test
	public void testNameHeuristicsOnlyGiveTextToWordsOfAString() throws Exception {
		// a number whose name holds "link" or "token" is not a URL or a token
		Assertions.assertNotEquals("https://example.com/resource", property("linkCount").get("example"));
		// "security" holds "uri", "shape" holds "sha": neither is a word of the name
		Assertions.assertEquals("string", property("security").get("example"));
		Assertions.assertEquals("string", property("shape").get("example"));
		// the words are still recognized
		Assertions.assertEquals("https://example.com/resource", property("homeUrl").get("example"));
		// a compound name keeps its URL example
		for (final String name : List.of("callbackurl", "permalink", "webhookUrl")) {
			Assertions.assertEquals("https://example.com/resource", property(name).get("example"), name);
		}
		Assertions.assertEquals("user@example.com", property("airmail").get("example"));
		Assertions.assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
				property("sha256").get("example"));
	}

	@Test
	public void testEnumWithGetValueIsAnIntegerWhoseExampleIsListed() throws Exception {
		final Map<String, Object> rank = map(schemas().get("Rank"));
		Assertions.assertEquals("integer", rank.get("type"));
		Assertions.assertEquals(List.of(10, 20), rank.get("enum"));
		final Object example = property("rank").get("example");
		Assertions.assertEquals(10L, example);
		Assertions.assertEquals("string", map(schemas().get("Plain")).get("type"));
		Assertions.assertEquals("ALPHA", property("plain").get("example"));
	}

	@Test
	public void testEnumGetValueWithoutJsonValueIsSentByName() throws Exception {
		// Jackson writes the name of the constant: the schema lists the names
		final Map<String, Object> legacy = map(schemas().get("Legacy"));
		Assertions.assertEquals("string", legacy.get("type"));
		Assertions.assertEquals(List.of("NEWER", "OLD"), legacy.get("enum"));
		Assertions.assertTrue(List.of("NEWER", "OLD").contains(property("legacy").get("example")));
	}

	@Test
	public void testCharacterIsAOneCharacterString() throws Exception {
		for (final String name : List.of("initial", "letter")) {
			final Map<String, Object> property = property(name);
			Assertions.assertEquals("string", property.get("type"), name);
			Assertions.assertEquals(1, property.get("minLength"), name);
			Assertions.assertEquals(1, property.get("maxLength"), name);
		}
	}
}
