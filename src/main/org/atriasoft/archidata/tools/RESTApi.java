package org.atriasoft.archidata.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * High-level REST API client that acts as a factory for {@link RESTApiRequest} instances.
 *
 * <p>Holds a base URL and optional authentication token, then creates fluent request builders via {@link #request(Object...)}.</p>
 *
 * <p>Typical usage:</p>
 * <pre>{@code
 * RESTApi api = new RESTApi("http://localhost:8080/api");
 * api.setToken(myJwtToken);
 *
 * MyObject result = api.request("users", userId)
 *     .get()
 *     .fetch(MyObject.class);
 *
 * api.request("users")
 *     .post()
 *     .bodyJson(newUser)
 *     .fetch(MyObject.class);
 * }</pre>
 */
public class RESTApi {
	static final Logger LOGGER = LoggerFactory.getLogger(RESTApi.class);
	final String baseUrl;
	private String tokenKey = null;
	private String token = null;
	boolean showIOStream = false;

	/**
	 * Creates a new REST API client with the given base URL.
	 * @param baseUrl The root URL of the API (e.g. "http://localhost:8080/api").
	 */
	public RESTApi(final String baseUrl) {
		this.baseUrl = baseUrl;
	}

	/**
	 * Enables detailed HTTP request/response logging for all subsequent requests.
	 * @return This API client instance for chaining.
	 */
	public RESTApi showIOStream() {
		this.showIOStream = true;
		return this;
	}

	/**
	 * Sets a Bearer authentication token for all subsequent requests.
	 * @param token The JWT or Bearer token value.
	 * @return This API client instance for chaining.
	 */
	public RESTApi setToken(final String token) {
		this.token = token;
		this.tokenKey = "Bearer";
		return this;
	}

	/**
	 * Sets an authentication token with a custom scheme for all subsequent requests.
	 * @param token The token value.
	 * @param tokenKey The authentication scheme (e.g. "Bearer", "Basic").
	 * @return This API client instance for chaining.
	 */
	public RESTApi setToken(final String token, final String tokenKey) {
		this.token = token;
		this.tokenKey = tokenKey;
		return this;
	}

	/**
	 * Creates a new request builder targeting the base URL.
	 * @return A new {@link RESTApiRequest} instance.
	 */
	public RESTApiRequest request() {
		return request("");
	}

	/**
	 * Creates a new request builder by appending URL path segments to the base URL.
	 *
	 * <p>Each element is separated by "/" and trailing slashes are removed.
	 * Null elements are skipped.</p>
	 *
	 * @param urlOffset Path segments to append (e.g. "users", userId).
	 * @return A new {@link RESTApiRequest} instance configured with the full URL, token, and IO logging.
	 */
	public RESTApiRequest request(final Object... urlOffset) {
		final StringBuilder url = new StringBuilder();
		url.append(this.baseUrl.replaceAll("/*$", ""));
		for (final Object elem : urlOffset) {
			if (elem == null) {
				continue;
			}
			url.append("/");
			url.append(elem.toString().replaceAll("/*$", ""));
		}
		final RESTApiRequest out = new RESTApiRequest(url.toString(), this.tokenKey, this.token);
		if (this.showIOStream) {
			out.showIOStream();
		}
		return out;
	}
}
