package org.atriasoft.archidata.dataAccess.model.codec;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import org.atriasoft.archidata.dataAccess.model.DeclaredTypeRules;
import org.atriasoft.archidata.exception.DataAccessException;
import org.bson.BsonValue;
import org.bson.json.JsonObject;
import org.bson.types.BSONTimestamp;
import org.bson.types.Binary;
import org.bson.types.Code;
import org.bson.types.CodeWithScope;
import org.bson.types.Decimal128;
import org.bson.types.MaxKey;
import org.bson.types.MinKey;
import org.bson.types.ObjectId;
import org.bson.types.Symbol;

import com.mongodb.DBRef;
import com.mongodb.client.model.geojson.Geometry;

/**
 * Checks the content of a free-form {@code org.bson.Document} field before it is handed to the MongoDB driver, by an
 * allowlist that fails closed: the driver encodes a value by its own class, and would store a class of the
 * application with all its fields ({@code @DataNotRead} and {@code @DataEncrypt} ones included, in clear).
 *
 * <p>Accepted:
 * <ul>
 * <li>the values: {@code String}, the boxed primitives, {@code BigDecimal}, {@code ObjectId}, {@code UUID},
 * {@code Date}, {@code Instant}, {@code OffsetDateTime}, {@code LocalDate}, {@code LocalTime},
 * {@code LocalDateTime}, {@code Pattern}, the enums, and the arrays of primitives ({@code byte[]}...);</li>
 * <li>the BSON types: every {@code BsonValue} ({@code BsonDocument}, {@code BsonArray}...), {@code Binary},
 * {@code Decimal128}, {@code Code}, {@code Symbol}, {@code MinKey}, {@code MaxKey}, {@code BSONTimestamp},
 * {@code JsonObject}, and a GeoJSON {@code Geometry} of the driver (closed value classes);</li>
 * <li>walked: a {@code Map} ({@code Document} included) whose keys are strings, a {@code Collection}, an array of
 * objects, the scope of a {@code CodeWithScope} and the id of a {@code DBRef}.</li>
 * </ul>
 * Anything else is refused with a {@link DataAccessException} naming the field and the path of the value inside the
 * document: a class of the application, and a class of the JDK or of a library that is not listed (a wrapper such
 * as {@code AtomicReference}, {@code Optional} or {@code Map.Entry}, whose content would not be checked).
 */
final class FreeFormContentGuard {
	/** Nesting levels walked: MongoDB refuses a document nested deeper than 100. */
	static final int MAX_DEPTH = 100;

	/** Classes accepted as a single value (exact classes). */
	private static final Set<Class<?>> VALUE_CLASSES = Set.of(String.class, Integer.class, Long.class, Double.class,
			Float.class, Short.class, Byte.class, Boolean.class, Character.class, BigDecimal.class, ObjectId.class,
			UUID.class, Date.class, Instant.class, OffsetDateTime.class, LocalDate.class, LocalTime.class,
			LocalDateTime.class, Pattern.class, Binary.class, Decimal128.class, Code.class, Symbol.class, MinKey.class,
			MaxKey.class, BSONTimestamp.class, JsonObject.class);

	private FreeFormContentGuard() {}

	/**
	 * Checks a free-form value and everything it holds.
	 *
	 * @param value the value of the field (the {@code Document} itself), may be {@code null}
	 * @param site how the field is named in the message ({@code "Field com.app.Owner.field"}), or {@code null}
	 * @throws DataAccessException on the first value that is not accepted
	 */
	static void check(final Object value, final String site) throws DataAccessException {
		check(value, site, "", 0);
	}

	private static void check(final Object value, final String site, final String path, final int depth)
			throws DataAccessException {
		if (value == null) {
			return;
		}
		if (depth > MAX_DEPTH) {
			throw new DataAccessException(siteName(site) + " is a free-form org.bson.Document nested deeper than "
					+ MAX_DEPTH + " levels (under '" + path + "'): it cannot be stored");
		}
		final Class<?> clazz = value.getClass();
		if (VALUE_CLASSES.contains(clazz) || value instanceof Enum<?> || value instanceof BsonValue
				|| value instanceof Geometry) {
			return;
		}
		if (value instanceof final CodeWithScope code) {
			check(code.getScope(), site, path, depth + 1);
			return;
		}
		if (value instanceof final DBRef ref) {
			check(ref.getId(), site, childPath(path, "$id"), depth + 1);
			return;
		}
		if (value instanceof final Map<?, ?> map) {
			for (final Map.Entry<?, ?> entry : map.entrySet()) {
				if (!(entry.getKey() instanceof String)) {
					throw refused(site, path, clazz.getName() + " with a "
							+ (entry.getKey() == null ? "null" : entry.getKey().getClass().getName()) + " key");
				}
				check(entry.getValue(), site, childPath(path, (String) entry.getKey()), depth + 1);
			}
			return;
		}
		if (value instanceof final Collection<?> collection) {
			int index = 0;
			for (final Object item : collection) {
				check(item, site, childPath(path, Integer.toString(index)), depth + 1);
				index++;
			}
			return;
		}
		if (clazz.isArray()) {
			if (!clazz.getComponentType().isPrimitive()) {
				final Object[] items = (Object[]) value;
				for (int iii = 0; iii < items.length; iii++) {
					check(items[iii], site, childPath(path, Integer.toString(iii)), depth + 1);
				}
			}
			return;
		}
		if (DeclaredTypeRules.isApplicationClass(clazz)) {
			throw new DataAccessException(siteName(site) + " is a free-form org.bson.Document holding, under '" + path
					+ "', a " + clazz.getName()
					+ ", a class of the application: the driver would store all its fields (@DataNotRead and"
					+ " @DataEncrypt ones included, in clear); put only values, Documents, Lists and Maps in a"
					+ " Document, or declare a field with that class");
		}
		throw refused(site, path, clazz.getName());
	}

	private static DataAccessException refused(final String site, final String path, final String what) {
		return new DataAccessException(siteName(site) + " is a free-form org.bson.Document holding, under '" + path
				+ "', a " + what
				+ ", which is not a value archidata accepts in a Document (a BSON or JDK value, an enum,"
				+ " a Document or a Map with String keys, a List, a Collection or an array of them): what it holds"
				+ " would not be checked; put only values, Documents, Lists and Maps in a Document, or declare a field"
				+ " with the class it holds");
	}

	private static String siteName(final String site) {
		return site == null ? "A value" : site;
	}

	private static String childPath(final String path, final String key) {
		return path.isEmpty() ? key : path + "." + key;
	}
}
