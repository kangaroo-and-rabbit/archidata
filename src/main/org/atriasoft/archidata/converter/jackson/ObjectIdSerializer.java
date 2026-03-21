package org.atriasoft.archidata.converter.jackson;

import java.io.IOException;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * Jackson serializer for MongoDB {@link ObjectId} values.
 * Converts an {@link ObjectId} to its hexadecimal string representation.
 */
public class ObjectIdSerializer extends JsonSerializer<ObjectId> {

	/** Default constructor. */
	public ObjectIdSerializer() {
		// default constructor
	}

	/**
	 * Serializes an {@link ObjectId} to its hexadecimal string representation.
	 * @param value the {@link ObjectId} to serialize.
	 * @param gen the JSON generator used to write the output.
	 * @param serializers the serializer provider.
	 * @throws IOException if an I/O error occurs during serialization.
	 */
	@Override
	public void serialize(final ObjectId value, final JsonGenerator gen, final SerializerProvider serializers)
			throws IOException {
		gen.writeString(value.toHexString());
	}
}
