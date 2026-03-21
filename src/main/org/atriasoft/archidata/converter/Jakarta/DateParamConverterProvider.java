package org.atriasoft.archidata.converter.Jakarta;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Date;

import org.atriasoft.archidata.tools.DateTools;

import jakarta.annotation.Priority;
import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;

/**
 * JAX-RS parameter converter provider for {@link Date} values.
 * Enables automatic conversion of ISO8601 date-time strings to {@link Date} in JAX-RS resource parameters.
 */
@Provider
@Priority(1)
public class DateParamConverterProvider implements ParamConverterProvider {

	/** Default constructor. */
	public DateParamConverterProvider() {
		// default constructor
	}

	/**
	 * Returns a {@link ParamConverter} for the given type if it is {@link Date}.
	 * @param <T> the type to convert.
	 * @param rawType the raw class of the parameter type.
	 * @param genericType the generic type of the parameter.
	 * @param annotations the annotations associated with the parameter.
	 * @return a {@link ParamConverter} for {@link Date}, or {@code null} if the type is not supported.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <T> ParamConverter<T> getConverter(
			final Class<T> rawType,
			final Type genericType,
			final Annotation[] annotations) {
		if (rawType != Date.class) {
			return null;
		}
		return (ParamConverter<T>) new DateParamConverter();
	}

	/**
	 * Parameter converter that handles conversion between {@link Date} and its ISO8601 string representation.
	 */
	public class DateParamConverter implements ParamConverter<Date> {

		/** Default constructor. */
		public DateParamConverter() {
			// default constructor
		}

		/**
		 * Converts an ISO8601 date-time string to a {@link Date}.
		 * @param value the string value to convert.
		 * @return the parsed {@link Date}, or {@code null} if the value is {@code null} or empty.
		 * @throws IllegalArgumentException if the string is not a valid ISO8601 date format.
		 */
		@Override
		public Date fromString(final String value) {
			if (value == null || value.isEmpty()) {
				return null;
			}
			try {
				return DateTools.parseDate(value);
			} catch (final IOException e) {
				throw new IllegalArgumentException("Invalid date format. Please use ISO8601", e);
			}
		}

		/**
		 * Converts a {@link Date} to its ISO8601 string representation with millisecond precision in UTC.
		 * @param value the {@link Date} to convert.
		 * @return the ISO8601 string representation, or {@code null} if the value is {@code null}.
		 */
		@Override
		public String toString(final Date value) {
			if (value == null) {
				return null;
			}
			return DateTools.serializeMilliWithUTCTimeZone(value);
		}
	}

}
