package org.atriasoft.archidata.converter.jackson;

import java.io.IOException;
import java.time.OffsetDateTime;

import org.atriasoft.archidata.tools.DateTools;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * Jackson serializer for {@link OffsetDateTime} values.
 * Converts an {@link OffsetDateTime} to its ISO8601 string representation with millisecond precision in UTC.
 */
public class OffsetDateTimeSerializer extends JsonSerializer<OffsetDateTime> {

	/** Default constructor. */
	public OffsetDateTimeSerializer() {
		// default constructor
	}

	/**
	 * Serializes an {@link OffsetDateTime} value to its ISO8601 string representation.
	 * @param value the {@link OffsetDateTime} to serialize.
	 * @param gen the JSON generator used to write the output.
	 * @param serializers the serializer provider.
	 * @throws IOException if an I/O error occurs during serialization.
	 */
	@Override
	public void serialize(final OffsetDateTime value, final JsonGenerator gen, final SerializerProvider serializers)
			throws IOException {
		gen.writeString(DateTools.serializeMilliWithUTCTimeZone(value));
	}
}
