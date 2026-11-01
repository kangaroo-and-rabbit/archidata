package org.atriasoft.archidata.tools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.UnaryOperator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads a secret from the file named by {@code <NAME>_FILE} (the Docker secrets convention), or from the
 * environment variable {@code <NAME>}.
 *
 * <p>The file is the recommended source: an environment variable is shown by {@code docker inspect},
 * {@code docker compose config} and {@code /proc/<pid>/environ}, where a file path reveals nothing.</p>
 *
 * <ul>
 * <li>{@code <NAME>_FILE} wins over {@code <NAME>}.</li>
 * <li>A {@code <NAME>_FILE} that cannot be read, or a file holding nothing but line breaks, is an error: starting
 * with a missing secret would only fail later, far from its cause. The error gives the name of the variable but
 * not its value: a password left there during a migration must not reach the logs.</li>
 * <li>A value in {@code <NAME>} is reported by a WARN giving the name of the variable, never its value.</li>
 * <li>With {@code SECRETS_FILE_ONLY=true}, a value in {@code <NAME>} is an error, which prevents a deployment
 * from going back to a secret in the configuration, and an empty {@code <NAME>} counts as undefined. Any value
 * of this switch other than {@code true} or {@code false} is an error, as a typo must not turn the check off.</li>
 * </ul>
 */
public final class SecretReader {
	private static final Logger LOGGER = LoggerFactory.getLogger(SecretReader.class);

	/** Suffix of the variable holding the path of the secret file. */
	public static final String FILE_SUFFIX = "_FILE";
	/** Variable that, set to {@code true}, refuses any secret given in the environment. */
	public static final String FILE_ONLY_VARIABLE = "SECRETS_FILE_ONLY";

	private final UnaryOperator<String> environment;

	/**
	 * Creates a reader on the given environment.
	 * @param environment Returns the value of a variable, or {@code null} if it is not defined.
	 */
	public SecretReader(final UnaryOperator<String> environment) {
		this.environment = Objects.requireNonNull(environment, "environment");
	}

	/**
	 * Reads a secret as text. Line breaks at the end of a file are removed, since most tools add one.
	 *
	 * @param name The base name of the variable, without the {@code _FILE} suffix.
	 * @return The secret, the value of {@code name} if no file is given (possibly empty), or {@code null} if
	 *         neither variable is defined.
	 * @throws IllegalStateException If the file cannot be read or is empty, if {@code SECRETS_FILE_ONLY} is
	 *         invalid, or if it is {@code true} and {@code name} holds a value. The message never contains a value.
	 */
	public String read(final String name) {
		final String inline = readInline(name);
		final byte[] content = readFile(name);
		if (content == null) {
			return inline;
		}
		final String secret = stripTrailingLineBreaks(new String(content, StandardCharsets.UTF_8));
		if (secret.isEmpty()) {
			throw emptyFile(name);
		}
		return secret;
	}

	/**
	 * Reads a secret as bytes. The content of a file is returned unchanged, because a binary key may end with a
	 * byte equal to a line break.
	 *
	 * @param name The base name of the variable, without the {@code _FILE} suffix.
	 * @return The secret, or {@code null} if neither variable is defined or if {@code name} is empty.
	 * @throws IllegalStateException If the file cannot be read or is empty, if {@code SECRETS_FILE_ONLY} is
	 *         invalid, or if it is {@code true} and {@code name} holds a value. The message never contains a value.
	 */
	public byte[] readBytes(final String name) {
		final String inline = readInline(name);
		final byte[] content = readFile(name);
		if (content != null) {
			// ISO-8859-1 keeps one character per byte: only the line breaks are looked at, not the key.
			if (stripTrailingLineBreaks(new String(content, StandardCharsets.ISO_8859_1)).isEmpty()) {
				throw emptyFile(name);
			}
			return content;
		}
		if (inline == null || inline.isEmpty()) {
			return null;
		}
		return inline.getBytes(StandardCharsets.UTF_8);
	}

	/**
	 * Returns the value of {@code name}. It is checked even when a file is given: a value left next to
	 * {@code _FILE} stays visible in the environment although nothing uses it.
	 */
	private String readInline(final String name) {
		final boolean fileOnly = isFileOnly();
		final String inline = this.environment.apply(name);
		if (inline == null || inline.isEmpty()) {
			return fileOnly ? null : inline;
		}
		if (fileOnly) {
			throw new IllegalStateException("Secret " + name + " is set in the environment while " + FILE_ONLY_VARIABLE
					+ " is true: use " + name + FILE_SUFFIX + " and remove " + name + ".");
		}
		LOGGER.warn("Secret {} is set in the environment, which docker inspect shows: use {}{} instead.", name, name,
				FILE_SUFFIX);
		return inline;
	}

	private boolean isFileOnly() {
		final String value = this.environment.apply(FILE_ONLY_VARIABLE);
		if (value == null || value.isEmpty() || "false".equalsIgnoreCase(value)) {
			return false;
		}
		if ("true".equalsIgnoreCase(value)) {
			return true;
		}
		throw new IllegalStateException(FILE_ONLY_VARIABLE + " must be true or false.");
	}

	/** Returns the content of the file named by {@code <name>_FILE}, or {@code null} if no file is given. */
	private byte[] readFile(final String name) {
		final String variable = name + FILE_SUFFIX;
		final String path = this.environment.apply(variable);
		if (path == null || path.isEmpty()) {
			return null;
		}
		try {
			return Files.readAllBytes(Path.of(path));
		} catch (final IOException | RuntimeException ex) {
			// Neither the path nor the cause, which repeats it: this variable may hold a password by mistake.
			throw new IllegalStateException(
					"Cannot read the secret file given by " + variable + " (" + ex.getClass().getSimpleName() + ").");
		}
	}

	private static IllegalStateException emptyFile(final String name) {
		return new IllegalStateException("The secret file given by " + name + FILE_SUFFIX + " is empty.");
	}

	private static String stripTrailingLineBreaks(final String value) {
		int end = value.length();
		while (end > 0 && (value.charAt(end - 1) == '\n' || value.charAt(end - 1) == '\r')) {
			end--;
		}
		return value.substring(0, end);
	}
}
