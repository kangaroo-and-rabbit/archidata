package test.atriasoft.archidata.tools;

import java.net.InetAddress;

import org.atriasoft.archidata.exception.InputException;
import org.atriasoft.archidata.tools.SsrfGuard;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** Every range refused by {@link SsrfGuard#isForbidden(InetAddress)}, and the public addresses it lets through. */
class TestSsrfGuard {

	private static final String[] FORBIDDEN_ADDRESSES = { //
			"0.0.0.0", "0.1.2.3", // 0.0.0.0/8
			"10.0.0.1", "10.255.255.255", // 10/8
			"100.64.0.1", "100.127.255.254", // CGNAT 100.64/10
			"127.0.0.1", "127.8.9.10", // loopback
			"169.254.169.254", // link-local, cloud metadata
			"172.16.0.1", "172.31.255.255", // 172.16/12
			"192.0.0.8", "192.0.2.1", // IETF, TEST-NET-1
			"192.168.1.1", // 192.168/16
			"198.18.0.1", "198.19.255.255", // benchmarking
			"198.51.100.7", "203.0.113.9", // TEST-NET-2/3
			"224.0.0.1", "239.255.255.250", // multicast
			"240.0.0.1", "255.255.255.255", // reserved, broadcast
			"::", "::1", // unspecified, loopback
			"::7f00:1", // IPv4-compatible
			"64:ff9b::7f00:1", "64:ff9b:1::1", // NAT64
			"100::1", // discard
			"2001::1", "2001:db8::1", // Teredo, documentation
			"2002:7f00:1::1", // 6to4
			"fc00::1", "fd00:ec2::254", // ULA (AWS IPv6 metadata)
			"fe80::1", "febf::1", // link-local
			"fec0::1", // site-local
			"ff02::1", "ff05::2" // multicast
	};

	private static final String[] PUBLIC_ADDRESSES = { "1.1.1.1", "8.8.8.8", "100.63.255.255", "100.128.0.1",
			"172.15.0.1", "172.32.0.1", "192.0.1.1", "198.17.255.255", "198.20.0.1", "223.255.255.255",
			"2606:4700:4700::1111", "2a00:1450:4007::64" };

	private static final String[] FORBIDDEN_URLS = { "http://127.0.0.1/", "http://[::1]/", "http://[::ffff:127.0.0.1]/",
			"http://[::ffff:8.8.8.8]/", "http://169.254.169.254/latest/meta-data", "http://[fd00:ec2::254]/",
			"http://0.0.0.0:8080/", "http://localhost/" };

	private static final String[] REFUSED_SYNTAX = { "ftp://8.8.8.8/file", "file:///etc/passwd", "gopher://8.8.8.8/",
			"jar:http://8.8.8.8/a!/b", "//8.8.8.8/", "http:///path", "http://user:pass@8.8.8.8/", "" };

	@Test
	void forbiddenAddresses() throws Exception {
		for (final String literal : FORBIDDEN_ADDRESSES) {
			Assertions.assertTrue(SsrfGuard.isForbidden(InetAddress.getByName(literal)), literal);
		}
	}

	@Test
	void publicAddresses() throws Exception {
		for (final String literal : PUBLIC_ADDRESSES) {
			Assertions.assertFalse(SsrfGuard.isForbidden(InetAddress.getByName(literal)), literal);
		}
	}

	@Test
	void forbiddenUrls() {
		for (final String url : FORBIDDEN_URLS) {
			Assertions.assertThrows(InputException.class, () -> SsrfGuard.validateUrl(url), url);
		}
	}

	@Test
	void refusedSyntax() {
		for (final String url : REFUSED_SYNTAX) {
			Assertions.assertThrows(InputException.class, () -> SsrfGuard.checkUrlSyntax(url), url);
		}
	}

	@Test
	void nullUrlIsRefused() {
		Assertions.assertThrows(InputException.class, () -> SsrfGuard.validateUrl(null));
	}

	@Test
	void publicLiteralIsAccepted() throws Exception {
		SsrfGuard.validateUrl("https://8.8.8.8/image.png");
		SsrfGuard.validateUrl("HTTP://[2606:4700:4700::1111]:8080/a?b=c");
	}
}
