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
	void otherOriginGetsNoCorsHeader() throws Exception {
		ConfigBaseVariable.setCorsAllowedOrigins("https://app.example.com");
		Assertions.assertTrue(filter("https://evil.example.com").isEmpty());
		Assertions.assertTrue(filter("https://app.example.com.evil.com").isEmpty());
		Assertions.assertTrue(filter(null).isEmpty());
	}
}
