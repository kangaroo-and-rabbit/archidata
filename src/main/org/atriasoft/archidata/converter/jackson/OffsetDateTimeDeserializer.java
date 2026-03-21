package org.atriasoft.archidata.converter.jackson;

import java.io.IOException;
import java.time.OffsetDateTime;

import org.atriasoft.archidata.tools.DateTools;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * Jackson deserializer for {@link OffsetDateTime} values.
 * Parses an ISO8601 date-time string into an {@link OffsetDateTime} instance.
 */
public class OffsetDateTimeDeserializer extends JsonDeserializer<OffsetDateTime> {

	/** Default constructor. */
	public OffsetDateTimeDeserializer() {
		// default constructor
	}

	/**
	 * Deserializes an ISO8601 date-time string into an {@link OffsetDateTime}.
	 * @param p the JSON parser providing the text value.
	 * @param ctxt the deserialization context.
	 * @return the parsed {@link OffsetDateTime} instance.
	 * @throws IOException if an I/O error occurs during deserialization.
	 */
	@Override
	public OffsetDateTime deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException {
		final String value = p.getText();
		final OffsetDateTime ret = DateTools.parseOffsetDateTime(value);
		return ret;
	}
}
