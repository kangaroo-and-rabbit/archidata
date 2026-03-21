package org.atriasoft.archidata.converter.Jakarta;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.OffsetDateTime;

import org.atriasoft.archidata.tools.DateTools;

import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;

/**
 * JAX-RS parameter converter provider for {@link OffsetDateTime} values.
 * Enables automatic conversion of ISO8601 date-time strings to {@link OffsetDateTime} in JAX-RS resource parameters.
 */
@Provider
public class OffsetDateTimeParamConverterProvider implements ParamConverterProvider {

	/** Default constructor. */
	public OffsetDateTimeParamConverterProvider() {
		// default constructor
	}

	/**
	 * Returns a {@link ParamConverter} for the given type if it is {@link OffsetDateTime}.
	 * @param <T> the type to convert.
	 * @param rawType the raw class of the parameter type.
	 * @param genericType the generic type of the parameter.
	 * @param annotations the annotations associated with the parameter.
	 * @return a {@link ParamConverter} for {@link OffsetDateTime}, or {@code null} if the type is not supported.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T> ParamConverter<T> getConverter(
			final Class<T> rawType,
			final Type genericType,
			final Annotation[] annotations) {
		if (rawType != OffsetDateTime.class) {
			return null;
		}
		return (ParamConverter<T>) new OffsetDateTimeParamConverter();
	}

	/**
	 * Parameter converter that handles conversion between {@link OffsetDateTime} and its ISO8601 string representation.
	 */
	public class OffsetDateTimeParamConverter implements ParamConverter<OffsetDateTime> {

		/** Default constructor. */
		public OffsetDateTimeParamConverter() {
			// default constructor
		}

		/**
		 * Converts an ISO8601 date-time string to an {@link OffsetDateTime}.
		 * @param value the string value to convert.
		 * @return the parsed {@link OffsetDateTime}, or {@code null} if the value is {@code null} or empty.
		 * @throws IllegalArgumentException if the string is not a valid ISO8601 date format.
		 */
		@Override
		public OffsetDateTime fromString(final String value) {
			if (value == null || value.isEmpty()) {
				return null;
			}
			try {
				return DateTools.parseOffsetDateTime(value);
			} catch (final IOException e) {
				throw new IllegalArgumentException("Invalid date format. Please use ISO8601", e);
			}
		}

		/**
		 * Converts an {@link OffsetDateTime} to its ISO8601 string representation with millisecond precision in UTC.
		 * @param value the {@link OffsetDateTime} to convert.
		 * @return the ISO8601 string representation, or {@code null} if the value is {@code null}.
		 */
		@Override
		public String toString(final OffsetDateTime value) {
			if (value == null) {
				return null;
			}
			return DateTools.serializeMilliWithUTCTimeZone(value);
		}

	};
}
