package org.atriasoft.archidata.tools;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.function.Predicate;

import org.atriasoft.archidata.exception.InputException;

/**
 * Utility class to prevent Server-Side Request Forgery (SSRF) attacks.
 *
 * <p>
 * A URL is accepted only when its scheme is {@code http} or {@code https}, it carries no user
 * information, and <b>every</b> address its host resolves to is a public unicast address. The ranges
 * refused by {@link #isForbidden(InetAddress)} are:
 * </p>
 * <ul>
 * <li>IPv4: {@code 0.0.0.0/8}, {@code 10.0.0.0/8}, {@code 100.64.0.0/10} (CGNAT), {@code 127.0.0.0/8},
 * {@code 169.254.0.0/16}, {@code 172.16.0.0/12}, {@code 192.0.0.0/24}, {@code 192.0.2.0/24},
 * {@code 192.168.0.0/16}, {@code 198.18.0.0/15}, {@code 198.51.100.0/24}, {@code 203.0.113.0/24},
 * {@code 224.0.0.0/4} (multicast) and {@code 240.0.0.0/4} (reserved, broadcast included);</li>
 * <li>IPv6: {@code ::/96} (unspecified, loopback, IPv4-compatible), {@code ::ffff:0:0/96} (IPv4-mapped),
 * {@code 64:ff9b::/96} and {@code 64:ff9b:1::/48} (NAT64), {@code 100::/64} (discard), {@code 2001::/32}
 * (Teredo), {@code 2001:db8::/32} (documentation), {@code 2002::/16} (6to4), {@code fc00::/7} (ULA),
 * {@code fe80::/10} (link-local), {@code fec0::/10} (site-local) and {@code ff00::/8} (multicast).</li>
 * </ul>
 * <p>
 * Validating a URL is not enough to fetch it safely: the HTTP client could follow a redirection or
 * resolve the name again (DNS rebinding). Use {@link SafeHttpFetcher}, which connects to the address
 * validated here and checks every redirection again.
 * </p>
 */
public class SsrfGuard {
	private SsrfGuard() {
		// Utility class
	}

	/**
	 * Tells whether an address must never be reached by a request built from user input.
	 * @param address The resolved address to check.
	 * @return {@code true} when the address is private, loopback, link-local, multicast, reserved, or a
	 *         transition address that can embed one of them.
	 */
	public static boolean isForbidden(final InetAddress address) {
		if (address == null) {
			return true;
		}
		if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
				|| address.isSiteLocalAddress() || address.isMulticastAddress()) {
			return true;
		}
		final byte[] raw = address.getAddress();
		if (address instanceof Inet4Address) {
			return isForbiddenIpv4(raw);
		}
		if (address instanceof Inet6Address) {
			return isForbiddenIpv6(raw);
		}
		return true;
	}

	private static int unsigned(final byte value) {
		return value & 0xFF;
	}

	private static boolean isForbiddenIpv4(final byte[] raw) {
		final int b0 = unsigned(raw[0]);
		final int b1 = unsigned(raw[1]);
		final int b2 = unsigned(raw[2]);
		return b0 == 0 // 0.0.0.0/8 "this network"
				|| b0 == 10 // 10.0.0.0/8
				|| (b0 == 100 && (b1 & 0xC0) == 64) // 100.64.0.0/10 CGNAT
				|| b0 == 127 // 127.0.0.0/8
				|| (b0 == 169 && b1 == 254) // 169.254.0.0/16 link-local (cloud metadata)
				|| (b0 == 172 && (b1 & 0xF0) == 16) // 172.16.0.0/12
				|| (b0 == 192 && b1 == 0 && b2 == 0) // 192.0.0.0/24 IETF protocol assignments
				|| (b0 == 192 && b1 == 0 && b2 == 2) // 192.0.2.0/24 TEST-NET-1
				|| (b0 == 192 && b1 == 168) // 192.168.0.0/16
				|| (b0 == 198 && (b1 & 0xFE) == 18) // 198.18.0.0/15 benchmarking
				|| (b0 == 198 && b1 == 51 && b2 == 100) // 198.51.100.0/24 TEST-NET-2
				|| (b0 == 203 && b1 == 0 && b2 == 113) // 203.0.113.0/24 TEST-NET-3
				|| b0 >= 224; // 224.0.0.0/4 multicast, 240.0.0.0/4 reserved and broadcast
	}

	private static boolean allZero(final byte[] raw, final int from, final int to) {
		for (int iii = from; iii < to; iii++) {
			if (raw[iii] != 0) {
				return false;
			}
		}
		return true;
	}

	private static boolean isForbiddenIpv6(final byte[] raw) {
		final int b0 = unsigned(raw[0]);
		final int b1 = unsigned(raw[1]);
		final int b2 = unsigned(raw[2]);
		final int b3 = unsigned(raw[3]);
		// ::/96: unspecified, loopback and IPv4-compatible addresses
		if (allZero(raw, 0, 12)) {
			return true;
		}
		// ::ffff:0:0/96: IPv4-mapped addresses (the JDK usually turns them into Inet4Address)
		if (allZero(raw, 0, 10) && unsigned(raw[10]) == 0xFF && unsigned(raw[11]) == 0xFF) {
			return true;
		}
		// 64:ff9b::/96 NAT64 and 64:ff9b:1::/48 local-use NAT64
		if (b0 == 0x00 && b1 == 0x64 && b2 == 0xFF && b3 == 0x9B) {
			return true;
		}
		// 100::/64 discard-only
		if (b0 == 0x01 && b1 == 0x00 && allZero(raw, 2, 8)) {
			return true;
		}
		// 2001::/32 Teredo, 2001:db8::/32 documentation
		if (b0 == 0x20 && b1 == 0x01 && ((b2 == 0x00 && b3 == 0x00) || (b2 == 0x0D && b3 == 0xB8))) {
			return true;
		}
		// 2002::/16 6to4 (embeds an IPv4 address)
		if (b0 == 0x20 && b1 == 0x02) {
			return true;
		}
		// fc00::/7 unique local addresses (fd00:ec2::254 is the AWS IPv6 metadata service)
		if ((b0 & 0xFE) == 0xFC) {
			return true;
		}
		// fe80::/10 link-local, fec0::/10 site-local
		if (b0 == 0xFE && (b1 & 0x80) == 0x80) {
			return true;
		}
		// ff00::/8 multicast
		return b0 == 0xFF;
	}

	/**
	 * Parses a URL and checks its syntax: scheme {@code http} or {@code https}, a host, no user information.
	 * The host is not resolved.
	 * @param url The URL to check.
	 * @return The parsed URI.
	 * @throws InputException If the URL is missing, malformed, uses another scheme, has no host or carries
	 *                        credentials.
	 */
	public static URI checkUrlSyntax(final String url) throws InputException {
		if (url == null || url.isEmpty()) {
			throw new InputException("url", "URL is missing");
		}
		final URI uri;
		try {
			uri = new URI(url);
		} catch (final URISyntaxException e) {
			throw new InputException("url", "Invalid URL format");
		}
		return checkUriSyntax(uri);
	}

	/**
	 * Checks the syntax of an already parsed URI (see {@link #checkUrlSyntax(String)}).
	 * @param uri The URI to check.
	 * @return The same URI.
	 * @throws InputException If the URI is not an absolute http(s) URI with a host and without credentials.
	 */
	public static URI checkUriSyntax(final URI uri) throws InputException {
		final String scheme = uri.getScheme();
		if (scheme == null || (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))) {
			throw new InputException("url", "Only HTTP and HTTPS URLs are allowed");
		}
		final String host = uri.getHost();
		if (host == null || host.isEmpty()) {
			throw new InputException("url", "URL has no host");
		}
		if (uri.getRawUserInfo() != null) {
			throw new InputException("url", "URL must not carry credentials");
		}
		return uri;
	}

	/**
	 * Returns the host of a URI without the brackets of an IPv6 literal.
	 * @param uri The URI.
	 * @return The bare host name or address literal.
	 */
	public static String bareHost(final URI uri) {
		final String host = uri.getHost();
		if (host != null && host.startsWith("[") && host.endsWith("]")) {
			return host.substring(1, host.length() - 1);
		}
		return host;
	}

	/**
	 * Resolves the host of a URI once and checks every address it resolves to.
	 * @param uri The URI whose host is resolved (its syntax must have been checked).
	 * @param forbidden The predicate that tells which addresses are refused.
	 * @return The first resolved address: connect to this one, do not resolve the name again.
	 * @throws InputException If the host cannot be resolved or one of its addresses is refused.
	 */
	public static InetAddress resolveAllowed(final URI uri, final Predicate<InetAddress> forbidden)
			throws InputException {
		final String host = bareHost(uri);
		final InetAddress[] addresses;
		try {
			addresses = InetAddress.getAllByName(host);
		} catch (final UnknownHostException e) {
			throw new InputException("url", "Cannot resolve hostname");
		}
		if (addresses.length == 0) {
			throw new InputException("url", "Cannot resolve hostname");
		}
		// An IPv6 literal that the JDK turned into an IPv4 address is an IPv4-mapped address.
		if (uri.getHost().startsWith("[") && addresses[0] instanceof Inet4Address) {
			throw new InputException("url", "Access to IPv4-mapped IPv6 addresses is forbidden");
		}
		for (final InetAddress address : addresses) {
			if (forbidden.test(address)) {
				throw new InputException("url", "Access to internal network addresses is forbidden");
			}
		}
		return addresses[0];
	}

	/**
	 * Validates a URL against SSRF attacks by checking scheme and every resolved IP address.
	 * <p>
	 * This only checks the URL: to fetch it, use {@link SafeHttpFetcher}, which pins the validated address
	 * and checks every redirection.
	 * </p>
	 * @param url The URL to validate.
	 * @throws InputException If the URL is null, uses a non-HTTP scheme, or resolves to a private/internal IP.
	 */
	public static void validateUrl(final String url) throws InputException {
		final URI uri = checkUrlSyntax(url);
		resolveAllowed(uri, SsrfGuard::isForbidden);
	}
}
