package org.atriasoft.archidata.catcher;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.persistence.Column;
import jakarta.validation.Path;
import jakarta.validation.constraints.NotNull;

/**
 * Represents a single input validation error in a REST response.
 * Contains details about the argument name, property path, and validation error message.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RestInputError {
	private static Pattern PATTERN = Pattern.compile("^([^.]+)\\.([^.]+)(\\.(.*))?");
	/** The argument name that caused the validation error. */
	@Column(length = 0)
	public String argument;
	/** The property path within the argument that caused the validation error. */
	@Column(length = 0)
	public String path;
	/** The validation error message describing what went wrong. */
	@NotNull
	@Column(length = 0)
	public String message;

	@Override
	public String toString() {
		return "RestInputError [argument=" + this.argument + ", path=" + this.path + ", message=" + this.message + "]";
	}

	/**
	 * Default constructor for deserialization.
	 */
	public RestInputError() {}

	/**
	 * Constructs an input error with argument, path, and message.
	 * @param argument the argument name that caused the error
	 * @param path the property path within the argument
	 * @param message the validation error message
	 */
	public RestInputError(final String argument, final String path, final String message) {
		this.argument = argument;
		this.path = path;
		this.message = message;
	}

	/**
	 * Constructs an input error with path and message.
	 * @param path the property path that caused the error
	 * @param message the validation error message
	 */
	public RestInputError(final String path, final String message) {
		this.path = path;
		this.message = message;
	}

	/**
	 * Constructs an input error from a Jakarta Validation property path and message.
	 * Parses the path to extract the argument name and nested property path.
	 * @param path the Jakarta Validation property path
	 * @param message the validation error message
	 */
	public RestInputError(final Path path, final String message) {
		final Matcher matcher = PATTERN.matcher(path.toString());
		if (matcher.find()) {
			//String firstPart = matcher.group(1); this is the request base element ==> not needed
			this.argument = matcher.group(2);
			this.path = matcher.group(4);
		} else {
			this.path = path.toString();
		}
		this.message = message;
	}

	/**
	 * Returns the full qualified path by combining argument and path with a dot separator.
	 * @return the full path string, or just the argument if path is null
	 */
	String getFullPath() {
		if (this.path == null) {
			return this.argument;
		}
		return this.argument + "." + this.path;
	}
}
