package org.atriasoft.archidata.dataAccess.model.codec;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.atriasoft.archidata.bean.TypeInfo;
import org.atriasoft.archidata.bean.accessor.PropertyGetter;
import org.atriasoft.archidata.bean.accessor.PropertySetter;
import org.atriasoft.archidata.bean.exception.IntrospectionException;
import org.atriasoft.archidata.dataAccess.model.DbClassModel;
import org.atriasoft.archidata.dataAccess.model.DbPropertyDescriptor;
import org.atriasoft.archidata.exception.DataAccessException;
import org.bson.Document;
import org.bson.types.ObjectId;

/**
 * Factory that creates pre-compiled {@link MongoFieldCodec} instances for each field type.
 *
 * <p>All type resolution happens here, once, at construction time.
 * The resulting codecs are simple lambda calls with zero type checking at runtime.
 *
 * <p>The type checking {@code if/else} chain from {@code setValueToDb}, {@code convertInDocument},
 * {@code setValueFromDoc}, and {@code createObjectFromDocument} is executed exactly once per field
 * to select the right lambda, then never again.
 */
public final class MongoCodecFactory {

	private static final DateTimeFormatter LOCAL_DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	/** Identity converter (for types that MongoDB driver accepts directly). */
	private static final MongoTypeWriter IDENTITY_W = v -> v;
	private static final MongoTypeReader IDENTITY_R = v -> v;

	private MongoCodecFactory() {
	}

	// ========== Public API ==========

	/**
	 * Build a complete field codec for a property with its resolved DB metadata.
	 *
	 * @param getter the pre-built lambda getter for the property
	 * @param setter the pre-built lambda setter for the property (may be null for read-only)
	 * @param typeInfo the resolved type info for the property
	 * @param dbFieldName the MongoDB field name
	 * @return a pre-compiled MongoFieldCodec
	 */
	public static MongoFieldCodec buildFieldCodec(
			final PropertyGetter getter,
			final PropertySetter setter,
			final TypeInfo typeInfo,
			final String dbFieldName) {
		final MongoTypeWriter writer = buildWriter(typeInfo);
		final MongoTypeReader reader = buildReader(typeInfo);
		return new MongoFieldCodec(getter, setter, dbFieldName, writer, reader, typeInfo.isPrimitive());
	}

	/**
	 * Build a standalone writer for a given type.
	 * Used for collection element conversion and sub-object recursion.
	 */
	public static MongoTypeWriter buildWriter(final TypeInfo typeInfo) {
		final Class<?> raw = typeInfo.rawType();

		// --- Primitives and boxed: identity (MongoDB driver handles them natively) ---
		if (raw == long.class || raw == Long.class) {
			return IDENTITY_W;
		}
		if (raw == int.class || raw == Integer.class) {
			return IDENTITY_W;
		}
		if (raw == float.class || raw == Float.class) {
			return IDENTITY_W;
		}
		if (raw == double.class || raw == Double.class) {
			return IDENTITY_W;
		}
		if (raw == boolean.class || raw == Boolean.class) {
			return IDENTITY_W;
		}
		if (raw == short.class || raw == Short.class) {
			return IDENTITY_W;
		}
		if (raw == String.class) {
			return IDENTITY_W;
		}
		if (raw == ObjectId.class) {
			return IDENTITY_W;
		}
		if (raw == UUID.class) {
			return IDENTITY_W;
		}
		if (raw == Date.class) {
			return IDENTITY_W;
		}
		if (raw == Character.class) {
			return IDENTITY_W;
		}

		// --- Temporal conversions ---
		if (raw == Instant.class) {
			return v -> Date.from((Instant) v);
		}
		if (raw == LocalDate.class) {
			return v -> ((LocalDate) v).format(LOCAL_DATE_FMT);
		}
		if (raw == LocalTime.class) {
			return v -> ((LocalTime) v).toNanoOfDay();
		}

		// --- Enum ---
		if (raw.isEnum()) {
			return Object::toString;
		}

		// --- List<E> ---
		if (typeInfo.isList()) {
			final MongoTypeWriter elementWriter = buildElementWriterFromTypeInfo(typeInfo);
			return v -> {
				final List<?> list = (List<?>) v;
				final List<Object> out = new ArrayList<>(list.size());
				for (final Object elem : list) {
					out.add(elem == null ? null : elementWriter.toMongo(elem));
				}
				return out;
			};
		}

		// --- Set<E> (stored as List in MongoDB) ---
		if (typeInfo.isSet()) {
			final MongoTypeWriter elementWriter = buildElementWriterFromTypeInfo(typeInfo);
			return v -> {
				final Set<?> set = (Set<?>) v;
				final List<Object> out = new ArrayList<>(set.size());
				for (final Object elem : set) {
					out.add(elem == null ? null : elementWriter.toMongo(elem));
				}
				return out;
			};
		}

		// --- Map<K, V> ---
		if (typeInfo.isMap()) {
			final MongoTypeWriter valueWriter = buildElementWriterFromTypeInfo(typeInfo);
			return v -> {
				final Map<?, ?> map = (Map<?, ?>) v;
				final Document doc = new Document();
				for (final Map.Entry<?, ?> entry : map.entrySet()) {
					final String key = convertMapKeyToString(entry.getKey());
					final Object val = entry.getValue() == null ? null : valueWriter.toMongo(entry.getValue());
					doc.append(key, val);
				}
				return doc;
			};
		}

		// --- Sub-object (POJO/record) → recursive Document ---
		return MongoCodecFactory::writeSubObject;
	}

	/**
	 * Build a standalone reader for a given type.
	 * Used for collection element conversion and sub-object recursion.
	 */
	public static MongoTypeReader buildReader(final TypeInfo typeInfo) {
		final Class<?> raw = typeInfo.rawType();

		// --- Types that MongoDB returns natively ---
		if (raw == String.class) {
			return IDENTITY_R;
		}
		if (raw == ObjectId.class) {
			return IDENTITY_R;
		}
		if (raw == UUID.class) {
			return IDENTITY_R;
		}
		if (raw == Date.class) {
			return IDENTITY_R;
		}
		if (raw == Boolean.class || raw == boolean.class) {
			return IDENTITY_R;
		}

		// --- Numeric types with MongoDB coercion ---
		if (raw == Long.class || raw == long.class) {
			return MongoCodecFactory::readLong;
		}
		if (raw == Integer.class || raw == int.class) {
			return MongoCodecFactory::readInteger;
		}
		if (raw == Short.class || raw == short.class) {
			return MongoCodecFactory::readShort;
		}
		if (raw == Float.class || raw == float.class) {
			return MongoCodecFactory::readFloat;
		}
		if (raw == Double.class || raw == double.class) {
			return MongoCodecFactory::readDouble;
		}
		if (raw == Character.class) {
			return v -> (char) ((Integer) v).intValue();
		}

		// --- Temporal ---
		if (raw == Instant.class) {
			return v -> ((Date) v).toInstant();
		}
		if (raw == LocalDate.class) {
			return v -> LocalDate.parse((String) v, LOCAL_DATE_FMT);
		}
		if (raw == LocalTime.class) {
			return v -> LocalTime.ofNanoOfDay((Long) v);
		}

		// --- Enum (O(1) lookup via pre-built HashMap) ---
		if (raw.isEnum()) {
			final Object[] constants = raw.getEnumConstants();
			final Map<String, Object> lookup = new HashMap<>(constants.length * 2);
			for (final Object c : constants) {
				lookup.put(c.toString(), c);
			}
			return v -> {
				final Object result = lookup.get((String) v);
				if (result == null) {
					throw new DataAccessException(
							"Unknown enum value '" + v + "' for " + raw.getSimpleName());
				}
				return result;
			};
		}

		// --- List<E> ---
		if (typeInfo.isList()) {
			final MongoTypeReader elementReader = buildElementReaderFromTypeInfo(typeInfo);
			return v -> {
				final List<?> mongoList = (List<?>) v;
				final List<Object> out = new ArrayList<>(mongoList.size());
				for (final Object item : mongoList) {
					out.add(item == null ? null : elementReader.fromMongo(item));
				}
				return out;
			};
		}

		// --- Set<E> (MongoDB stores as List) ---
		if (typeInfo.isSet()) {
			final MongoTypeReader elementReader = buildElementReaderFromTypeInfo(typeInfo);
			return v -> {
				final List<?> mongoList = (List<?>) v;
				final Set<Object> out = new HashSet<>(mongoList.size());
				for (final Object item : mongoList) {
					out.add(item == null ? null : elementReader.fromMongo(item));
				}
				return out;
			};
		}

		// --- Map<K, V> ---
		if (typeInfo.isMap()) {
			final MongoTypeReader valueReader = buildElementReaderFromTypeInfo(typeInfo);
			final MapKeyConverter keyConverter = buildMapKeyConverter(typeInfo.keyType());
			return v -> {
				final Document subDoc = (Document) v;
				final Map<Object, Object> out = new HashMap<>(subDoc.size());
				for (final Map.Entry<String, Object> entry : subDoc.entrySet()) {
					final Object key = keyConverter.fromString(entry.getKey());
					final Object val = entry.getValue() == null ? null : valueReader.fromMongo(entry.getValue());
					out.put(key, val);
				}
				return out;
			};
		}

		// --- Sub-object (POJO/record) ---
		return v -> readSubObject(v, raw);
	}

	// ========== Numeric coercion helpers ==========

	private static Object readLong(final Object v) {
		if (v instanceof final Long l) {
			return l;
		}
		if (v instanceof final Integer i) {
			return i.longValue();
		}
		if (v instanceof final Short s) {
			return s.longValue();
		}
		return ((Number) v).longValue();
	}

	private static Object readInteger(final Object v) {
		if (v instanceof final Integer i) {
			return i;
		}
		if (v instanceof final Long l) {
			return l.intValue();
		}
		if (v instanceof final Short s) {
			return s.intValue();
		}
		return ((Number) v).intValue();
	}

	private static Object readShort(final Object v) {
		if (v instanceof final Short s) {
			return s;
		}
		if (v instanceof final Integer i) {
			return i.shortValue();
		}
		if (v instanceof final Long l) {
			return l.shortValue();
		}
		return ((Number) v).shortValue();
	}

	private static Object readFloat(final Object v) {
		if (v instanceof final Float f) {
			return f;
		}
		if (v instanceof final Double d) {
			return d.floatValue();
		}
		return ((Number) v).floatValue();
	}

	private static Object readDouble(final Object v) {
		if (v instanceof final Double d) {
			return d;
		}
		if (v instanceof final Float f) {
			return f.doubleValue();
		}
		return ((Number) v).doubleValue();
	}

	// ========== Sub-object recursive helpers ==========

	/**
	 * Convert a Java POJO/record to a MongoDB Document (recursive writer).
	 * Replaces the old {@code convertInDocument()} for sub-objects.
	 */
	private static Object writeSubObject(final Object data) throws Exception {
		final Class<?> clazz = data.getClass();
		final DbClassModel dbModel = DbClassModel.of(clazz);
		final Document out = new Document();

		// Regular fields
		for (final DbPropertyDescriptor desc : dbModel.getRegularFields()) {
			final MongoFieldCodec codec = desc.getCodec();
			if (codec != null) {
				codec.writeToDoc(null, data, out, null);
			}
		}

		// Special fields (PK, timestamps, deleted)
		writeSpecialField(dbModel.getPrimaryKey(), data, out);
		writeSpecialField(dbModel.getCreationTimestamp(), data, out);
		writeSpecialField(dbModel.getUpdateTimestamp(), data, out);
		writeSpecialField(dbModel.getDeletedField(), data, out);

		// Addon fields: use their insertData() mechanism (not codec)
		for (final DbPropertyDescriptor desc : dbModel.getAddonFields()) {
			final MongoFieldCodec codec = desc.getCodec();
			if (codec != null) {
				codec.writeToDoc(null, data, out, null);
			}
		}

		return out;
	}

	private static void writeSpecialField(final DbPropertyDescriptor desc, final Object data, final Document out)
			throws Exception {
		if (desc == null || desc.getCodec() == null) {
			return;
		}
		desc.getCodec().writeToDoc(null, data, out, null);
	}

	/**
	 * Read a MongoDB Document into a Java POJO/record (recursive reader).
	 * Replaces the old sub-object path in {@code createObjectFromDocument()}.
	 */
	private static Object readSubObject(final Object mongoValue, final Class<?> targetClass) throws Exception {
		if (!(mongoValue instanceof final Document doc)) {
			throw new DataAccessException(
					"Expected Document for " + targetClass.getSimpleName()
							+ " but got " + mongoValue.getClass().getSimpleName());
		}
		final DbClassModel dbModel = DbClassModel.of(targetClass);
		final Object instance = dbModel.getClassModel().newInstance();

		for (final DbPropertyDescriptor desc : dbModel.getAllFields()) {
			if (desc.getAddOn() != null) {
				// AddOns have their own fillFromDoc logic, skip for simple sub-objects
				continue;
			}
			final MongoFieldCodec codec = desc.getCodec();
			if (codec != null) {
				codec.readFromDoc(doc, instance);
			}
		}
		return instance;
	}

	// ========== Element codec builders ==========

	/**
	 * Extract the full generic Type for the element (value) of a List/Set/Map from the parent's genericType.
	 * For List/Set: returns the first type argument.
	 * For Map: returns the second type argument (the value type).
	 * Falls back to the raw elementType class if generic info is unavailable.
	 */
	private static TypeInfo resolveElementTypeInfo(final TypeInfo parentTypeInfo) {
		final Type generic = parentTypeInfo.genericType();
		if (generic instanceof final ParameterizedType pt) {
			final Type[] args = pt.getActualTypeArguments();
			if (parentTypeInfo.isMap() && args.length > 1) {
				return TypeInfo.fromType(args[1]);
			}
			if (args.length > 0) {
				return TypeInfo.fromType(args[0]);
			}
		}
		// Fallback: use raw elementType
		final Class<?> elementType = parentTypeInfo.elementType();
		if (elementType == null) {
			return null;
		}
		return TypeInfo.ofRaw(elementType);
	}

	/**
	 * Build a writer for collection/map element types, preserving full generic type info.
	 */
	private static MongoTypeWriter buildElementWriterFromTypeInfo(final TypeInfo parentTypeInfo) {
		final TypeInfo elementTi = resolveElementTypeInfo(parentTypeInfo);
		if (elementTi == null) {
			return MongoCodecFactory::writeSubObject;
		}
		return buildWriter(elementTi);
	}

	/**
	 * Build a reader for collection/map element types, preserving full generic type info.
	 */
	private static MongoTypeReader buildElementReaderFromTypeInfo(final TypeInfo parentTypeInfo) {
		final TypeInfo elementTi = resolveElementTypeInfo(parentTypeInfo);
		if (elementTi == null) {
			return IDENTITY_R;
		}
		return buildReader(elementTi);
	}

	// ========== Map key conversion ==========

	/**
	 * Build a pre-compiled map key converter for the given key type.
	 */
	public static MapKeyConverter buildMapKeyConverter(final Class<?> keyType) {
		if (keyType == null || keyType == String.class) {
			return new MapKeyConverter() {
				@Override
				public Object fromString(final String key) {
					return key;
				}

				@Override
				public String toString(final Object key) {
					return (String) key;
				}
			};
		}
		if (keyType == Integer.class) {
			return new MapKeyConverter() {
				@Override
				public Object fromString(final String key) {
					return Integer.parseInt(key);
				}

				@Override
				public String toString(final Object key) {
					return key.toString();
				}
			};
		}
		if (keyType == Long.class) {
			return new MapKeyConverter() {
				@Override
				public Object fromString(final String key) {
					return Long.parseLong(key);
				}

				@Override
				public String toString(final Object key) {
					return key.toString();
				}
			};
		}
		if (keyType == Short.class) {
			return new MapKeyConverter() {
				@Override
				public Object fromString(final String key) {
					return Short.parseShort(key);
				}

				@Override
				public String toString(final Object key) {
					return key.toString();
				}
			};
		}
		if (keyType == ObjectId.class) {
			return new MapKeyConverter() {
				@Override
				public Object fromString(final String key) {
					return new ObjectId(key);
				}

				@Override
				public String toString(final Object key) {
					return key.toString();
				}
			};
		}
		if (keyType.isEnum()) {
			final Object[] constants = keyType.getEnumConstants();
			final Map<String, Object> lookup = new HashMap<>(constants.length * 2);
			for (final Object c : constants) {
				lookup.put(c.toString(), c);
			}
			return new MapKeyConverter() {
				@Override
				public Object fromString(final String key) throws Exception {
					final Object result = lookup.get(key);
					if (result == null) {
						throw new DataAccessException("Unknown enum key '" + key + "' for " + keyType.getSimpleName());
					}
					return result;
				}

				@Override
				public String toString(final Object key) {
					return key.toString();
				}
			};
		}
		// Fallback: toString/identity
		return new MapKeyConverter() {
			@Override
			public Object fromString(final String key) {
				return key;
			}

			@Override
			public String toString(final Object key) {
				return key.toString();
			}
		};
	}

	/**
	 * Convert a map key to its String representation (used by writer).
	 */
	private static String convertMapKeyToString(final Object key) throws DataAccessException {
		if (key instanceof final String s) {
			return s;
		}
		if (key instanceof final Integer i) {
			return i.toString();
		}
		if (key instanceof final Long l) {
			return l.toString();
		}
		if (key instanceof final Short s) {
			return s.toString();
		}
		if (key instanceof final ObjectId oid) {
			return oid.toHexString();
		}
		if (key.getClass().isEnum()) {
			return key.toString();
		}
		throw new DataAccessException("Unsupported Map key type: " + key.getClass().getCanonicalName());
	}
}
