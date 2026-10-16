package test.atriasoft.archidata.tools;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.atriasoft.archidata.tools.ConfigBaseVariable;
import org.atriasoft.archidata.tools.JWTWrapper;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import test.atriasoft.archidata.apiExtern.Common;

/** Fetching the SSO public key (https only, bounded, no redirection) and the rate of the refreshes. */
class TestJwtSsoKey {
	private static HttpServer server;
	private static String baseUrl;
	private static String currentKey;
	private static String weakKey;
	private static final AtomicReference<String> RECEIVED_API_KEY = new AtomicReference<>();
	private static final AtomicInteger TARGET_CALLS = new AtomicInteger();

	private static void reply(final HttpExchange exchange, final String publicKey) throws IOException {
		final byte[] data = ("{\"key\":"
				+ com.fasterxml.jackson.databind.json.JsonMapper.builder().build().writeValueAsString(publicKey) + "}")
						.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().add("Content-Type", "application/json");
		exchange.sendResponseHeaders(200, data.length);
		try (OutputStream os = exchange.getResponseBody()) {
			os.write(data);
		}
	}

	@BeforeAll
	static void startServer() throws Exception {
		Assertions.assertNotNull(Common.ADMIN_TOKEN);
		currentKey = JWTWrapper.getPublicKeyJson();
		weakKey = new RSAKeyGenerator(1024, true).generate().toPublicJWK().toJSONString();
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/good/public_key", exchange -> {
			RECEIVED_API_KEY.set(exchange.getRequestHeaders().getFirst("ApiKey"));
			reply(exchange, currentKey);
		});
		server.createContext("/weak/public_key", exchange -> reply(exchange, weakKey));
		server.createContext("/redirect/public_key", exchange -> {
			exchange.getResponseHeaders().add("Location", baseUrl + "/target/public_key");
			exchange.sendResponseHeaders(302, -1);
			exchange.close();
		});
		server.createContext("/target/public_key", exchange -> {
			TARGET_CALLS.incrementAndGet();
			reply(exchange, weakKey);
		});
		server.start();
		baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
	}

	@AfterAll
	static void stopServer() {
		server.stop(0);
		JWTWrapper.initValidateToken(currentKey);
		JWTWrapper.setRefreshTrigger(null);
		ConfigBaseVariable.setSsoToken(null);
	}

	@Test
	void plainHttpToAnotherHostIsRefused() {
		Assertions.assertThrows(IOException.class,
				() -> JWTWrapper.initLocalTokenRemote("http://sso.example.com/karso/api/", "test"));
		Assertions.assertThrows(IOException.class,
				() -> JWTWrapper.initLocalTokenRemote("ftp://sso.example.com/karso/api/", "test"));
	}

	@Test
	void loopbackHttpIsAcceptedAndSendsTheToken() throws Exception {
		ConfigBaseVariable.setSsoToken("sso-secret");
		try {
			JWTWrapper.initLocalTokenRemote(baseUrl + "/good/", "test");
		} finally {
			ConfigBaseVariable.setSsoToken(null);
		}
		Assertions.assertEquals("sso-secret", RECEIVED_API_KEY.get());
		Assertions.assertEquals(currentKey, JWTWrapper.getPublicKeyJson());
	}

	@Test
	void weakKeyIsRefused() {
		Assertions.assertThrows(IOException.class, () -> JWTWrapper.initLocalTokenRemote(baseUrl + "/weak/", "test"));
		Assertions.assertEquals(currentKey, JWTWrapper.getPublicKeyJson());
	}

	@Test
	void redirectionIsNotFollowed() throws Exception {
		JWTWrapper.initLocalTokenRemote(baseUrl + "/redirect/", "test");
		Assertions.assertEquals(0, TARGET_CALLS.get());
		Assertions.assertEquals(currentKey, JWTWrapper.getPublicKeyJson());
	}

	@Test
	void unknownSignatureRefreshesTheKeyOncePerInterval() throws Exception {
		final RSAKey otherKey = new RSAKeyGenerator(2048).generate();
		final SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).build(),
				new JWTClaimsSet.Builder().subject(new ObjectId().toHexString()).issuer("Karso")
						.expirationTime(Date.from(Instant.now().plusSeconds(60))).build());
		jwt.sign(new RSASSASigner(otherKey));
		final String token = jwt.serialize();
		final AtomicInteger refreshes = new AtomicInteger();
		JWTWrapper.setRefreshTrigger(refreshes::incrementAndGet);
		try {
			for (int iii = 0; iii < 5; iii++) {
				Assertions.assertNull(JWTWrapper.validateToken(token, "Karso", null));
			}
		} finally {
			JWTWrapper.setRefreshTrigger(null);
		}
		Assertions.assertEquals(1, refreshes.get());
	}
}
