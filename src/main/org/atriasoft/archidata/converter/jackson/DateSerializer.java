package org.atriasoft.archidata.converter.jackson;

import java.io.IOException;
import java.util.Date;

import org.atriasoft.archidata.tools.DateTools;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * Jackson serializer for {@link Date} values.
 * Converts a {@link Date} to its ISO8601 string representation with millisecond precision in UTC.
 */
public class DateSerializer extends JsonSerializer<Date> {

	/** Default constructor. */
	public DateSerializer() {
		// default constructor
	}

	/**
	 * Serializes a {@link Date} value to its ISO8601 string representation.
	 * @param value the {@link Date} to serialize.
	 * @param gen the JSON generator used to write the output.
	 * @param serializers the serializer provider.
	 * @throws IOException if an I/O error occurs during serialization.
	 */
	@Override
	public void serialize(final Date value, final JsonGenerator gen, final SerializerProvider serializers)
			throws IOException {
		gen.writeString(DateTools.serializeMilliWithUTCTimeZone(value));
	}
}
