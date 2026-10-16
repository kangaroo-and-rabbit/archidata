package test.atriasoft.archidata.security;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.atriasoft.archidata.filter.AuthenticationFilter;
import org.atriasoft.archidata.filter.PartRight;
import org.atriasoft.archidata.model.UserByToken;
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

import test.atriasoft.archidata.apiExtern.Common;

/**
 * The filter accepts a JWT only when it is meant for its application: {@code aud} claim, or the
 * {@code application} claim of a token issued without {@code aud}.
 */
class TestJwtAudience {
	private static final String APPLICATION = "karideo";
	/** Public key in place before the test, put back at the end (the shared test tokens use it). */
	private static String previousPublicKey;
	private static RSAKey signingKey;

	private static final class ExposedFilter extends AuthenticationFilter {
		ExposedFilter() {
			super(APPLICATION);
		}

		UserByToken validate(final String token) throws Exception {
			return validateJwtToken(token);
		}
	}

	@BeforeAll
	static void installKey() throws Exception {
		// Load the shared test key first, so it is the one restored afterwards.
		Assertions.assertNotNull(Common.ADMIN_TOKEN);
		previousPublicKey = JWTWrapper.getPublicKeyJson();
		signingKey = new RSAKeyGenerator(2048).keyID("audience-test").generate();
		JWTWrapper.initValidateToken(signingKey.toPublicJWK().toJSONString());
	}

	@AfterAll
	static void restoreKey() {
		JWTWrapper.initValidateToken(previousPublicKey);
	}

	/** A token signed with the test key; {@code audience} and {@code application} may be null. */
	private static String token(final List<String> audience, final String application) throws Exception {
		final JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder().subject(new ObjectId().toHexString())
				.issuer("Karso").claim("login", "user").issueTime(new Date())
				.expirationTime(Date.from(Instant.now().plusSeconds(600)))
				.claim("roles", Map.of(APPLICATION, Map.of("USER", PartRight.READ.getValue())));
		if (audience != null) {
			builder.audience(audience);
		}
		if (application != null) {
			builder.claim("application", application);
		}
		final SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).build(),
				builder.build());
		jwt.sign(new RSASSASigner(signingKey));
		return jwt.serialize();
	}

	@Test
	void audienceOfTheApplicationIsAccepted() throws Exception {
		Assertions.assertNotNull(new ExposedFilter().validate(token(List.of(APPLICATION), APPLICATION)));
		Assertions.assertNotNull(new ExposedFilter().validate(token(List.of("other", APPLICATION), null)));
	}

	@Test
	void audienceOfAnotherApplicationIsRefused() throws Exception {
		Assertions.assertNull(new ExposedFilter().validate(token(List.of("karusic"), "karusic")));
		// The audience wins over an application claim that names this application.
		Assertions.assertNull(new ExposedFilter().validate(token(List.of("karusic"), APPLICATION)));
	}

	@Test
	void legacyTokenWithoutAudienceUsesTheApplicationClaim() throws Exception {
		Assertions.assertNotNull(new ExposedFilter().validate(token(null, APPLICATION)));
		Assertions.assertNull(new ExposedFilter().validate(token(null, "karusic")));
		Assertions.assertNull(new ExposedFilter().validate(token(null, null)));
	}

	@Test
	void generatedTokenCarriesTheAudience() throws Exception {
		final String generated = JWTWrapper.generateJWToken(new ObjectId(), "login", "Karso", APPLICATION, null, null,
				10);
		final JWTClaimsSet claims = SignedJWT.parse(generated).getJWTClaimsSet();
		Assertions.assertEquals(List.of(APPLICATION), claims.getAudience());
		Assertions.assertEquals(APPLICATION, claims.getClaim("application"));
	}
}
