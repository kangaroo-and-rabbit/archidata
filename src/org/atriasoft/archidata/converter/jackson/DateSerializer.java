package org.atriasoft.archidata.converter.jackson;

import java.io.IOException;
import java.util.Date;

import org.atriasoft.archidata.tools.DateTools;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class DateSerializer extends JsonSerializer<Date> {
	@Override
	public void serialize(final Date value, final JsonGenerator gen, final SerializerProvider serializers)
			throws IOException {
		gen.writeString(DateTools.serializeMilliWithUTCTimeZone(value));
	}
}
