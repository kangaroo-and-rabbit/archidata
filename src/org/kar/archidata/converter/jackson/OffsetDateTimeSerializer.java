package org.kar.archidata.converter.jackson;

import java.io.IOException;
import java.time.OffsetDateTime;

import org.kar.archidata.tools.DateTools;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class OffsetDateTimeSerializer extends JsonSerializer<OffsetDateTime> {
	@Override
	public void serialize(final OffsetDateTime value, final JsonGenerator gen, final SerializerProvider serializers)
			throws IOException {
		gen.writeString(DateTools.serializeMilliWithUTCTimeZone(value));
	}
}