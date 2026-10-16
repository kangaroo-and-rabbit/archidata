package org.atriasoft.archidata.tools;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.atriasoft.archidata.filter.AuthenticationFilter;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

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

	/** Written by the key refresh thread, read by every request thread. */
	private static volatile RSAKey rsaJWK = null;
	private static volatile RSAKey rsaPublicJWK = null;
	/** Connection timeout when fetching the SSO public key. */
	public static final int SSO_CONNECT_TIMEOUT_MS = 5_000;
	/** Read timeout when fetching the SSO public key. */
	public static final int SSO_READ_TIMEOUT_MS = 10_000;
	/** Tolerance on the {@code nbf} claim for clocks that are not exactly in sync, in milliseconds. */
	public static final long MAX_CLOCK_SKEW_MS = 30_000L;
	/** Smallest RSA key accepted from the SSO, in bits. */
	public static final int MIN_RSA_KEY_SIZE = 2048;
	/** Largest answer accepted from the SSO public key endpoint. */
	private static final int MAX_PUBLIC_KEY_ANSWER = 64 * 1024;
	/** Shortest delay between two refreshes asked by an unknown token signature. */
	public static final Duration MIN_REFRESH_INTERVAL = Duration.ofSeconds(60);
	/** When the last refresh was asked by {@link #validateToken} (nanoTime), {@code Long.MIN_VALUE} when never. */
	private static final AtomicLong LAST_REFRESH_REQUEST = new AtomicLong(Long.MIN_VALUE);
	/** Accept an {@code http://} SSO address that is not a loopback one. */
	private static volatile boolean allowInsecureSsoAddress = false;
	/**
	 * Optional hook invoked when {@link #validateToken} detects an invalid signature.
	 * Set by {@code UpdateJwtPublicKey} to trigger an immediate re-fetch of the SSO public key
	 * (the SSO may have been restarted and rotated its key).
	 */
	private static final AtomicReference<Runnable> REFRESH_TRIGGER = new AtomicReference<>();

	/**
	 * Registers a hook called when a JWT signature fails validation.
	 * @param trigger A no-arg callback (typically interrupts the refresh thread). May be {@code null} to clear.
	 */
	public static void setRefreshTrigger(final Runnable trigger) {
		REFRESH_TRIGGER.set(trigger);
		LAST_REFRESH_REQUEST.set(Long.MIN_VALUE);
	}

	/**
	 * Allows an {@code http://} SSO address that is not a loopback one. By default
	 * {@link #initLocalTokenRemote(String, String)} only accepts {@code https://}, or {@code http://} to
	 * {@code localhost} / a loopback address: over plain HTTP, anyone on the path can read {@code SSO_TOKEN} and
	 * replace the public key with their own (and then forge any token).
	 * @param allow {@code true} to accept plain HTTP (a private network you trust, for example).
	 */
	public static void setAllowInsecureSsoAddress(final boolean allow) {
		allowInsecureSsoAddress = allow;
	}

	/**
	 * Asks the refresh thread to fetch the public key now, at most once per {@link #MIN_REFRESH_INTERVAL}: an
	 * anonymous client sending tokens with a bad signature must not make the server hammer the SSO.
	 */
	private static void requestPublicKeyRefresh() {
		final long now = System.nanoTime();
		final long last = LAST_REFRESH_REQUEST.get();
		if (last != Long.MIN_VALUE && now - last < MIN_REFRESH_INTERVAL.toNanos()) {
			return;
		}
		if (!LAST_REFRESH_REQUEST.compareAndSet(last, now)) {
			return;
		}
		final Runnable trigger = REFRESH_TRIGGER.get();
		if (trigger != null) {
			try {
				trigger.run();
			} catch (final RuntimeException e) {
				LOGGER.warn("Refresh trigger threw: {}", e.getMessage(), e);
			}
		}
	}

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
	 * Tells whether the SSO public key may be fetched from this URL: {@code https}, or {@code http} to a
	 * loopback host, or any {@code http} when {@link #setAllowInsecureSsoAddress(boolean)} allows it.
	 * @param uri The URL of the public key.
	 * @return {@code true} when the URL may be used.
	 */
	static boolean isAcceptableSsoUri(final URI uri) {
		final String scheme = uri.getScheme();
		if ("https".equalsIgnoreCase(scheme)) {
			return true;
		}
		if (!"http".equalsIgnoreCase(scheme)) {
			return false;
		}
		if (allowInsecureSsoAddress) {
			return true;
		}
		final String host = uri.getHost();
		if (host == null) {
			return false;
		}
		final String bare = host.startsWith("[") ? host.substring(1, host.length() - 1) : host;
		return "localhost".equalsIgnoreCase(bare) || bare.startsWith("127.") || "::1".equals(bare)
				|| "0:0:0:0:0:0:0:1".equals(bare);
	}

	/**
	 * Initializes the JWT public key by fetching it from a remote SSO server.
	 * <p>
	 * The address must be {@code https://} (or {@code http://} to a loopback host, see
	 * {@link #setAllowInsecureSsoAddress(boolean)}); the connection and the read time out
	 * ({@value #SSO_CONNECT_TIMEOUT_MS} ms, {@value #SSO_READ_TIMEOUT_MS} ms); redirections are not followed;
	 * the answer is bounded and the key must be an RSA key of at least {@value #MIN_RSA_KEY_SIZE} bits.
	 * {@code SSO_TOKEN} is sent in the {@code ApiKey} header and never logged.
	 * </p>
	 * @param ssoUri The base URI of the SSO service.
	 * @param application The application name sent as User-Agent.
	 * @throws IOException If the address is refused, the HTTP request fails or the key is too weak.
	 * @throws ParseException If the public key cannot be parsed.
	 */
	public static void initLocalTokenRemote(final String ssoUri, final String application)
			throws IOException, ParseException {
		final URI uri;
		try {
			uri = new URI(ssoUri + "public_key");
		} catch (final URISyntaxException ex) {
			throw new IOException("Invalid SSO address", ex);
		}
		if (!isAcceptableSsoUri(uri)) {
			throw new IOException("The SSO address must use https (http is only accepted for a loopback host): "
					+ uri.getScheme() + "://" + uri.getHost());
		}
		final HttpURLConnection con = (HttpURLConnection) uri.toURL().openConnection();
		try {
			con.setConnectTimeout(SSO_CONNECT_TIMEOUT_MS);
			con.setReadTimeout(SSO_READ_TIMEOUT_MS);
			// A redirection would send SSO_TOKEN to another host: never follow it.
			con.setInstanceFollowRedirects(false);
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
			if (responseCode != HttpURLConnection.HTTP_OK) {
				LOGGER.debug("GET JWT validator token not worked response code {} from {}://{}", responseCode,
						uri.getScheme(), uri.getHost());
				return;
			}
			final byte[] body;
			try (InputStream in = con.getInputStream()) {
				body = in.readNBytes(MAX_PUBLIC_KEY_ANSWER + 1);
			}
			if (body.length > MAX_PUBLIC_KEY_ANSWER) {
				throw new IOException("SSO public key answer is too large");
			}
			// Do not commit security fail: never log the answer nor the token.
			final ObjectMapper mapper = ContextGenericTools.createObjectMapper();
			final PublicKey values = mapper.readValue(new String(body, StandardCharsets.UTF_8), PublicKey.class);
			final RSAKey key = RSAKey.parse(values.key);
			if (key.size() < MIN_RSA_KEY_SIZE) {
				throw new IOException("SSO public key is too weak: " + key.size() + " bits");
			}
			final RSAKey previous = rsaPublicJWK;
			rsaPublicJWK = key;
			if (previous == null || !previous.equals(key)) {
				LOGGER.info("JWT public key loaded from the SSO, thumbprint {}", thumbprintOf(key));
			}
		} finally {
			con.disconnect();
		}
	}

	private static String thumbprintOf(final RSAKey key) {
		try {
			return key.computeThumbprint().toString();
		} catch (final JOSEException ex) {
			return "?";
		}
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
	 * <p>
	 * The token carries the standard {@code aud} claim set to {@code application}, in addition to the
	 * {@code application} claim, so that only that application accepts it.
	 * </p>
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
			// Standard audience claim: the application the token is meant for (checked by AuthenticationFilter).
			if (application != null) {
				builder.audience(application);
			}
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
	 * Tells whether a token is meant for an application.
	 * <p>
	 * A token with an {@code aud} claim must list the application in it. A token without {@code aud} (issued
	 * before the audience was written) must carry the application in its {@code application} claim.
	 * </p>
	 * @param claims The claims of the token.
	 * @param application The name of the application validating the token.
	 * @return {@code true} when the token is meant for this application.
	 */
	public static boolean isForApplication(final JWTClaimsSet claims, final String application) {
		final List<String> audience = claims.getAudience();
		if (audience != null && !audience.isEmpty()) {
			if (audience.contains(application)) {
				return true;
			}
			LOGGER.warn("JWT audience mismatch: expected='{}' got={}", application, audience);
			return false;
		}
		final Object tokenApplication = claims.getClaim("application");
		if (application.equals(tokenApplication)) {
			return true;
		}
		LOGGER.warn("JWT application mismatch (no audience): expected='{}' got='{}'", application, tokenApplication);
		return false;
	}

	/**
	 * Validates a signed JWT token against the configured public key.
	 * <p>
	 * Checks, in order: the RS256/PS signature, the expiration ({@code exp} is mandatory, a token without it is
	 * refused), {@code nbf} when present (with {@value #MAX_CLOCK_SKEW_MS} ms of tolerance), the issuer and,
	 * when {@code application} is given, the audience.
	 * </p>
	 * @param signedToken The serialized JWT token to validate.
	 * @param issuer The expected issuer claim.
	 * @param application The application the token must be meant for (see
	 *                    {@link #isForApplication(JWTClaimsSet, String)}), or {@code null} to skip this check.
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
			final RSAKey publicKey = rsaPublicJWK;
			if (publicKey == null) {
				LOGGER.error("JWT public key is not present !!!");
				requestPublicKeyRefresh();
				return null;
			}
			final JWSVerifier verifier = new RSASSAVerifier(publicKey);
			if (!signedJWT.verify(verifier)) {
				LOGGER.warn("JWT token is NOT verified");
				// SSO may have rotated its key (typically after a restart). Ask the refresh
				// thread to fetch the new public key now instead of waiting for the next tick.
				requestPublicKeyRefresh();
				return null;
			}
			final Date now = new Date();
			final Date expiration = signedJWT.getJWTClaimsSet().getExpirationTime();
			if (expiration == null) {
				// A token without expiration would be valid forever: refuse it.
				LOGGER.warn("JWT token has no expiration time (exp)");
				return null;
			}
			if (!now.before(expiration)) {
				LOGGER.warn("JWT token is expired now = {} with={}", now, expiration);
				return null;
			}
			final Date notBefore = signedJWT.getJWTClaimsSet().getNotBeforeTime();
			if (notBefore != null && notBefore.getTime() > now.getTime() + MAX_CLOCK_SKEW_MS) {
				LOGGER.warn("JWT token is not valid yet now = {} nbf={}", now, notBefore);
				return null;
			}
			if (!issuer.equals(signedJWT.getJWTClaimsSet().getIssuer())) {
				LOGGER.warn("JWT issuer is wrong: '{}' != '{}'", issuer, signedJWT.getJWTClaimsSet().getIssuer());
				return null;
			}
			if (application != null && !isForApplication(signedJWT.getJWTClaimsSet(), application)) {
				return null;
			}
			// the element must be validated outside ...
			// LOGGER.debug("JWT token is verified 'alice' =?= '" + signedJWT.getJWTClaimsSet().getSubject() + "'");
			// LOGGER.debug("JWT token isuer 'https://c2id.com' =?= '" + signedJWT.getJWTClaimsSet().getIssuer() + "'");
			return signedJWT.getJWTClaimsSet();
		} catch (final JOSEException | ParseException e) {
			// A malformed token is the caller's problem: no stack trace, which would flood the log.
			LOGGER.warn("Failed to validate JWT token: {}", e.getMessage());
		}
		return null;
	}

}
