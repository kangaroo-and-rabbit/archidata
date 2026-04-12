package org.atriasoft.archidata.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.atriasoft.archidata.filter.AuthenticationFilter;
import org.bson.types.ObjectId;
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
	public static final String TEST_SIGNATURE = "TEST_SIGNATURE_FOR_LOCAL_TEST_AND_TEST_E2E";

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
		return new Base64URL(TEST_SIGNATURE);
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

/**
 * Utility class for JWT (JSON Web Token) generation and validation using RSA keys.
 *
 * <p>Supports both local key generation and remote public key retrieval for SSO scenarios.</p>
 */
public class JWTWrapper {
	private JWTWrapper() {
		// Utility class
	}

	static final Logger LOGGER = LoggerFactory.getLogger(JWTWrapper.class);

	private static RSAKey rsaJWK = null;
	private static RSAKey rsaPublicJWK = null;

	/** DTO for exchanging the RSA public key via REST. */
	public static class PublicKey {
		/** The RSA public key in JSON string format. */
		public String key;

		/** Default constructor for Jackson deserialization. */
		public PublicKey() {}

		/**
		 * Creates a new PublicKey with the given key string.
		 * @param key The RSA public key in JSON string format.
		 */
		public PublicKey(final String key) {
			this.key = key;
		}
	}

	/**
	 * Initializes the JWT public key by fetching it from a remote SSO server.
	 * @param ssoUri The base URI of the SSO service.
	 * @param application The application name sent as User-Agent.
	 * @throws IOException If the HTTP request fails.
	 * @throws ParseException If the public key cannot be parsed.
	 */
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
			final StringBuilder response = new StringBuilder();
			while ((inputLine = in.readLine()) != null) {
				response.append(inputLine);
			}
			in.close();
			// print result
			// Do not commit security fail: LOGGER.debug(response.toString());
			final ObjectMapper mapper = ContextGenericTools.createObjectMapper();
			final PublicKey values = mapper.readValue(response.toString(), PublicKey.class);
			rsaPublicJWK = RSAKey.parse(values.key);
			return;
		}
		LOGGER.debug("GET JWT validator token not worked response code {} from {} ", responseCode, obj);
	}

	/**
	 * Generates a local RSA key pair for JWT signing and verification.
	 * @param baseUUID The key ID to use, or {@code null} to generate a random UUID.
	 * @throws Exception If key generation fails.
	 */
	public static void initLocalToken(final String baseUUID) throws Exception {
		// RSA signatures require a public and private RSA key pair, the public key
		// must be made known to the JWS recipient in order to verify the signatures
		try {
			String generatedStringForKey = baseUUID;
			if (generatedStringForKey == null) {
				// LOGGER.trace("Generate new UUID: {}", generatedStringForKey);
				generatedStringForKey = UUID.randomUUID().toString();
			} else {
				// LOGGER.trace("USE UUID: {}", generatedStringForKey);
			}
			rsaJWK = new RSAKeyGenerator(2048).keyID(generatedStringForKey).generate();
			rsaPublicJWK = rsaJWK.toPublicJWK();
			// LOGGER.trace("RSA key (all): " + rsaJWK.toJSONString());
			// LOGGER.trace("RSA key (pub): " + rsaPublicJWK.toJSONString());
		} catch (final JOSEException e) {
			LOGGER.error("Can not generate keys, aborting private keys: {}", e.getMessage(), e);
			rsaJWK = null;
			rsaPublicJWK = null;
		}
	}

	/**
	 * Initializes the JWT validator with a public key in JSON format.
	 * @param publicKey The RSA public key in JSON string format.
	 */
	public static void initValidateToken(final String publicKey) {
		try {
			rsaPublicJWK = RSAKey.parse(publicKey);
		} catch (final ParseException e) {
			LOGGER.error("Can not parse public key: {}", e.getMessage(), e);
		}

	}

	/**
	 * Returns the public key as a JSON string.
	 * @return The RSA public key JSON, or {@code null} if not initialized.
	 */
	public static String getPublicKeyJson() {
		if (rsaPublicJWK == null) {
			return null;
		}
		return rsaPublicJWK.toJSONString();
	}

	/**
	 * Returns the public key as a standard Java {@link java.security.interfaces.RSAPublicKey}.
	 * @return The RSA public key, or {@code null} if not initialized.
	 * @throws JOSEException If the key conversion fails.
	 */
	public static java.security.interfaces.RSAPublicKey getPublicKeyJava() throws JOSEException {
		if (rsaPublicJWK == null) {
			return null;
		}
		// Convert back to std Java interface
		return rsaPublicJWK.toRSAPublicKey();
	}

	/** Create a token with the provided elements.
	 * @param userID UniqueId of the USER (global unique ID).
	 * @param userLogin Login of the user (never change).
	 * @param issuer The one who provides the token.
	 * @param application The target application name.
	 * @param roles Optional map of roles to include (high-level: ADMIN, USER).
	 * @param rights Optional map of fine-grained rights to include (articles, users).
	 * @param timeoutInMinutes Expiration delay in minutes.
	 * @return The encoded JWT token, or {@code null} on failure. */
	public static String generateJWToken(
			final Object userID,
			final String userLogin,
			final String issuer,
			final String application,
			final Map<String, Object> roles,
			final Map<String, Object> rights,
			final int timeoutInMinutes) {
		if (rsaJWK == null) {
			LOGGER.warn("JWT private key is not present !!!");
			return null;
		}
		try {
			// Create RSA-signer with the private key
			final JWSSigner signer = new RSASSASigner(rsaJWK);

			LOGGER.trace("timeoutInMinutes= {}", timeoutInMinutes);
			final Instant nowInstant = Instant.now();
			final Date now = Date.from(nowInstant);
			LOGGER.trace("now       = {}", now);
			final Date expiration = Date.from(nowInstant.plus(Duration.ofMinutes(timeoutInMinutes)));

			LOGGER.trace("expiration= {}", expiration);
			String serializeUserId = "";
			if (userID instanceof final Long userIdLong) {
				serializeUserId = Long.toString(userIdLong);
			} else if (userID instanceof final ObjectId userIdObjectId) {
				serializeUserId = userIdObjectId.toString();
			} else {
				return null;
			}
			final JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder().subject(serializeUserId)
					.claim("login", userLogin).claim("application", application).issuer(issuer).issueTime(now)
					.expirationTime(expiration);
			// add roles if needed:
			if (roles != null) {
				builder.claim("roles", roles);
			}
			// add rights if needed:
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
			LOGGER.error("Failed to generate JWT token: {}", ex.getMessage(), ex);
		}
		return null;
	}

	/**
	 * Validates a signed JWT token against the configured public key.
	 * @param signedToken The serialized JWT token to validate.
	 * @param issuer The expected issuer claim.
	 * @param application The expected application claim (currently unused).
	 * @return The validated claims set, or {@code null} if validation fails.
	 */
	public static JWTClaimsSet validateToken(final String signedToken, final String issuer, final String application) {
		try {
			// On the consumer side, parse the JWS and verify its RSA signature
			final SignedJWT signedJWT = SignedJWT.parse(signedToken);
			if (signedJWT == null) {
				LOGGER.error("FAIL to parse signing");
				return null;
			}
			if (ConfigBaseVariable.getTestMode() && signedToken.endsWith(TestSigner.TEST_SIGNATURE)) {
				LOGGER.warn("Someone use a test token: {}", signedToken);
			} else if (rsaPublicJWK == null) {
				LOGGER.warn("JWT public key is not present !!!");
				if (!ConfigBaseVariable.getTestMode()) {
					return null;
				}
				final String rawSignature = new String(signedJWT.getSigningInput(), StandardCharsets.UTF_8);
				if (rawSignature.equals(TestSigner.TEST_SIGNATURE)) {
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
				LOGGER.error("JWT token is expired now = {} with={}", new Date(),
						signedJWT.getJWTClaimsSet().getExpirationTime());
				return null;
			}
			if (!issuer.equals(signedJWT.getJWTClaimsSet().getIssuer())) {
				LOGGER.error("JWT issuer is wrong: '{}' != '{}'", issuer,
						signedJWT.getJWTClaimsSet().getIssuer());
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
			LOGGER.error("Failed to validate JWT token: {}", e.getMessage(), e);
		}
		return null;
	}

	/**
	 * Creates a JWT token for testing purposes using a dummy signer.
	 *
	 * <p>Only works when test mode is enabled via {@link ConfigBaseVariable#getTestMode()}.</p>
	 * @param userID The user identifier.
	 * @param userLogin The user login name.
	 * @param issuer The token issuer.
	 * @param application The target application name.
	 * @param roles Optional roles map to include as claims.
	 * @param rights Optional fine-grained rights map to include as claims.
	 * @return The serialized test JWT token, or {@code null} if test mode is disabled.
	 */
	public static String createJwtTestToken(
			final long userID,
			final String userLogin,
			final String issuer,
			final String application,
			final Map<String, Map<String, Object>> roles,
			final Map<String, Map<String, Object>> rights) {
		if (!ConfigBaseVariable.getTestMode()) {
			LOGGER.error("Test mode disable !!!!!");
			return null;
		}
		try {
			final int timeOutInMinutes = 3600;

			final Instant nowInstant = Instant.now();
			final Date now = Date.from(nowInstant);
			final Date expiration = Date.from(nowInstant.plus(Duration.ofMinutes(timeOutInMinutes)));

			final JWTClaimsSet.Builder builder = new JWTClaimsSet.Builder().subject(Long.toString(userID))
					.claim("login", userLogin).claim("application", application).issuer(issuer).issueTime(now)
					.expirationTime(expiration);
			// add roles if needed:
			if (roles != null && !roles.isEmpty()) {
				builder.claim("roles", roles);
			}
			// add rights if needed:
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
			LOGGER.error("Can not generate Test Token: {}", ex.getMessage(), ex);
		}
		return null;
	}
}
