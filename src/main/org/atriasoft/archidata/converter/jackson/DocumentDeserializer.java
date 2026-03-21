package org.atriasoft.archidata.converter.jackson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bson.Document;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Jackson deserializer for BSON {@link Document} values.
 * Parses a JSON object into a {@link Document}, recursively converting nested objects and arrays.
 */
public class DocumentDeserializer extends JsonDeserializer<Document> {

	/** Default constructor. */
	public DocumentDeserializer() {
		// default constructor
	}

	/**
	 * Deserializes a JSON object into a BSON {@link Document}.
	 * @param p the JSON parser providing the token stream.
	 * @param ctxt the deserialization context.
	 * @return the deserialized {@link Document} instance.
	 * @throws IOException if an I/O error occurs or the input is not a JSON object.
	 */
	@Override
	public Document deserialize(final JsonParser p, final DeserializationContext ctxt) throws IOException {
		final JsonNode node = p.readValueAsTree();
		if (node instanceof final ObjectNode objectNode) {
			return convertObjectNode(objectNode);
		}
		throw ctxt.wrongTokenException(p, Document.class, p.currentToken(), "Expected JSON object for Document");
	}

	private Document convertObjectNode(final ObjectNode node) {
		final Document doc = new Document();
		final Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
		while (fields.hasNext()) {
			final Map.Entry<String, JsonNode> entry = fields.next();
			doc.put(entry.getKey(), convertNode(entry.getValue()));
		}
		return doc;
	}

	private Object convertNode(final JsonNode node) {
		if (node == null || node.isNull()) {
			return null;
		}
		if (node.isObject()) {
			return convertObjectNode((ObjectNode) node);
		}
		if (node.isArray()) {
			final List<Object> list = new ArrayList<>();
			final ArrayNode arrayNode = (ArrayNode) node;
			for (final JsonNode elem : arrayNode) {
				list.add(convertNode(elem));
			}
			return list;
		}
		if (node.isTextual()) {
			return node.textValue();
		}
		if (node.isInt()) {
			return node.intValue();
		}
		if (node.isLong()) {
			return node.longValue();
		}
		if (node.isDouble() || node.isFloat()) {
			return node.doubleValue();
		}
		if (node.isBoolean()) {
			return node.booleanValue();
		}
		return node.asText();
	}
}
