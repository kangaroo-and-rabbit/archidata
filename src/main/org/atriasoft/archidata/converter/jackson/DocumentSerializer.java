package org.atriasoft.archidata.converter.jackson;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.bson.types.ObjectId;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

/**
 * Jackson serializer for BSON {@link Document} values.
 * Recursively serializes a {@link Document} and its nested structures (lists, maps, primitives)
 * into their JSON representation.
 */
public class DocumentSerializer extends JsonSerializer<Document> {

	/** Default constructor. */
	public DocumentSerializer() {
		// default constructor
	}

	/**
	 * Serializes a BSON {@link Document} into a JSON object.
	 * @param value the {@link Document} to serialize.
	 * @param gen the JSON generator used to write the output.
	 * @param serializers the serializer provider.
	 * @throws IOException if an I/O error occurs during serialization.
	 */
	@Override
	public void serialize(final Document value, final JsonGenerator gen, final SerializerProvider serializers)
			throws IOException {
		gen.writeStartObject();
		for (final Map.Entry<String, Object> entry : value.entrySet()) {
			gen.writeFieldName(entry.getKey());
			writeValue(gen, entry.getValue(), serializers);
		}
		gen.writeEndObject();
	}

	private void writeValue(final JsonGenerator gen, final Object value, final SerializerProvider serializers)
			throws IOException {
		if (value == null) {
			gen.writeNull();
		} else if (value instanceof final Document doc) {
			serialize(doc, gen, serializers);
		} else if (value instanceof final ObjectId oid) {
			gen.writeString(oid.toHexString());
		} else if (value instanceof final Date date) {
			serializers.defaultSerializeValue(date, gen);
		} else if (value instanceof final List<?> list) {
			gen.writeStartArray();
			for (final Object elem : list) {
				writeValue(gen, elem, serializers);
			}
			gen.writeEndArray();
		} else if (value instanceof final Map<?, ?> map) {
			gen.writeStartObject();
			for (final Map.Entry<?, ?> entry : map.entrySet()) {
				gen.writeFieldName(String.valueOf(entry.getKey()));
				writeValue(gen, entry.getValue(), serializers);
			}
			gen.writeEndObject();
		} else if (value instanceof final String s) {
			gen.writeString(s);
		} else if (value instanceof final Integer i) {
			gen.writeNumber(i);
		} else if (value instanceof final Long l) {
			gen.writeNumber(l);
		} else if (value instanceof final Double d) {
			gen.writeNumber(d);
		} else if (value instanceof final Float f) {
			gen.writeNumber(f);
		} else if (value instanceof final Boolean b) {
			gen.writeBoolean(b);
		} else {
			serializers.defaultSerializeValue(value, gen);
		}
	}
}
