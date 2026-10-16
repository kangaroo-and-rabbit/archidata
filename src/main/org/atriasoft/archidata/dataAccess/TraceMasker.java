package org.atriasoft.archidata.dataAccess;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.atriasoft.archidata.dataAccess.model.DbClassModel;
import org.atriasoft.archidata.dataAccess.model.DbPropertyDescriptor;
import org.atriasoft.archidata.db.BsonCodecs;
import org.bson.BsonArray;
import org.bson.BsonDocument;
import org.bson.BsonString;
import org.bson.BsonValue;
import org.bson.conversions.Bson;
import org.bson.json.JsonWriterSettings;

/**
 * Renders filters and documents for the TRACE logs of the data layer, with the values of the protected
 * fields ({@code @DataNotRead}, {@code @DataEncrypt}) replaced by {@value #MASK}.
 *
 * <p>A protected field is matched by its database name at any depth (inside {@code $and}, {@code $or},
 * an operator document, an array of documents...). Only call it under {@code LOGGER.isTraceEnabled()}:
 * the rendering costs a full JSON serialization.
 */
final class TraceMasker {
	/** Replacement written instead of a protected value. */
	static final String MASK = "***";

	private static final JsonWriterSettings INDENTED = JsonWriterSettings.builder().indent(true).build();

	private TraceMasker() {
		// Utility class
	}

	/**
	 * Returns the database names of the protected fields of a model.
	 *
	 * @param model the entity model
	 * @return the names of its {@code @DataNotRead} and {@code @DataEncrypt} fields
	 */
	static Set<String> protectedFieldNames(final DbClassModel model) {
		final Set<String> out = new HashSet<>();
		for (final DbPropertyDescriptor desc : model.getAllFields()) {
			if (desc.isNotRead() || desc.isEncrypted()) {
				out.add(desc.getDbFieldName());
			}
		}
		return out;
	}

	/**
	 * Renders a filter or a document as indented JSON, protected values masked.
	 *
	 * @param bson the filter or document
	 * @param protectedNames the database names to mask
	 * @return the JSON text
	 */
	static String toMaskedJson(final Bson bson, final Set<String> protectedNames) {
		final BsonDocument copy = BsonCodecs.toBsonDocument(bson).clone();
		mask(copy, protectedNames);
		return copy.toJson(INDENTED);
	}

	private static void mask(final BsonDocument doc, final Set<String> protectedNames) {
		for (final Map.Entry<String, BsonValue> entry : doc.entrySet()) {
			if (protectedNames.contains(entry.getKey())) {
				entry.setValue(new BsonString(MASK));
			} else {
				maskValue(entry.getValue(), protectedNames);
			}
		}
	}

	private static void maskValue(final BsonValue value, final Set<String> protectedNames) {
		if (value instanceof final BsonDocument sub) {
			mask(sub, protectedNames);
		} else if (value instanceof final BsonArray array) {
			for (final BsonValue item : array) {
				maskValue(item, protectedNames);
			}
		}
	}
}
