package org.kar.archidata.converter.jackson;

import java.io.IOException;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class ObjectIdSerializer extends JsonSerializer<ObjectId> {
	@Override
	public void serialize(final ObjectId value, final JsonGenerator gen, final SerializerProvider serializers)
			throws IOException {
		gen.writeString(value.toHexString());
	}
}