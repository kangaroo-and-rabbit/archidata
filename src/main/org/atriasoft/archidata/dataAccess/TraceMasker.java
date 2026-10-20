package org.atriasoft.archidata.dataAccess;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.atriasoft.archidata.bean.exception.IntrospectionException;
import org.atriasoft.archidata.dataAccess.model.DbClassModel;
import org.atriasoft.archidata.dataAccess.model.DbPropertyDescriptor;
import org.atriasoft.archidata.dataAccess.model.FieldPathCursor;
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
 * <p>A protected field is masked <strong>by its position</strong> in the document, not by its name: the
 * document is walked together with the model of the entity ({@link FieldPathCursor}), so a protected field
 * of an embedded object, of the elements of a list or a set, or of the values of a map is masked where it
 * stands. A dotted filter key that designates a protected path ({@code credentials.passwordHash},
 * {@code history.0.passwordHash}, {@code map.someKey.secret}) is masked too. The structure of the filter
 * ({@code $and}, {@code $or}, {@code $elemMatch}, operator documents, arrays) is followed. A column renamed
 * by {@code OptionRenameColumn} is masked under its new name. A position that cannot be resolved with
 * certainty (too deep, ambiguous) is masked, and so is the whole value of a filter operator that does not
 * apply to the position ({@code $expr}, {@code $where}, {@code $jsonSchema}, {@code $text}...).
 *
 * <p>Only call it under {@code LOGGER.isTraceEnabled()}: the rendering costs a full JSON serialization.
 */
final class TraceMasker {
	/** Replacement written instead of a protected value. */
	static final String MASK = "***";

	/** Above this number of simultaneous readings of a key (index or key, dotted path), mask the value. */
	private static final int MAX_POSITIONS = 16;

	/**
	 * Filter operators whose value applies to the same position (a sub-filter, a compared value, a list of
	 * them). Any other key starting with {@code $} in a filter is read as a map key only; when it is not one,
	 * its whole value is masked ({@code $expr}, {@code $where}, {@code $function}, {@code $jsonSchema},
	 * {@code $text}... can name any field).
	 */
	private static final Set<String> POSITION_OPERATORS = Set.of("$and", "$or", "$nor", "$not", "$eq", "$ne", "$gt",
			"$gte", "$lt", "$lte", "$in", "$nin", "$exists", "$type", "$all", "$elemMatch", "$size", "$regex",
			"$options", "$mod", "$bitsAllClear", "$bitsAllSet", "$bitsAnyClear", "$bitsAnySet");

	private static final JsonWriterSettings INDENTED = JsonWriterSettings.builder().indent(true).build();

	private TraceMasker() {
		// Utility class
	}

	/**
	 * Renders a filter or a document as indented JSON, protected values masked.
	 *
	 * @param bson the filter or document
	 * @param clazz the entity class the filter or the document belongs to
	 * @param options the query options (resolves the renamed columns), may be {@code null}
	 * @param filter {@code true} for a filter (keys starting with {@code $} may be operators, dotted keys are
	 *            paths), {@code false} for a document read from the database (every key is a field name or a
	 *            map key, even one starting with {@code $})
	 * @return the JSON text
	 */
	static String toMaskedJson(
			final Bson bson,
			final Class<?> clazz,
			final QueryOptions options,
			final boolean filter) {
		final BsonDocument copy = BsonCodecs.toBsonDocument(bson).clone();
		final FieldPathCursor root;
		final Map<String, String> renames = new HashMap<>();
		try {
			root = FieldPathCursor.of(clazz);
			for (final DbPropertyDescriptor desc : DbClassModel.of(clazz).getAllFields()) {
				final String inTable = desc.getFieldName(options).inTable();
				if (!inTable.equals(desc.getDbFieldName())) {
					renames.put(inTable, desc.getDbFieldName());
				}
			}
		} catch (final IntrospectionException ex) {
			// Fail closed: without the model nothing can be told apart.
			return "\"" + MASK + " (model of " + clazz.getSimpleName() + " not available)\"";
		}
		mask(copy, List.of(root), renames, filter);
		return copy.toJson(INDENTED);
	}

	private static void mask(
			final BsonDocument doc,
			final List<FieldPathCursor> positions,
			final Map<String, String> renames,
			final boolean filter) {
		if (positions.isEmpty()) {
			return;
		}
		for (final Map.Entry<String, BsonValue> entry : doc.entrySet()) {
			final String key = entry.getKey();
			final List<FieldPathCursor> next = new ArrayList<>();
			boolean hit = false;
			// In a filter, a key starting with '$' may be an operator: a position operator ($and, $or,
			// $elemMatch, $gt...) keeps the position. It may also be a map key, so it is read as a key as well.
			final boolean dollarKey = filter && !key.isEmpty() && key.charAt(0) == '$';
			final boolean operatorKey = dollarKey && POSITION_OPERATORS.contains(key);
			if (operatorKey) {
				next.addAll(positions);
			}
			// In a filter, a key is also read as a dotted path when it can be one (a dot, an index).
			final boolean alsoPath = filter
					&& (key.indexOf('.') >= 0 || (!key.isEmpty() && Character.isDigit(key.charAt(0))));
			for (final FieldPathCursor position : positions) {
				// At the root, a column renamed by OptionRenameColumn is read under its model name.
				final String name = position.depth() == 0 ? modelName(key, renames) : key;
				boolean resolved = false;
				boolean unknownBelowRoot = false;
				for (final FieldPathCursor reading : readings(position, name, alsoPath)) {
					switch (reading.status()) {
						case PROTECTED:
						case UNSAFE:
							hit = true;
							resolved = true;
							break;
						case OK:
							next.add(reading);
							resolved = true;
							break;
						default:
							// Unknown segment: told apart below the root only (the root may hold other fields).
							unknownBelowRoot |= reading.depth() > 0;
							break;
					}
				}
				if (resolved) {
					continue;
				}
				// A key that no reading resolves, below the root: a field the model does not know (for
				// example one added by a subclass, stored by the class of the value) can be anything.
				// A key starting with '$' is told apart below, over all the positions at once.
				if (unknownBelowRoot && !dollarKey) {
					hit = true;
				}
			}
			// Fail closed: a '$' key that is neither a position operator nor a known map key is another operator
			// ($expr, $where, $jsonSchema...), which can name any protected field.
			if (dollarKey && !operatorKey && next.isEmpty()) {
				hit = true;
			}
			if (hit || next.size() > MAX_POSITIONS) {
				entry.setValue(new BsonString(MASK));
			} else {
				maskValue(entry.getValue(), next, renames, filter);
			}
		}
	}

	/** Replaces the first segment of a key by the model name of a renamed column. */
	private static String modelName(final String key, final Map<String, String> renames) {
		if (renames.isEmpty()) {
			return key;
		}
		final int dot = key.indexOf('.');
		final String first = dot < 0 ? key : key.substring(0, dot);
		final String renamed = renames.get(first);
		if (renamed == null) {
			return key;
		}
		return dot < 0 ? renamed : renamed + key.substring(dot);
	}

	/** The readings of a key at a position: as a document key, and also as a dotted path when asked. */
	private static List<FieldPathCursor> readings(
			final FieldPathCursor position,
			final String name,
			final boolean alsoPath) {
		if (alsoPath) {
			return List.of(position.member(name), position.path(name));
		}
		return List.of(position.member(name));
	}

	private static void maskValue(
			final BsonValue value,
			final List<FieldPathCursor> positions,
			final Map<String, String> renames,
			final boolean filter) {
		if (value instanceof final BsonDocument sub) {
			mask(sub, positions, renames, filter);
		} else if (value instanceof final BsonArray array) {
			for (final BsonValue item : array) {
				maskValue(item, positions, renames, filter);
			}
		}
	}
}
