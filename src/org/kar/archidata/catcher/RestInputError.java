package org.kar.archidata.catcher;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.persistence.Column;
import jakarta.validation.Path;
import jakarta.validation.constraints.NotNull;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class RestInputError {
	private static Pattern PATTERN = Pattern.compile("^([^.]+)\\.([^.]+)(\\.(.*))?");
	@Column(length = 0)
	public String argument;
	@Column(length = 0)
	public String path;
	@NotNull
	@Column(length = 0)
	public String message;

	@Override
	public String toString() {
		return "RestInputError [argument=" + this.argument + ", path=" + this.path + ", message=" + this.message + "]";
	}

	public RestInputError() {}

	public RestInputError(final String argument, final String path, final String message) {
		this.path = argument;
		this.path = path;
		this.message = message;
	}

	public RestInputError(final String path, final String message) {
		this.path = path;
		this.message = message;
	}

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

	String getFullPath() {
		if (this.path == null) {
			return this.argument;
		}
		return this.argument + "." + this.path;
	}
}
