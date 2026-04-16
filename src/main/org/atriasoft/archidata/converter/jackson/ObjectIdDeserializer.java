package org.atriasoft.archidata.converter.jackson;

import java.io.IOException;

import org.bson.types.ObjectId;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

/**
 * Jackson deserializer for MongoDB {@link ObjectId} values.
 * Parses a hexadecimal string into an {@link ObjectId} instance.
 */
public class ObjectIdDeserializer extends JsonDeserializer<ObjectId> {

	/** Default constructor. */
	public ObjectIdDeserializer() {
		// default constructor
	}

	/**
	 * Deserializes a hexadecimal string into an {@link ObjectId}.
	 * @param p the JSON parser providing the string value.
	 * @param ctxt the deserialization context.
	 * @return the parsed {@link ObjectId} instance.
	 * @throws IOException if an I/O error occurs during deserialization.
	 */
	@Override
	public ObjectId deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException {
		return new ObjectId(p.getValueAsString());
	}
}
