package org.atriasoft.archidata.crypto;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;

import org.atriasoft.archidata.tools.ConfigBaseVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Global registry mapping logical key names to {@link CipherProvider} instances.
 *
 * <p>The store is lazily populated from the configuration (see
 * {@link ConfigBaseVariable}) on first access: a base key named {@value #DEFAULT_KEY_NAME} and any
 * additional named keys found in the configured keys directory. Keys can also be
 * {@linkplain #register(String, CipherProvider) registered at runtime}, which is the mechanism used
 * to inject organisation- or sub-domain-specific keys.</p>
 *
 * <p>Thread-safe.</p>
 */
public final class EncryptionKeyStore {

	private static final Logger LOGGER = LoggerFactory.getLogger(EncryptionKeyStore.class);

	/** Logical name of the base key used when {@code @DataEncrypt} carries no explicit key. */
	public static final String DEFAULT_KEY_NAME = "default";

	private static final ConcurrentHashMap<String, CipherProvider> PROVIDERS = new ConcurrentHashMap<>();
	private static volatile boolean initialized = false;

	private EncryptionKeyStore() {
		// Utility class.
	}

	/**
	 * Registers (or replaces) a named key provider. Intended for runtime injection of
	 * organisation/sub-domain specific keys.
	 *
	 * <p>Once the configuration is locked ({@link ConfigBaseVariable#lock()}), a new name can still be
	 * registered, but an existing key (the {@value #DEFAULT_KEY_NAME} one included) can no longer be replaced:
	 * a hot swap would make the data encrypted with it unreadable, or encrypt new data with a key chosen by
	 * whoever calls this method. The check and the registration are atomic.</p>
	 *
	 * @param name the logical key name referenced by {@code @DataEncrypt}.
	 * @param provider the cipher provider.
	 * @throws IllegalStateException if the configuration is locked and {@code name} is already registered.
	 */
	public static void register(final String name, final CipherProvider provider) {
		ensureInitialized();
		synchronized (EncryptionKeyStore.class) {
			if (ConfigBaseVariable.isLocked() && PROVIDERS.containsKey(name)) {
				throw new IllegalStateException(
						"Configuration is locked: the encryption key '" + name + "' cannot be replaced");
			}
			PROVIDERS.put(name, provider);
		}
	}

	/**
	 * Returns the provider registered under a logical name.
	 *
	 * @param name the logical key name.
	 * @return the provider, or {@code null} if none is registered.
	 */
	public static CipherProvider get(final String name) {
		ensureInitialized();
		return PROVIDERS.get(name);
	}

	/**
	 * Indicates whether a provider is registered under a logical name.
	 *
	 * @param name the logical key name.
	 * @return {@code true} if a provider exists.
	 */
	public static boolean contains(final String name) {
		ensureInitialized();
		return PROVIDERS.containsKey(name);
	}

	/**
	 * Clears all registered keys and resets the lazy-initialisation flag.
	 * Mainly used by tests between runs.
	 */
	public static void reset() {
		synchronized (EncryptionKeyStore.class) {
			PROVIDERS.clear();
			initialized = false;
		}
	}

	private static void ensureInitialized() {
		if (initialized) {
			return;
		}
		synchronized (EncryptionKeyStore.class) {
			if (initialized) {
				return;
			}
			loadFromConfig();
			initialized = true;
		}
	}

	/**
	 * Loads keys from the configuration. Existing (manually registered) keys are not overridden.
	 */
	private static void loadFromConfig() {
		final byte[] defaultKey = ConfigBaseVariable.getEncryptDefaultKey();
		if (defaultKey != null) {
			try {
				PROVIDERS.putIfAbsent(DEFAULT_KEY_NAME, KeyMaterialLoader.fromBytes(defaultKey));
				LOGGER.info("Loaded base encryption key '{}'", DEFAULT_KEY_NAME);
			} catch (final Exception e) {
				LOGGER.error("Failed to load base encryption key", e);
			}
		}
		final String keysDir = ConfigBaseVariable.getEncryptKeysDir();
		if (keysDir != null && !keysDir.isEmpty()) {
			loadKeysDirectory(Path.of(keysDir));
		}
	}

	private static void loadKeysDirectory(final Path dir) {
		if (!Files.isDirectory(dir)) {
			LOGGER.warn("Encryption keys directory does not exist: {}", dir);
			return;
		}
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
			for (final Path file : stream) {
				if (!Files.isRegularFile(file)) {
					continue;
				}
				final String name = stripExtension(file.getFileName().toString());
				try {
					PROVIDERS.putIfAbsent(name, KeyMaterialLoader.fromBytes(Files.readAllBytes(file)));
					LOGGER.info("Loaded encryption key '{}'", name);
				} catch (final Exception e) {
					LOGGER.error("Failed to load encryption key from {}", file, e);
				}
			}
		} catch (final IOException e) {
			LOGGER.error("Failed to scan encryption keys directory {}", dir, e);
		}
	}

	private static String stripExtension(final String fileName) {
		final int dot = fileName.lastIndexOf('.');
		return dot > 0 ? fileName.substring(0, dot) : fileName;
	}
}
