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

@Provider
@Priority(1)
public class DateParamConverterProvider implements ParamConverterProvider {

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

	public class DateParamConverter implements ParamConverter<Date> {
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

		@Override
		public String toString(final Date value) {
			if (value == null) {
				return null;
			}
			return DateTools.serializeMilliWithUTCTimeZone(value);
		}
	}

}
