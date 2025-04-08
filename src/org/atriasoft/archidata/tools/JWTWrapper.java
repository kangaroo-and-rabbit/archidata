package org.atriasoft.archidata.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.atriasoft.archidata.filter.AuthenticationFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jca.JCAContext;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

class TestSigner implements JWSSigner {
	public static String test_signature = "TEST_SIGNATURE_FOR_LOCAL_TEST_AND_TEST_E2E";

	/** Signs the specified {@link JWSObject#getSigningInput input} of a {@link JWSObject JWS object}.
	 *
	 * @param header The JSON Web Signature (JWS) header. Must specify a supported JWS algorithm and must not be {@code null}.
	 * @param signingInput The input to sign. Must not be {@code null}.
	 *
	 * @return The resulting signature part (third part) of the JWS object.
	 *
	 * @throws JOSEException If the JWS algorithm is not supported, if a critical header parameter is not supported or marked for deferral to the application, or if signing failed for some other
	 *             internal reason. */
	@Override
	public Base64URL sign(final JWSHeader header, final byte[] signingInput) throws JOSEException {
		return new Base64URL(test_signature);
	}

	@Override
	public Set<JWSAlgorithm> supportedJWSAlgorithms() {
		// TODO Auto-generated method stub
		return Set.of(JWSAlgorithm.RS256);
	}

	@Override
	public JCAContext getJCAContext() {
		// TODO Auto-generated method stub
		return new JCAContext();
	}
}

public class JWTWrapper {
	static final Logger LOGGER = LoggerFactory.getLogger(JWTWrapper.class);

	private static RSAKey rsaJWK = null;
	private static RSAKey rsaPublicJWK = null;

	public static class PublicKey {
		public String key;

		public PublicKey(final String key) {
			this.key = key;
		}

		public PublicKey() {}
	}

	public static void initLocalTokenRemote(final String ssoUri, final String application)
			throws IOException, ParseException {
		// check Token:
		final URL obj = new URL(ssoUri + "public_key");
		// LOGGER.debug("Request token from: {}", obj);
		final HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		con.setRequestMethod("GET");
		con.setRequestProperty("User-Agent", application);
		con.setRequestProperty("Cache-Control", "no-cache");
		con.setRequestProperty("Content-Type", "application/json");
		con.setRequestProperty("Accept", "application/json");
		final String ssoToken = ConfigBaseVariable.ssoToken();
		if (ssoToken != null) {
			con.setRequestProperty(AuthenticationFilter.APIKEY, ssoToken);
		}
		final int responseCode = con.getResponseCode();

		// LOGGER.debug("GET Response Code :: {}", responseCode);
		if (responseCode == HttpURLConnection.HTTP_OK) { // success
			final BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));

			String inputLine;
			final StringBuffer response = new StringBuffer();
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
			// print result
			LOGGER.debug(response.toString());
			final ObjectMapper mapper = ContextGenericTools.createObjectMapper();
			final PublicKey values = mapper.readValue(response.toString(), PublicKey.class);
			rsaPublicJWK = RSAKey.parse(values.key);
			return;
		}
		LOGGER.debug("GET JWT validator token not worked response code {} from {} ", responseCode, obj);
	}

	public static void initLocalToken(final String baseUUID) throws Exception {
		// RSA signatures require a public and private RSA key pair, the public key
		// must be made known to the JWS recipient in order to verify the signatures
		try {
			String generatedStringForKey = baseUUID;
			if (generatedStringForKey == null) {
				LOGGER.error(" Generate new UUID : {}", generatedStringForKey);
				generatedStringForKey = UUID.randomUUID().toString();
			} else {
				LOGGER.error("USE UUID : {}", generatedStringForKey);
			}
			rsaJWK = new RSAKeyGenerator(2048).keyID(generatedStringForKey).generate();
			rsaPublicJWK = rsaJWK.toPublicJWK();
			LOGGER.error("RSA key (all): " + rsaJWK.toJSONString());
			LOGGER.error("RSA key (pub): " + rsaPublicJWK.toJSONString());
		} catch (final JOSEException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			LOGGER.debug("Can not generate teh  public abnd private keys ...");
			rsaJWK = null;
			rsaPublicJWK = null;
		}
	}

	public static void initValidateToken(final String publicKey) {
		try {
			rsaPublicJWK = RSAKey.parse(publicKey);
		} catch (final ParseException e) {
			e.printStackTrace();
			LOGGER.debug("Can not retrieve public Key !!!!!!!! RSAKey='{}'", publicKey);
		}

	}

	public static String getPublicKeyJson() {
		if (rsaPublicJWK == null) {
			return null;
		}
		return rsaPublicJWK.toJSONString();
	}

	public static java.security.interfaces.RSAPublicKey getPublicKeyJava() throws JOSEException {
		if (rsaPublicJWK == null) {
			return null;
		}
		// Convert back to std Java interface
		return rsaPublicJWK.toRSAPublicKey();
	}

	/** Create a token with the provided elements
	 * @param userID UniqueId of the USER (global unique ID)
	 * @param userLogin Login of the user (never change)
	 * @param isuer The one who provide the Token
	 * @param timeOutInMunites Expiration of the token.
	 * @return the encoded token */
	public static String generateJWToken(
			final long userID,
			final String userLogin,
			final String isuer,
			final String application,
			final Map<String, Object> rights,
			final int timeOutInMunites) {
		if (rsaJWK == null) {
			LOGGER.warn("JWT private key is not present !!!");
			return null;
		}
		/* LOGGER.debug(" ===> expire in : " + timeOutInMunites); LOGGER.debug(" ===>" + new Date().getTime()); LOGGER.debug(" ===>" + new Date(new Date().getTime())); LOGGER.debug(" ===>" + new
		 * Date(new Date().getTime() - 60 * timeOutInMunites * 1000)); */
		try {
			// Create RSA-signer with the private key
			final JWSSigner signer = new RSASSASigner(rsaJWK);

			LOGGER.warn("timeOutInMunites= {}", timeOutInMunites);
			final Date now = new Date();
			LOGGER.warn("now       = {}", now);
			final Date expiration = new Date(new Date().getTime() - 60 * timeOutInMunites * 1000 /* millisecond */);

			LOGGER.warn("expiration= {}", expiration);
			final JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder().subject(Long.toString(userID))
					.claim("login", userLogin).claim("application", application).issuer(isuer).issueTime(now)
					.expirationTime(expiration); // Do not ask why we need a "-" here ... this have no meaning
			// add right if needed:
			if (rights != null) {
				builder.claim("right", rights);
			}
			// Prepare JWT with claims set
			final JWTClaimsSet claimsSet = builder.build();
			final SignedJWT signedJWT = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT)
					/* .keyID(rsaJWK.getKeyID()) */.build(), claimsSet);

			// Compute the RSA signature
			signedJWT.sign(signer);
			// serialize the output...
			return signedJWT.serialize();
		} catch (final JOSEException ex) {
			ex.printStackTrace();
		}
		return null;
	}

	public static JWTClaimsSet validateToken(final String signedToken, final String isuer, final String application) {
		try {
			// On the consumer side, parse the JWS and verify its RSA signature
			final SignedJWT signedJWT = SignedJWT.parse(signedToken);
			if (signedJWT == null) {
				LOGGER.error("FAIL to parse signing");
				return null;
			}
			if (ConfigBaseVariable.getTestMode() && signedToken.endsWith(TestSigner.test_signature)) {
				LOGGER.warn("Someone use a test token: {}", signedToken);
			} else if (rsaPublicJWK == null) {
				LOGGER.warn("JWT public key is not present !!!");
				if (!ConfigBaseVariable.getTestMode()) {
					return null;
				}
				final String rawSignature = new String(signedJWT.getSigningInput(), StandardCharsets.UTF_8);
				if (rawSignature.equals(TestSigner.test_signature)) {
					// Test token : .application..
				} else {
					return null;
				}
			} else {
				final JWSVerifier verifier = new RSASSAVerifier(rsaPublicJWK);
				if (!signedJWT.verify(verifier)) {
					LOGGER.error("JWT token is NOT verified ");
					return null;
				}
			}
			if (!ConfigBaseVariable.getTestMode()
					&& !new Date().before(signedJWT.getJWTClaimsSet().getExpirationTime())) {
				LOGGER.error("JWT token is expired now = " + new Date() + " with="
						+ signedJWT.getJWTClaimsSet().getExpirationTime());
				return null;
			}
			if (!isuer.equals(signedJWT.getJWTClaimsSet().getIssuer())) {
				LOGGER.error(
						"JWT issuer is wong: '" + isuer + "' != '" + signedJWT.getJWTClaimsSet().getIssuer() + "'");
				return null;
			}
			if (application != null) {
				// TODO: verify the token is used for the correct application.
			}
			// the element must be validated outside ...
			// LOGGER.debug("JWT token is verified 'alice' =?= '" + signedJWT.getJWTClaimsSet().getSubject() + "'");
			// LOGGER.debug("JWT token isuer 'https://c2id.com' =?= '" + signedJWT.getJWTClaimsSet().getIssuer() + "'");
			return signedJWT.getJWTClaimsSet();
		} catch (final JOSEException | ParseException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static String createJwtTestToken(
			final long userID,
			final String userLogin,
			final String isuer,
			final String application,
			final Map<String, Map<String, Object>> rights) {
		if (!ConfigBaseVariable.getTestMode()) {
			LOGGER.error("Test mode disable !!!!!");
			return null;
		}
		try {
			final int timeOutInMunites = 3600;

			final Date now = new Date();
			final Date expiration = new Date(new Date().getTime() + timeOutInMunites * 1000 /* ms */);

			final JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder().subject(Long.toString(userID))
					.claim("login", userLogin).claim("application", application).issuer(isuer).issueTime(now)
					.expirationTime(expiration); // Do not ask why we need a "-" here ... this have no meaning
			// add right if needed:
			if (rights != null && !rights.isEmpty()) {
				builder.claim("right", rights);
			}
			// Prepare JWT with claims set
			final JWTClaimsSet claimsSet = builder.build();
			final SignedJWT signedJWT = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).type(JOSEObjectType.JWT)
					/* .keyID(rsaJWK.getKeyID()) */.build(), claimsSet);

			// Compute the RSA signature
			signedJWT.sign(new TestSigner());

			// serialize the output...
			return signedJWT.serialize();
		} catch (final Exception ex) {
			ex.printStackTrace();
			LOGGER.error("Can not generate Test Token... {}", ex.getLocalizedMessage());
		}
		return null;
	}
}
