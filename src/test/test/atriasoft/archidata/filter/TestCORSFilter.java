package test.atriasoft.archidata.filter;

import java.util.Map;

import org.atriasoft.archidata.filter.CORSFilter;
import org.atriasoft.archidata.tools.ConfigBaseVariable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.MultivaluedMap;

/** The three cases of {@link CORSFilter}: no list, origin in the list, origin out of the list. */
class TestCORSFilter {

	@AfterEach
	void reset() {
		ConfigBaseVariable.setCorsAllowedOrigins(null);
	}

	private static MultivaluedMap<String, Object> filter(final String origin) throws Exception {
		final MultivaluedMap<String, Object> headers = FilterStubs.headers();
		new CORSFilter().filter(
				FilterStubs.request("http://localhost/api/x", origin == null ? Map.of() : Map.of("Origin", origin)),
				FilterStubs.response(headers));
		return headers;
	}

	@Test
	void withoutListAnyOriginWithoutCredentials() throws Exception {
		ConfigBaseVariable.setCorsAllowedOrigins(null);
		final MultivaluedMap<String, Object> headers = filter("https://evil.example.com");
		Assertions.assertEquals("*", headers.getFirst("Access-Control-Allow-Origin"));
		Assertions.assertNull(headers.getFirst("Access-Control-Allow-Credentials"));
		Assertions.assertNotNull(headers.getFirst("Access-Control-Allow-Methods"));
	}

	@Test
	void allowedOriginIsReflectedWithCredentials() throws Exception {
		ConfigBaseVariable.setCorsAllowedOrigins("https://app.example.com, http://localhost:4200");
		final MultivaluedMap<String, Object> headers = filter("http://localhost:4200");
		Assertions.assertEquals("http://localhost:4200", headers.getFirst("Access-Control-Allow-Origin"));
		Assertions.assertEquals("true", headers.getFirst("Access-Control-Allow-Credentials"));
		Assertions.assertEquals("Origin", headers.getFirst("Vary"));
		Assertions.assertNotNull(headers.getFirst("Access-Control-Allow-Headers"));
	}

	@Test
	void otherOriginGetsNoCorsHeaderButVary() throws Exception {
		ConfigBaseVariable.setCorsAllowedOrigins("https://app.example.com");
		for (final String origin : new String[] { "https://evil.example.com", "https://app.example.com.evil.com",
				null }) {
			final MultivaluedMap<String, Object> headers = filter(origin);
			Assertions.assertEquals(java.util.Set.of("Vary"), headers.keySet(), "origin " + origin);
			Assertions.assertEquals(java.util.List.of("Origin"), headers.get("Vary"), "origin " + origin);
		}
	}

	@Test
	void varyOriginIsNotDuplicated() throws Exception {
		ConfigBaseVariable.setCorsAllowedOrigins("https://app.example.com");
		final MultivaluedMap<String, Object> headers = FilterStubs.headers();
		headers.add("Vary", "Accept-Encoding, origin");
		new CORSFilter().filter(
				FilterStubs.request("http://localhost/api/x", Map.of("Origin", "https://app.example.com")),
				FilterStubs.response(headers));
		Assertions.assertEquals(java.util.List.of("Accept-Encoding, origin"), headers.get("Vary"));
		Assertions.assertEquals("https://app.example.com", headers.getFirst("Access-Control-Allow-Origin"));
	}

	@Test
	void withoutListNoVary() throws Exception {
		ConfigBaseVariable.setCorsAllowedOrigins(null);
		Assertions.assertNull(filter("https://app.example.com").get("Vary"));
	}
}
