package org.atriasoft.archidata.tools;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;

import org.atriasoft.archidata.exception.InputException;

/**
 * Utility class to prevent Server-Side Request Forgery (SSRF) attacks.
 *
 * <p>Validates URLs by resolving the hostname to an IP address and checking
 * that the resolved IP is not in a private, loopback, or link-local range.</p>
 */
public class SsrfGuard {
	private SsrfGuard() {
		// Utility class
	}

	/**
	 * Validates a URL against SSRF attacks by checking scheme and resolved IP address.
	 * @param url The URL to validate.
	 * @throws InputException If the URL is null, uses a non-HTTP scheme, or resolves to a private/internal IP.
	 */
	public static void validateUrl(final String url) throws InputException {
		if (url == null || url.isEmpty()) {
			throw new InputException("url", "URL is missing");
		}
		final URI uri;
		try {
			uri = new URI(url);
		} catch (final URISyntaxException e) {
			throw new InputException("url", "Invalid URL format");
		}
		final String scheme = uri.getScheme();
		if (scheme == null || (!"http".equals(scheme) && !"https".equals(scheme))) {
			throw new InputException("url", "Only HTTP and HTTPS URLs are allowed");
		}
		final String host = uri.getHost();
		if (host == null || host.isEmpty()) {
			throw new InputException("url", "URL has no host");
		}
		// Resolve the hostname to an IP address and check against private ranges
		final InetAddress resolvedAddress;
		try {
			resolvedAddress = InetAddress.getByName(host);
		} catch (final UnknownHostException e) {
			throw new InputException("url", "Cannot resolve hostname: " + host);
		}
		if (resolvedAddress.isLoopbackAddress()) {
			throw new InputException("url", "Access to loopback addresses is forbidden");
		}
		if (resolvedAddress.isSiteLocalAddress()) {
			throw new InputException("url", "Access to internal network addresses is forbidden");
		}
		if (resolvedAddress.isLinkLocalAddress()) {
			throw new InputException("url", "Access to link-local addresses is forbidden");
		}
		if (resolvedAddress.isAnyLocalAddress()) {
			throw new InputException("url", "Access to wildcard addresses is forbidden");
		}
	}
}
