package test.atriasoft.archidata.security;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.atriasoft.archidata.annotation.security.RightAllowed;
import org.atriasoft.archidata.filter.AuthenticationFilter;
import org.atriasoft.archidata.filter.MySecurityContext;
import org.atriasoft.archidata.filter.PartRight;
import org.atriasoft.archidata.model.UserByToken;
import org.atriasoft.archidata.tools.JWTWrapper;
import org.bson.types.ObjectId;
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

import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ResourceInfo;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;
import test.atriasoft.archidata.apiExtern.Common;

/**
 * Extra claims of {@link JWTWrapper#generateJWToken(Object, String, String, String, Map, Map, int, Map)}, and the
 * token of a trusted device ({@code kind=device}): no role at all, refused by every route of the default filter that
 * is not {@code @PermitAll}.
 */
class TestDeviceToken {
	private static final String APPLICATION = "karoto";
	private static final String ISSUER = "Karso";

	private static final class ExposedFilter extends AuthenticationFilter {
		ExposedFilter() {
			super(APPLICATION, ISSUER);
		}

		UserByToken validate(final String token) throws Exception {
			return validateJwtToken(token);
		}
	}

	/** Routes of an application, as the filter sees them. */
	public static final class Routes {
		@PermitAll
		public void open() {}

		@RolesAllowed("USER")
		public void user() {}

		@RolesAllowed("ADMIN")
		public void admin() {}

		@RightAllowed(right = "photos", access = PartRight.READ)
		public void photos() {}
	}

	@BeforeAll
	static void loadKeys() {
		// The shared test key pair signs and checks the tokens of this class.
		Assertions.assertNotNull(Common.ADMIN_TOKEN);
	}

	private static Map<String, Object> deviceClaims() {
		final Map<String, Object> out = new LinkedHashMap<>();
		out.put(JWTWrapper.CLAIM_KIND, JWTWrapper.KIND_DEVICE);
		out.put("scope", "karoto:upload");
		out.put("dev", "6720f1a2b3c4d5e6f7a8b9c0");
		out.put("jti", UUID.randomUUID().toString());
		return out;
	}

	private static Map<String, Object> userRoles() {
		return Map.of(APPLICATION,
				Map.of("USER", PartRight.READ_WRITE.getValue(), "ADMIN", PartRight.READ_WRITE.getValue()));
	}

	private static String deviceToken(final ObjectId owner) {
		return JWTWrapper.generateJWToken(owner, "owner", ISSUER, APPLICATION, null, null, 15, deviceClaims());
	}

	/** What a test does with a token. */
	private interface TokenUse<T> {
		T apply(String token) throws Exception;
	}

	/** Reads with the filter a token that generateJWToken refuses to write. */
	private static UserByToken readForeignToken(final Map<String, Object> claims) throws Exception {
		return withForeignToken(claims, token -> new ExposedFilter().validate(token));
	}

	/**
	 * Uses a token that generateJWToken refuses to write (another SSO, an older one): signed with a key of this test,
	 * the shared public key being put back afterwards.
	 */
	private static <T> T withForeignToken(final Map<String, Object> claims, final TokenUse<T> use) throws Exception {
		final RSAKey key = new RSAKeyGenerator(2048).keyID("device-test").generate();
		final JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder().subject(new ObjectId().toHexString())
				.issuer(ISSUER).audience(APPLICATION).claim("login", "owner").issueTime(new Date())
				.expirationTime(Date.from(Instant.now().plusSeconds(600)));
		for (final Map.Entry<String, Object> entry : claims.entrySet()) {
			builder.claim(entry.getKey(), entry.getValue());
		}
		final SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).build(),
				builder.build());
		jwt.sign(new RSASSASigner(key));
		final String previous = JWTWrapper.getPublicKeyJson();
		JWTWrapper.initValidateToken(key.toPublicJWK().toJSONString());
		try {
			return use.apply(jwt.serialize());
		} finally {
			JWTWrapper.initValidateToken(previous);
		}
	}

	@Test
	void extraClaimsAreSignedAndReadBack() {
		final ObjectId owner = new ObjectId();
		final Map<String, Object> extra = deviceClaims();
		final String token = JWTWrapper.generateJWToken(owner, "owner", ISSUER, APPLICATION, null, null, 15, extra);
		final JWTClaimsSet claims = JWTWrapper.validateToken(token, ISSUER, APPLICATION);
		Assertions.assertNotNull(claims);
		for (final Map.Entry<String, Object> entry : extra.entrySet()) {
			Assertions.assertEquals(entry.getValue(), claims.getClaim(entry.getKey()), entry.getKey());
		}
		Assertions.assertEquals(owner.toHexString(), claims.getSubject());
		Assertions.assertEquals(List.of(APPLICATION), claims.getAudience());
		Assertions.assertEquals(APPLICATION, claims.getClaim("application"));
		Assertions.assertEquals("owner", claims.getClaim("login"));
		Assertions.assertNull(claims.getClaim("roles"));
		Assertions.assertNull(claims.getClaim("right"));
	}

	@Test
	void eachReservedNameIsRefusedWhateverItsCase() {
		final List<String> names = new ArrayList<>();
		for (final String reserved : List.of("sub", "aud", "iss", "iat", "exp", "nbf", "login", "application", "roles",
				"right")) {
			Assertions.assertTrue(JWTWrapper.RESERVED_CLAIMS.contains(reserved), reserved);
			names.add(reserved);
			names.add(reserved.toUpperCase(Locale.ROOT));
			names.add(reserved.substring(0, 1).toUpperCase(Locale.ROOT) + reserved.substring(1));
			names.add(" " + reserved + " ");
		}
		for (final String name : names) {
			final Map<String, Object> extra = new HashMap<>();
			extra.put(JWTWrapper.CLAIM_KIND, JWTWrapper.KIND_DEVICE);
			extra.put(name, "forged");
			Assertions.assertThrows(IllegalArgumentException.class, () -> JWTWrapper.generateJWToken(new ObjectId(),
					"owner", ISSUER, APPLICATION, null, null, 15, extra), name);
		}
	}

	@Test
	void kindIsWrittenOnlyAsDeviceAndWithoutRoles() {
		// Another value, another spelling of the name, a name with spaces: refused when the token is issued.
		final List<Map<String, Object>> refused = List.of(Map.of(JWTWrapper.CLAIM_KIND, "robot"),
				Map.of(JWTWrapper.CLAIM_KIND, "Device"), Map.of(JWTWrapper.CLAIM_KIND, "device "),
				Map.of(JWTWrapper.CLAIM_KIND, 3), Map.of("Kind", JWTWrapper.KIND_DEVICE),
				Map.of("KIND", JWTWrapper.KIND_DEVICE), Map.of(" kind", JWTWrapper.KIND_DEVICE),
				Map.of("scope ", "karoto:upload"));
		for (final Map<String, Object> extra : refused) {
			Assertions.assertThrows(IllegalArgumentException.class, () -> JWTWrapper.generateJWToken(new ObjectId(),
					"owner", ISSUER, APPLICATION, null, null, 15, extra), extra.toString());
		}
		// A device token never carries roles nor rights: a reader that does not know the kind would grant them.
		Assertions.assertThrows(IllegalArgumentException.class, () -> JWTWrapper.generateJWToken(new ObjectId(),
				"owner", ISSUER, APPLICATION, userRoles(), null, 15, deviceClaims()));
		Assertions.assertThrows(IllegalArgumentException.class, () -> JWTWrapper.generateJWToken(new ObjectId(),
				"owner", ISSUER, APPLICATION, null, Map.of(APPLICATION, Map.of("photos", 1)), 15, deviceClaims()));
		Assertions.assertThrows(IllegalArgumentException.class, () -> JWTWrapper.generateJWToken(new ObjectId(),
				"owner", ISSUER, APPLICATION, Map.of(), null, 15, deviceClaims()));
	}

	@Test
	void rolesInjectedThroughTheExtraClaimsAreRefused() {
		final Map<String, Object> extra = new HashMap<>();
		extra.put("roles", userRoles());
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> JWTWrapper.generateJWToken(new ObjectId(), "owner", ISSUER, APPLICATION, null, null, 15, extra));
		// Even with the same value as the roles argument: an extra claim never writes roles.
		Assertions.assertThrows(IllegalArgumentException.class, () -> JWTWrapper.generateJWToken(new ObjectId(),
				"owner", ISSUER, APPLICATION, userRoles(), null, 15, extra));
	}

	@Test
	void nullOrBlankNamesAndNullValuesAreRefused() {
		final Map<String, Object> nullName = new HashMap<>();
		nullName.put(null, "x");
		final Map<String, Object> blankName = new HashMap<>();
		blankName.put("  ", "x");
		final Map<String, Object> nullValue = new HashMap<>();
		nullValue.put("scope", null);
		for (final Map<String, Object> extra : List.of(nullName, blankName, nullValue)) {
			Assertions.assertThrows(IllegalArgumentException.class, () -> JWTWrapper.generateJWToken(new ObjectId(),
					"owner", ISSUER, APPLICATION, null, null, 15, extra));
		}
	}

	@Test
	void withoutExtraClaimsTheTokenIsUnchanged() throws Exception {
		for (final String token : List.of(
				JWTWrapper.generateJWToken(new ObjectId(), "u", ISSUER, APPLICATION, userRoles(), null, 15),
				JWTWrapper.generateJWToken(new ObjectId(), "u", ISSUER, APPLICATION, userRoles(), null, 15, null),
				JWTWrapper.generateJWToken(new ObjectId(), "u", ISSUER, APPLICATION, userRoles(), null, 15,
						Map.of()))) {
			final JWTClaimsSet claims = JWTWrapper.validateToken(token, ISSUER, APPLICATION);
			Assertions.assertNotNull(claims);
			Assertions.assertNull(claims.getClaim(JWTWrapper.CLAIM_KIND));
			Assertions.assertNotNull(claims.getClaim("roles"));
			final UserByToken user = new ExposedFilter().validate(token);
			Assertions.assertEquals(UserByToken.TYPE_USER, user.getType());
			final MySecurityContext context = new MySecurityContext(user, "https", APPLICATION);
			Assertions.assertTrue(context.isUserInRole(APPLICATION + "/USER"));
			Assertions.assertTrue(context.isUserInRole(APPLICATION + "/ADMIN"));
		}
	}

	@Test
	void userTokenWithoutRolesGetsNoRole() throws Exception {
		final String token = JWTWrapper.generateJWToken(new ObjectId(), "u", ISSUER, APPLICATION, null, null, 15);
		final UserByToken user = new ExposedFilter().validate(token);
		Assertions.assertEquals(UserByToken.TYPE_USER, user.getType());
		Assertions.assertTrue(user.getRoles().isEmpty());
		Assertions.assertTrue(user.getRight().isEmpty());
		final MySecurityContext context = new MySecurityContext(user, "https", APPLICATION);
		// The filter checks "application/ROLE": no role entry for the application, no USER.
		Assertions.assertFalse(context.isUserInRole(APPLICATION + "/USER"));
		Assertions.assertFalse(context.isUserInRole(APPLICATION + "/ADMIN"));
		Assertions.assertFalse(context.hasResourceRight(APPLICATION, "photos", PartRight.READ));
	}

	@Test
	void deviceTokenIsTypeDeviceWithoutAnyRole() throws Exception {
		final ObjectId owner = new ObjectId();
		final UserByToken device = new ExposedFilter().validate(deviceToken(owner));
		Assertions.assertNotNull(device);
		Assertions.assertEquals(UserByToken.TYPE_DEVICE, device.getType());
		Assertions.assertEquals(owner, device.getOid());
		Assertions.assertEquals("owner", device.getName());
		Assertions.assertTrue(device.getRoles().isEmpty());
		Assertions.assertTrue(device.getRight().isEmpty());
		final MySecurityContext context = new MySecurityContext(device, "https", APPLICATION);
		Assertions.assertFalse(context.isUserInRole("USER"));
		Assertions.assertFalse(context.isUserInRole(APPLICATION + "/USER"));
		Assertions.assertFalse(context.isUserInRole(APPLICATION + "/ADMIN"));
		Assertions.assertFalse(context.hasResourceRight(APPLICATION, "photos", PartRight.READ));
	}

	@Test
	void rolesOfADeviceTokenAreIgnored() throws Exception {
		// Written by another issuer than generateJWToken, which refuses it.
		final Map<String, Object> claims = new HashMap<>(deviceClaims());
		claims.put("roles", userRoles());
		claims.put("right", Map.of(APPLICATION, Map.of("photos", PartRight.READ_WRITE.getValue())));
		final UserByToken device = readForeignToken(claims);
		Assertions.assertEquals(UserByToken.TYPE_DEVICE, device.getType());
		Assertions.assertTrue(device.getRoles().isEmpty());
		Assertions.assertTrue(device.getRight().isEmpty());
		final MySecurityContext context = new MySecurityContext(device, "https", APPLICATION);
		Assertions.assertFalse(context.isUserInRole(APPLICATION + "/USER"));
		Assertions.assertFalse(context.isUserInRole(APPLICATION + "/ADMIN"));
		Assertions.assertFalse(context.hasResourceRight(APPLICATION, "photos", PartRight.READ));
	}

	@Test
	void unknownKindIsRefused() throws Exception {
		for (final Object kind : List.of("robot", "Device", "device ", "", 3, List.of("device"))) {
			final Map<String, Object> claims = new HashMap<>();
			claims.put(JWTWrapper.CLAIM_KIND, kind);
			claims.put("roles", userRoles());
			Assertions.assertNull(readForeignToken(claims), String.valueOf(kind));
		}
		// The control: the same token without kind is a person's.
		Assertions.assertEquals(UserByToken.TYPE_USER, readForeignToken(Map.of("roles", userRoles())).getType());
	}

	@Test
	void deviceIsNeverUserEvenWithRolesGivenByAnApplication() {
		final UserByToken device = new UserByToken();
		device.setOid(new ObjectId());
		device.setType(UserByToken.TYPE_DEVICE);
		// What an application filter may add to let its own device routes through.
		device.getRoles().put(APPLICATION,
				new HashMap<>(Map.of("DEVICE", PartRight.READ_WRITE, "USER", PartRight.READ_WRITE)));
		final MySecurityContext context = new MySecurityContext(device, "https", APPLICATION);
		Assertions.assertTrue(context.isUserInRole(APPLICATION + "/DEVICE"));
		Assertions.assertFalse(context.isUserInRole(APPLICATION + "/USER"));
		Assertions.assertFalse(context.isUserInRole("USER"));
		Assertions.assertFalse(context.checkRightInGroup(APPLICATION, "USER", false, false));
	}

	/** What the filter did with a request. */
	private static final class Outcome {
		Response aborted;
		SecurityContext installed;
	}

	private static Outcome run(final String route, final String token) throws Exception {
		final Method method = Routes.class.getMethod(route);
		final ResourceInfo info = (ResourceInfo) Proxy.newProxyInstance(TestDeviceToken.class.getClassLoader(),
				new Class<?>[] { ResourceInfo.class }, (proxy, called, args) -> switch (called.getName()) {
					case "getResourceMethod" -> method;
					case "getResourceClass" -> Routes.class;
					default -> null;
				});
		final UriInfo uri = (UriInfo) Proxy.newProxyInstance(TestDeviceToken.class.getClassLoader(),
				new Class<?>[] { UriInfo.class }, (proxy, called, args) -> switch (called.getName()) {
					case "getPath" -> route;
					case "getRequestUri" -> URI.create("https://localhost/" + route);
					case "getQueryParameters" -> new MultivaluedHashMap<String, String>();
					default -> null;
				});
		final Outcome outcome = new Outcome();
		final ContainerRequestContext request = (ContainerRequestContext) Proxy.newProxyInstance(
				TestDeviceToken.class.getClassLoader(), new Class<?>[] { ContainerRequestContext.class },
				(proxy, called, args) -> {
					switch (called.getName()) {
						case "getHeaderString":
							return HttpHeaders.AUTHORIZATION.equals(args[0]) ? "Bearer " + token : null;
						case "getUriInfo":
							return uri;
						case "abortWith":
							outcome.aborted = (Response) args[0];
							return null;
						case "setSecurityContext":
							outcome.installed = (SecurityContext) args[0];
							return null;
						default:
							return null;
					}
				});
		final AuthenticationFilter filter = new AuthenticationFilter(APPLICATION, ISSUER);
		final Field field = AuthenticationFilter.class.getDeclaredField("resourceInfo");
		field.setAccessible(true);
		field.set(filter, info);
		filter.filter(request);
		return outcome;
	}

	@Test
	void defaultFilterRefusesADeviceOnEveryRouteThatChecksAccess() throws Exception {
		final String device = deviceToken(new ObjectId());
		for (final String route : List.of("user", "admin", "photos")) {
			final Outcome outcome = run(route, device);
			Assertions.assertNotNull(outcome.aborted, route);
			Assertions.assertEquals(403, outcome.aborted.getStatus(), route);
			Assertions.assertNull(outcome.installed, route);
		}
		// The same routes with the token of a person who has the roles: accepted (the test checks the test).
		final String user = JWTWrapper.generateJWToken(new ObjectId(), "u", ISSUER, APPLICATION, userRoles(),
				Map.of(APPLICATION, Map.of("photos", PartRight.READ.getValue())), 15);
		for (final String route : List.of("user", "admin", "photos")) {
			final Outcome outcome = run(route, user);
			Assertions.assertNull(outcome.aborted, route);
			Assertions.assertNotNull(outcome.installed, route);
		}
	}

	@Test
	void permitAllIgnoresTheTokenOfADevice() throws Exception {
		// @PermitAll does not read the token: the request goes on as an anonymous one, without security context.
		final Outcome outcome = run("open", deviceToken(new ObjectId()));
		Assertions.assertNull(outcome.aborted);
		Assertions.assertNull(outcome.installed);
	}

	@Test
	void unknownKindAnswers401() throws Exception {
		final Outcome outcome = withForeignToken(Map.of(JWTWrapper.CLAIM_KIND, "robot", "roles", userRoles()),
				token -> run("user", token));
		Assertions.assertNotNull(outcome.aborted);
		Assertions.assertEquals(401, outcome.aborted.getStatus());
	}
}
