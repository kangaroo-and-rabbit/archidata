package org.kar.archidata.converter.Jakarta;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.time.OffsetDateTime;

import org.kar.archidata.tools.DateTools;

import jakarta.ws.rs.ext.ParamConverter;
import jakarta.ws.rs.ext.ParamConverterProvider;
import jakarta.ws.rs.ext.Provider;

@Provider
public class OffsetDateTimeParamConverter implements ParamConverterProvider {

	@SuppressWarnings("unchecked")
	@Override
	public <T> ParamConverter<T> getConverter(
			final Class<T> rawType,
			final Type genericType,
			final Annotation[] annotations) {
		if (rawType != OffsetDateTime.class) {
			return null;
		}
		return (ParamConverter<T>) new ParamConverter<OffsetDateTime>() {
			@Override
			public OffsetDateTime fromString(final String value) {
				if (value == null || value.isEmpty()) {
					return null;
				}
				System.out.println(value);
				try {
					return DateTools.parseOffsetDateTime(value);
				} catch (final IOException e) {
					throw new IllegalArgumentException("Invalid date format. Please use ISO8601", e);
				}
			}

			@Override
			public String toString(final OffsetDateTime value) {
				if (value == null) {
					return null;
				}
				return DateTools.serializeMilliWithUTCTimeZone(value);
			}

		};
	}
}
