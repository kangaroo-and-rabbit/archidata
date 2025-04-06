package org.kar.archidata.converter.jackson;

import java.io.IOException;
import java.time.OffsetDateTime;

import org.kar.archidata.tools.DateTools;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

public class OffsetDateTimeDeserializer extends JsonDeserializer<OffsetDateTime> {
	@Override
	public OffsetDateTime deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException {
		final String value = p.getText();
		final OffsetDateTime ret = DateTools.parseOffsetDateTime(value);
		return ret;
	}
}