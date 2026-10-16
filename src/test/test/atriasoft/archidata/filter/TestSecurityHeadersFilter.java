package test.atriasoft.archidata.filter;

import java.util.Map;

import org.atriasoft.archidata.filter.SecurityHeadersFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.MultivaluedMap;

/** Default security headers, HSTS over https only (W-19). */
class TestSecurityHeadersFilter {

	private static MultivaluedMap<String, Object> filter(final String uri, final MultivaluedMap<String, Object> headers)
			throws Exception {
		new SecurityHeadersFilter().filter(FilterStubs.request(uri, Map.of()), FilterStubs.response(headers));
		return headers;
	}

	@Test
	void defaultHeadersOverHttp() throws Exception {
		final MultivaluedMap<String, Object> headers = filter("http://localhost/api/x", FilterStubs.headers());
		Assertions.assertEquals("nosniff", headers.getFirst("X-Content-Type-Options"));
		Assertions.assertEquals("no-referrer", headers.getFirst("Referrer-Policy"));
		Assertions.assertEquals("DENY", headers.getFirst("X-Frame-Options"));
		Assertions.assertNull(headers.getFirst("Strict-Transport-Security"));
	}

	@Test
	void hstsOverHttps() throws Exception {
		final MultivaluedMap<String, Object> headers = filter("https://api.example.com/x", FilterStubs.headers());
		Assertions.assertEquals(SecurityHeadersFilter.HSTS_VALUE, headers.getFirst("Strict-Transport-Security"));
	}

	@Test
	void headerOfTheResourceIsKept() throws Exception {
		final MultivaluedMap<String, Object> headers = FilterStubs.headers();
		headers.putSingle("X-Frame-Options", "SAMEORIGIN");
		Assertions.assertEquals("SAMEORIGIN", filter("http://localhost/x", headers).getFirst("X-Frame-Options"));
	}
}
