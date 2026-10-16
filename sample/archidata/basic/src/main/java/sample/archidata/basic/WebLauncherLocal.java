package sample.archidata.basic;

import java.util.Map;

import org.atriasoft.archidata.filter.PartRight;
import org.atriasoft.archidata.tools.ConfigBaseVariable;
import org.atriasoft.archidata.tools.JWTWrapper;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sample.archidata.basic.filter.SampleAuthenticationFilter;

/**
 * Development entry point: hard-coded local configuration and a self-signed JWT, no SSO needed.
 *
 * <p>Prints two tokens to use with curl ({@code Authorization: Bearer <token>}): one with the USER
 * role, one with the ADMIN role.
 *
 * <p><b>Never deploy this launcher</b>: it uses the tutorial database password, signs its own tokens and
 * writes a valid ADMIN token in the log. It refuses to start when the API address is not a loopback one.
 */
public class WebLauncherLocal extends WebLauncher {
	private static final Logger LOGGER = LoggerFactory.getLogger(WebLauncherLocal.class);

	public WebLauncherLocal() {
		// Must match the MongoDB started for the tutorial (doc/TUTORIAL_01_first_install.md).
		ConfigBaseVariable.setApiAddress("http://127.0.0.1:9000/sample/api/");
		ConfigBaseVariable.setDbPassword("base_db_password");
		ConfigBaseVariable.setBdDatabase("sample_basic");
		// The front of the tutorial runs on the dev server of Vite/Angular: only it may call the API from a
		// browser (credentials allowed). Other origins get no CORS header.
		ConfigBaseVariable.setCorsAllowedOrigins("http://localhost:4200,http://127.0.0.1:4200");
	}

	/**
	 * Tells whether the address the server listens on is a loopback one.
	 * @return {@code true} for localhost / 127.x / ::1.
	 */
	public static boolean listensOnLoopback() {
		final String host = java.net.URI.create(ConfigBaseVariable.getLocalAddress()).getHost();
		return host != null && ("localhost".equals(host) || host.startsWith("127.") || "[::1]".equals(host));
	}

	/** The key is generated locally: do not let the SSO key updater replace it. */
	@Override
	protected void startJwtKeyUpdater() {
		// nothing
	}

	/**
	 * Create a token signed by the local key.
	 * @param login login written in the token.
	 * @param role role granted in the "sample" group (READ_WRITE).
	 * @return the serialized JWT.
	 */
	public static String createToken(final String login, final String role) {
		// The subject must be an ObjectId: AuthenticationFilter parses it as such.
		// Role values are PartRight codes (int); a Boolean is rejected when the token is read.
		final Map<String, Object> roles = Map.of(SampleAuthenticationFilter.APPLICATION_NAME,
				Map.of(role, PartRight.READ_WRITE.getValue()));
		// The issuer must be the one expected by the filter ("Karso" by default). Validity: 1 day.
		return JWTWrapper.generateJWToken(new ObjectId(), login, "Karso", SampleAuthenticationFilter.APPLICATION_NAME,
				roles, null, 24 * 60);
	}

	public static void main(final String[] args) throws Exception {
		installLogBridge();
		final WebLauncherLocal launcher = new WebLauncherLocal();
		if (!listensOnLoopback()) {
			LOGGER.error("WebLauncherLocal is a development launcher: it only listens on a loopback address");
			return;
		}
		// Generate a local RSA key pair: the same process signs and checks the tokens.
		JWTWrapper.initLocalToken(null);
		LOGGER.info("USER  token: {}", createToken("user", "USER"));
		LOGGER.info("ADMIN token: {}", createToken("admin", "ADMIN"));
		launcher.migrateDB();
		launcher.synchronizeIndexes();
		launcher.registerShutdownHook();
		launcher.process();
		Thread.currentThread().join();
	}
}
