package org.kar.archidata.converter.jackson;

import java.io.IOException;
import java.util.Date;

import org.kar.archidata.tools.DateTools;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

public class DateDeserializer extends JsonDeserializer<Date> {
	@Override
	public Date deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException {
		final String value = p.getText();
		final Date ret = DateTools.parseDate(value);
		return ret;
	}
}