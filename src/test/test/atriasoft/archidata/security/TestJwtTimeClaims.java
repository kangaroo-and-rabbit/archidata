package test.atriasoft.archidata.security;

import java.time.Instant;
import java.util.Date;

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

/** {@code exp} is mandatory and {@code nbf} is honoured (W-16). */
class TestJwtTimeClaims {
	private static String previousPublicKey;
	private static RSAKey signingKey;

	@BeforeAll
	static void installKey() throws Exception {
		Assertions.assertNotNull(Common.ADMIN_TOKEN);
		previousPublicKey = JWTWrapper.getPublicKeyJson();
		signingKey = new RSAKeyGenerator(2048).generate();
		JWTWrapper.initValidateToken(signingKey.toPublicJWK().toJSONString());
	}

	@AfterAll
	static void restoreKey() {
		JWTWrapper.initValidateToken(previousPublicKey);
	}

	private static String token(final Instant expiration, final Instant notBefore) throws Exception {
		final JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder().subject(new ObjectId().toHexString())
				.issuer("Karso");
		if (expiration != null) {
			builder.expirationTime(Date.from(expiration));
		}
		if (notBefore != null) {
			builder.notBeforeTime(Date.from(notBefore));
		}
		final SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT).build(),
				builder.build());
		jwt.sign(new RSASSASigner(signingKey));
		return jwt.serialize();
	}

	@Test
	void tokenWithoutExpirationIsRefused() throws Exception {
		Assertions.assertNull(JWTWrapper.validateToken(token(null, null), "Karso", null));
	}

	@Test
	void expiredTokenIsRefused() throws Exception {
		Assertions.assertNull(JWTWrapper.validateToken(token(Instant.now().minusSeconds(1), null), "Karso", null));
	}

	@Test
	void validTokenIsAccepted() throws Exception {
		Assertions.assertNotNull(JWTWrapper.validateToken(token(Instant.now().plusSeconds(600), null), "Karso", null));
		// within the clock tolerance
		Assertions.assertNotNull(JWTWrapper
				.validateToken(token(Instant.now().plusSeconds(600), Instant.now().plusSeconds(10)), "Karso", null));
	}

	@Test
	void tokenNotValidYetIsRefused() throws Exception {
		Assertions.assertNull(JWTWrapper
				.validateToken(token(Instant.now().plusSeconds(600), Instant.now().plusSeconds(300)), "Karso", null));
	}
}
