package org.atriasoft.archidata.converter.jackson;

import java.io.IOException;
import java.util.Date;

import org.atriasoft.archidata.tools.DateTools;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * Jackson deserializer for {@link Date} values.
 * Parses an ISO8601 date-time string into a {@link Date} instance.
 */
public class DateDeserializer extends JsonDeserializer<Date> {

	/** Default constructor. */
	public DateDeserializer() {
		// default constructor
	}

	/**
	 * Deserializes an ISO8601 date-time string into a {@link Date}.
	 * @param p the JSON parser providing the text value.
	 * @param ctxt the deserialization context.
	 * @return the parsed {@link Date} instance.
	 * @throws IOException if an I/O error occurs during deserialization.
	 */
	@Override
	public Date deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException {
		final String value = p.getText();
		final Date ret = DateTools.parseDate(value);
		return ret;
	}
}
