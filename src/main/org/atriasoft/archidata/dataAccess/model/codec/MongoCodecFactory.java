package org.atriasoft.archidata.dataAccess.model.codec;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.atriasoft.archidata.bean.ClassModel;
import org.atriasoft.archidata.bean.PropertyDescriptor;
import org.atriasoft.archidata.bean.TypeInfo;
import org.atriasoft.archidata.bean.accessor.PropertyGetter;
import org.atriasoft.archidata.bean.accessor.PropertySetter;
import org.atriasoft.archidata.bean.exception.IntrospectionException;
import org.atriasoft.archidata.crypto.FieldEncryptionContext;
import org.atriasoft.archidata.dataAccess.model.DbClassModel;
import org.atriasoft.archidata.dataAccess.model.DbPropertyDescriptor;
import org.atriasoft.archidata.dataAccess.model.DeclaredTypeRules;
import org.atriasoft.archidata.dataAccess.model.DeclaredTypeRules.Kind;
import org.atriasoft.archidata.exception.DataAccessException;
import org.atriasoft.archidata.tools.ConfigBaseVariable;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

	private static final Logger LOGGER = LoggerFactory.getLogger(MongoCodecFactory.class);

	private static final DateTimeFormatter LOCAL_DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

	/**
	 * Whether the sub-object reader of the current thread returns the {@code @DataNotRead} fields of embedded
	 * objects ({@code ReadAllColumn} on the current read). The readers are pre-compiled and shared, so the
	 * option travels by thread; the default ({@code false}) keeps the fields hidden.
	 */
	private static final ThreadLocal<Boolean> SUB_OBJECT_READ_ALL_COLUMN = ThreadLocal.withInitial(() -> Boolean.FALSE);

	/** Identity converter (for types that MongoDB driver accepts directly). */
	private static final MongoTypeWriter IDENTITY_W = v -> v;
	private static final MongoTypeReader IDENTITY_R = v -> v;

	private MongoCodecFactory() {}

	// ========== Public API ==========

	/**
	 * Sets whether the embedded sub-objects read by the current thread return their {@code @DataNotRead} fields.
	 *
	 * <p>Called around a read by the data access layer, with the {@code ReadAllColumn} flag of the query:
	 * restore the returned previous value in a {@code finally} block.
	 *
	 * @param readAllColumn {@code true} to read the {@code @DataNotRead} fields of sub-objects
	 * @return the previous value, to restore after the read
	 */
	public static boolean setSubObjectReadAllColumn(final boolean readAllColumn) {
		final boolean previous = SUB_OBJECT_READ_ALL_COLUMN.get();
		SUB_OBJECT_READ_ALL_COLUMN.set(readAllColumn);
		return previous;
	}

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
		return buildFieldCodec(getter, setter, typeInfo, dbFieldName, null);
	}

	/**
	 * Build a complete field codec, optionally wrapping the converters with field encryption.
	 *
	 * @param getter the pre-built lambda getter for the property
	 * @param setter the pre-built lambda setter for the property (may be null for read-only)
	 * @param typeInfo the resolved type info for the property
	 * @param dbFieldName the MongoDB field name
	 * @param encryptCtx the field encryption context, or {@code null} for a clear field
	 * @return a pre-compiled MongoFieldCodec
	 */
	public static MongoFieldCodec buildFieldCodec(
			final PropertyGetter getter,
			final PropertySetter setter,
			final TypeInfo typeInfo,
			final String dbFieldName,
			final FieldEncryptionContext encryptCtx) {
		return buildFieldCodec(getter, setter, typeInfo, dbFieldName, encryptCtx, null);
	}

	/**
	 * Build a complete field codec, optionally wrapping the converters with field encryption, for a field whose
	 * qualified name is known (used in the warning logged when a value of a subclass is stored).
	 *
	 * @param getter the pre-built lambda getter for the property
	 * @param setter the pre-built lambda setter for the property (may be null for read-only)
	 * @param typeInfo the resolved type info for the property
	 * @param dbFieldName the MongoDB field name
	 * @param encryptCtx the field encryption context, or {@code null} for a clear field
	 * @param qualifiedFieldName the field as {@code com.app.Owner.field}, or {@code null} when unknown
	 * @return a pre-compiled MongoFieldCodec
	 */
	public static MongoFieldCodec buildFieldCodec(
			final PropertyGetter getter,
			final PropertySetter setter,
			final TypeInfo typeInfo,
			final String dbFieldName,
			final FieldEncryptionContext encryptCtx,
			final String qualifiedFieldName) {
		MongoTypeWriter writer = buildWriter(typeInfo,
				qualifiedFieldName == null ? null : "Field " + qualifiedFieldName);
		MongoTypeReader reader = buildReader(typeInfo);
		if (encryptCtx != null) {
			if (typeInfo.rawType() != String.class) {
				throw new IllegalStateException("@DataEncrypt is only supported on String fields (field '" + dbFieldName
						+ "' is " + typeInfo.rawType().getName() + ")");
			}
			final MongoTypeWriter baseWriter = writer;
			final MongoTypeReader baseReader = reader;
			writer = javaValue -> {
				final Object base = baseWriter.toMongo(javaValue);
				try {
					return encryptCtx.encrypt((String) base);
				} catch (final Exception e) {
					// Never silently store the clear value: fail the whole operation.
					throw new DataAccessException("Failed to encrypt field '" + dbFieldName + "'", e);
				}
			};
			reader = mongoValue -> {
				if (!ConfigBaseVariable.getDataDecryptEnabled()) {
					// Decryption not authorized: leave the field unset, data stays protected.
					return null;
				}
				if (!encryptCtx.canDecrypt()) {
					// No decryption key on this node (e.g. encrypt-only deployment): leave unset.
					LOGGER.warn("No decryption key available for field '{}': value left unset", dbFieldName);
					return null;
				}
				final String clear;
				try {
					clear = encryptCtx.decrypt((String) mongoValue);
				} catch (final Exception e) {
					// Wrong key, scheme mismatch or tampered data: surface a clear error.
					throw new DataAccessException("Failed to decrypt field '" + dbFieldName + "'", e);
				}
				return baseReader.fromMongo(clear);
			};
		}
		return new MongoFieldCodec(getter, setter, dbFieldName, writer, reader, typeInfo.isPrimitive());
	}

	/**
	 * Build a standalone writer for a given type.
	 * Used for collection element conversion and sub-object recursion.
	 *
	 * <p>An embedded object is written with the model of this <em>declared</em> type, at any depth (elements of
	 * lists and sets, values of maps): a value of a subclass is stored with the fields of the declared type only
	 * (a warning is logged once). A declared type that does not tell which fields are stored ({@code Object}, an
	 * interface, an abstract class, a raw or unresolved list, set or map) gives a writer that refuses every value:
	 * a persisted field so declared is refused earlier, when the model is analysed ({@link DeclaredTypeRules}).
	 *
	 * @param typeInfo the resolved type information for the value to convert
	 * @return a pre-compiled writer lambda
	 */
	public static MongoTypeWriter buildWriter(final TypeInfo typeInfo) {
		return buildWriter(typeInfo, null);
	}

	/**
	 * @param site how the value is named in the warning of a subclass ({@code "Field com.app.Owner.field"},
	 *        {@code "An element of field ..."}), or {@code null} when unknown
	 */
	private static MongoTypeWriter buildWriter(final TypeInfo typeInfo, final String site) {
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
		if (raw == OffsetDateTime.class) {
			// Stored as a BSON date (UTC instant, millisecond precision): the offset is not kept
			return v -> Date.from(((OffsetDateTime) v).toInstant());
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

		// --- BSON Document (must be checked before Map, since Document implements Map) ---
		if (raw == Document.class) {
			// Free-form content, handed to the driver as it is: the driver would encode a class of the application
			// with its own model (every field, @DataNotRead and @DataEncrypt ones in clear). Checked by an allowlist.
			return v -> {
				FreeFormContentGuard.check(v, site);
				return v;
			};
		}

		// --- List<E> ---
		if (typeInfo.isList()) {
			final MongoTypeWriter elementWriter = buildElementWriterFromTypeInfo(typeInfo,
					elementSite("An element of", site));
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
			final MongoTypeWriter elementWriter = buildElementWriterFromTypeInfo(typeInfo,
					elementSite("An element of", site));
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
			final MongoTypeWriter valueWriter = buildElementWriterFromTypeInfo(typeInfo,
					elementSite("A value of", site));
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

		// --- Sub-object (POJO/record) → recursive Document, by the declared type ---
		final Kind kind = DeclaredTypeRules.kindOf(typeInfo);
		if (kind == Kind.EMBEDDED) {
			return v -> writeSubObject(v, raw, site);
		}
		if (kind.isRefused()) {
			return refusingWriter(typeInfo);
		}
		// A JDK, BSON or Jackson class the codec does not model (BigDecimal, arrays...): unchanged, by its own class,
		// unless the value is a class of the application (a subclass of a JDK class, an implementation of a BSON
		// interface, an array of them): it would be stored with its own model while the position is not modeled.
		return v -> {
			final Class<?> runtime = v.getClass();
			if (runtime != raw && DeclaredTypeRules.isApplicationClass(DeclaredTypeRules.innermostComponent(runtime))) {
				throw new DataAccessException((site == null ? "A value" : site) + " is declared " + raw.getName()
						+ ", a type archidata does not model, but holds a " + runtime.getName()
						+ ", a class of the application: it cannot be stored there; declare the field with that class");
			}
			return writeSubObject(v, runtime, null);
		};
	}

	/** Name the elements (or values) of a collection (or map) in the warning of a subclass. */
	private static String elementSite(final String prefix, final String site) {
		if (site == null) {
			return null;
		}
		return prefix + " " + Character.toLowerCase(site.charAt(0)) + site.substring(1);
	}

	/**
	 * A writer for a declared type that does not tell which fields are stored: refuses every value (fail closed).
	 * A persisted field so declared is refused when the model is analysed; this writer is reached only through
	 * the public {@link #buildWriter(TypeInfo)}.
	 */
	private static MongoTypeWriter refusingWriter(final TypeInfo typeInfo) {
		final String declared = typeInfo.genericType().getTypeName();
		return v -> {
			throw new DataAccessException("Cannot store a value declared " + declared
					+ ": archidata stores an embedded value by its declared type only, and this type does not tell"
					+ " which fields to store (Object, an interface, an abstract class, or a list, set or map whose"
					+ " element type is unknown)");
		};
	}

	/**
	 * Build a standalone reader for a given type.
	 * Used for collection element conversion and sub-object recursion.
	 *
	 * @param typeInfo the resolved type information for the value to convert
	 * @return a pre-compiled reader lambda
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
		if (raw == OffsetDateTime.class) {
			// Read back in UTC, like the BSON OffsetDateTimeCodec
			return v -> OffsetDateTime.ofInstant(((Date) v).toInstant(), ZoneOffset.UTC);
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
					throw new DataAccessException("Unknown enum value '" + v + "' for " + raw.getSimpleName());
				}
				return result;
			};
		}

		// --- BSON Document (must be checked before Map, since Document implements Map) ---
		if (raw == Document.class) {
			return IDENTITY_R;
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
	 * Convert a Java POJO/record to a MongoDB Document (recursive writer), with the model of the declared class:
	 * a value of a subclass is stored with the fields of the declared class only.
	 *
	 * @param data the value (not {@code null})
	 * @param declaredClass the declared class of the value
	 * @param site how the value is named in the warning of a subclass, or {@code null}
	 */
	private static Object writeSubObject(final Object data, final Class<?> declaredClass, final String site)
			throws Exception {
		checkDeclaredInstance(data, declaredClass, site);
		final DbClassModel dbModel = DbClassModel.of(declaredClass);
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
	 *
	 * <p>For Records, collects all field values from the Document first, then uses
	 * the canonical constructor via {@link ClassModel#newInstance(Map)}.
	 * For POJOs, uses the no-arg constructor and populates fields via setters.
	 */
	private static Object readSubObject(final Object mongoValue, final Class<?> targetClass) throws Exception {
		if (!(mongoValue instanceof final Document doc)) {
			throw new DataAccessException("Expected Document for " + targetClass.getSimpleName() + " but got "
					+ mongoValue.getClass().getSimpleName());
		}
		final DbClassModel dbModel = DbClassModel.of(targetClass);
		final ClassModel classModel = dbModel.getClassModel();
		// @DataNotRead applies inside embedded objects too: hidden unless the read asked for ReadAllColumn.
		final boolean readNotRead = SUB_OBJECT_READ_ALL_COLUMN.get();

		if (classModel.isRecord()) {
			// Records: collect all values then instantiate via canonical constructor
			final Map<String, Object> values = new LinkedHashMap<>();
			for (final DbPropertyDescriptor desc : dbModel.getAllFields()) {
				if (desc.getAddOn() != null || (desc.isNotRead() && !readNotRead)) {
					continue;
				}
				final MongoFieldCodec codec = desc.getCodec();
				if (codec != null) {
					final String fieldName = codec.getDbFieldName();
					if (doc.containsKey(fieldName)) {
						final Object mongoVal = doc.get(fieldName);
						if (mongoVal != null) {
							values.put(desc.getProperty().getName(), codec.getReader().fromMongo(mongoVal));
						}
					}
				}
			}
			return classModel.newInstance(values);
		}

		// POJOs: no-arg constructor + setters
		final Object instance = classModel.newInstance();
		for (final DbPropertyDescriptor desc : dbModel.getAllFields()) {
			if (desc.getAddOn() != null || (desc.isNotRead() && !readNotRead)) {
				continue;
			}
			final MongoFieldCodec codec = desc.getCodec();
			if (codec != null && codec.canSetValue()) {
				codec.readFromDoc(doc, instance);
			}
		}
		return instance;
	}

	// ========== Embedded objects with protected fields (full update) ==========

	/** Keys ({@code site|declared class|runtime class}) of the subclass warnings already logged. */
	private static final Set<String> SUBCLASS_WARNINGS = ConcurrentHashMap.newKeySet();

	/**
	 * Check that a value is an instance of its declared class, and log once per place and runtime class that a
	 * subclass is stored with the fields of the declared class only.
	 */
	private static void checkDeclaredInstance(final Object data, final Class<?> declaredClass, final String site)
			throws DataAccessException {
		final Class<?> runtimeClass = data.getClass();
		if (runtimeClass == declaredClass) {
			return;
		}
		final String name = site == null ? "A value" : site;
		if (!declaredClass.isInstance(data)) {
			throw new DataAccessException(name + " is declared " + declaredClass.getName() + " but holds a "
					+ runtimeClass.getName() + ", which is not one: it cannot be stored");
		}
		if (!SUBCLASS_WARNINGS.add(name + "|" + declaredClass.getName() + "|" + runtimeClass.getName())) {
			return;
		}
		LOGGER.warn(
				"{} is declared {} but holds a {}: only the fields of {} are stored, the fields {} adds are"
						+ " dropped ({})",
				name, declaredClass.getName(), runtimeClass.getName(), declaredClass.getSimpleName(),
				runtimeClass.getSimpleName(), addedFieldNames(declaredClass, runtimeClass));
	}

	/** Names of the readable properties a subclass adds to its declared class (names only, never a value). */
	private static List<String> addedFieldNames(final Class<?> declaredClass, final Class<?> runtimeClass) {
		final List<String> out = new ArrayList<>();
		try {
			final Set<String> declared = new HashSet<>();
			for (final PropertyDescriptor prop : ClassModel.of(declaredClass).getProperties()) {
				declared.add(prop.getName());
			}
			for (final PropertyDescriptor prop : ClassModel.of(runtimeClass).getProperties()) {
				if (prop.canRead() && !declared.contains(prop.getName())) {
					out.add(prop.getName());
				}
			}
		} catch (final IntrospectionException ex) {
			LOGGER.debug("Can not list the fields of {}: {}", runtimeClass.getName(), ex.getMessage());
		}
		return out;
	}

	/** Cache of {@link #hasProtectedSubFields(TypeInfo)}: the model of a class never changes. */
	private static final Map<TypeInfo, Boolean> PROTECTED_SUB_FIELDS = new ConcurrentHashMap<>();

	/**
	 * Tells whether a field of this type is stored as one embedded document modeled by archidata: a concrete
	 * class (a POJO or a record) of the application, and not a value, a list, a set, a map, a class of the JDK,
	 * of BSON or of Jackson ({@link DeclaredTypeRules.Kind#EMBEDDED}).
	 *
	 * @param typeInfo the declared type of the field
	 * @return {@code true} for an embedded object
	 */
	public static boolean isEmbeddedObjectType(final TypeInfo typeInfo) {
		return DeclaredTypeRules.kindOf(typeInfo) == Kind.EMBEDDED;
	}

	/**
	 * Told whether the writer encoded a field of this declared type with the class of its value rather than with
	 * the declared type. No type is written that way any more: an embedded object is written by its declared type,
	 * at any depth, and a field whose declared type does not tell what it holds is refused when the model is
	 * analysed.
	 *
	 * @param typeInfo the declared type of the field (not read)
	 * @return always {@code false}
	 * @deprecated nothing is written by the class of its value any more; the type rules are in
	 *             {@link DeclaredTypeRules}
	 */
	@Deprecated(since = "0.51.4")
	public static boolean isWrittenByRuntimeClass(final TypeInfo typeInfo) {
		return false;
	}

	/**
	 * Tells whether a field of this type holds, at any depth, embedded fields that a standard read leaves
	 * {@code null}: {@code @DataNotRead} or {@code @DataEncrypt} fields of an embedded object, of the elements
	 * of a list or a set, or of the values of a map.
	 *
	 * <p>Rewriting such a field as a whole from a read entity would erase them: a full update writes the
	 * visible fields of an embedded object one by one ({@link #writeEmbeddedVisibleFields}) and leaves a list,
	 * a set or a map of such objects untouched.
	 *
	 * @param typeInfo the declared type of the field
	 * @return {@code true} when the type contains protected embedded fields
	 */
	public static boolean hasProtectedSubFields(final TypeInfo typeInfo) {
		final Boolean cached = PROTECTED_SUB_FIELDS.get(typeInfo);
		if (cached != null) {
			return cached;
		}
		// Only the result of the root is cached: below it, a class met again (cycle) answers false.
		final boolean out = hasProtectedSubFields(typeInfo, new HashSet<>());
		PROTECTED_SUB_FIELDS.put(typeInfo, out);
		return out;
	}

	private static boolean hasProtectedSubFields(final TypeInfo typeInfo, final Set<Class<?>> visiting) {
		if (typeInfo == null) {
			return false;
		}
		final Kind kind = DeclaredTypeRules.kindOf(typeInfo);
		if (kind == Kind.LIST_OR_SET || kind == Kind.MAP) {
			return hasProtectedSubFields(DeclaredTypeRules.elementOf(typeInfo), visiting);
		}
		if (kind.isRefused()) {
			// A type that does not tell what it holds: refused when a model is analysed, so not reachable from
			// an entity. Counted as holding protected fields all the same (fail closed).
			return true;
		}
		if (kind != Kind.EMBEDDED) {
			return false;
		}
		final Class<?> raw = typeInfo.rawType();
		if (!visiting.add(raw)) {
			return false;
		}
		try {
			for (final DbPropertyDescriptor desc : DbClassModel.of(raw).getAllFields()) {
				if (isProtectedSubField(desc) || hasProtectedSubFields(desc.getProperty().getTypeInfo(), visiting)) {
					return true;
				}
			}
			return false;
		} catch (final IntrospectionException ex) {
			// Not reachable from an accepted model (an embedded class that cannot be analysed fails its root):
			// counted as holding protected fields all the same (fail closed).
			LOGGER.debug("Can not analyse the embedded type {}: {}", raw.getName(), ex.getMessage());
			return true;
		} finally {
			visiting.remove(raw);
		}
	}

	private static boolean isProtectedSubField(final DbPropertyDescriptor desc) {
		return desc.isNotRead() || desc.isEncrypted();
	}

	/**
	 * Writes the visible fields of an embedded object in dot notation ({@code path.field}), for a full update,
	 * with the model of the class of the value.
	 *
	 * @param path the dotted path of the embedded object in the document
	 * @param value the embedded object (not {@code null})
	 * @param docSet the {@code $set} document to fill
	 * @param docUnSet the {@code $unset} document to fill
	 * @throws Exception if the model cannot be analysed or a value cannot be converted
	 * @deprecated the class of the value may be a subclass of the declared type of the field, whose added fields
	 *             would be written: use
	 *             {@link #writeEmbeddedVisibleFields(String, DbPropertyDescriptor, Object, Document, Document)},
	 *             which writes by the declared type
	 */
	@Deprecated(since = "0.51.4")
	public static void writeEmbeddedVisibleFields(
			final String path,
			final Object value,
			final Document docSet,
			final Document docUnSet) throws Exception {
		writeEmbeddedVisibleFields(path, value, value.getClass(), null, docSet, docUnSet);
	}

	/**
	 * Writes the visible fields of an embedded object in dot notation ({@code path.field}), for a full update,
	 * with the model of the declared type of the field.
	 *
	 * <p>The protected fields ({@code @DataNotRead}, {@code @DataEncrypt}) are not written, so the values
	 * stored in the database stay. A visible field set to {@code null} is {@code $unset}. A nested embedded
	 * object holding protected fields is written the same way (or {@code $unset} when {@code null}); a nested
	 * list, set or map of such objects is not written at all. An object that is not stored yet is created by
	 * its first non-null visible field (when every visible field is {@code null}, nothing is created). A value of
	 * a subclass is written with the fields of the declared type only (a warning is logged once).
	 *
	 * @param path the dotted path of the embedded object in the document
	 * @param field the field holding the embedded object: its declared type gives the fields written
	 * @param value the embedded object (not {@code null})
	 * @param docSet the {@code $set} document to fill
	 * @param docUnSet the {@code $unset} document to fill
	 * @throws Exception if the model cannot be analysed or a value cannot be converted
	 */
	public static void writeEmbeddedVisibleFields(
			final String path,
			final DbPropertyDescriptor field,
			final Object value,
			final Document docSet,
			final Document docUnSet) throws Exception {
		writeEmbeddedVisibleFields(path, value, field.getProperty().getType(), "Field " + qualifiedName(field), docSet,
				docUnSet);
	}

	private static void writeEmbeddedVisibleFields(
			final String path,
			final Object value,
			final Class<?> declaredClass,
			final String site,
			final Document docSet,
			final Document docUnSet) throws Exception {
		checkDeclaredInstance(value, declaredClass, site);
		for (final DbPropertyDescriptor desc : DbClassModel.of(declaredClass).getAllFields()) {
			final MongoFieldCodec codec = desc.getCodec();
			if (codec == null || isProtectedSubField(desc)) {
				continue;
			}
			final TypeInfo typeInfo = desc.getProperty().getTypeInfo();
			if (!hasProtectedSubFields(typeInfo)) {
				codec.writeToDoc(path, value, docSet, docUnSet);
				continue;
			}
			if (!isEmbeddedObjectType(typeInfo)) {
				// List, set or map of objects with protected fields: kept as stored.
				continue;
			}
			final String subPath = path + "." + codec.getDbFieldName();
			final Object subValue = desc.getProperty().getValue(value);
			if (subValue == null) {
				docUnSet.append(subPath, "");
			} else {
				writeEmbeddedVisibleFields(subPath, subValue, typeInfo.rawType(), "Field " + qualifiedName(desc),
						docSet, docUnSet);
			}
		}
	}

	/**
	 * Name a field as {@code com.app.Owner.field}, with the class that declares it.
	 *
	 * @param desc the field
	 * @return the qualified name of the field
	 */
	public static String qualifiedName(final DbPropertyDescriptor desc) {
		final PropertyDescriptor prop = desc.getProperty();
		Class<?> owner = null;
		if (prop.getField() != null) {
			owner = prop.getField().getDeclaringClass();
		} else if (prop.getGetter() != null) {
			owner = prop.getGetter().getDeclaringClass();
		}
		return (owner == null ? "?" : owner.getName()) + "." + prop.getName();
	}

	// ========== Element codec builders ==========

	/**
	 * Extract the full generic Type for the element (value) of a List/Set/Map from the parent's genericType.
	 * For List/Set: returns the first type argument.
	 * For Map: returns the second type argument (the value type).
	 * Falls back to the raw elementType class if generic info is unavailable.
	 *
	 * @param parentTypeInfo the type of the list, set or map
	 * @return the type of its elements (values), or {@code null} when it cannot be known (raw type, wildcard,
	 *         type variable): see {@link DeclaredTypeRules#elementOf(TypeInfo)}
	 */
	public static TypeInfo resolveElementTypeInfo(final TypeInfo parentTypeInfo) {
		return DeclaredTypeRules.elementOf(parentTypeInfo);
	}

	/**
	 * Build a writer for collection/map element types, preserving full generic type info. An unknown element
	 * type (raw type, wildcard, type variable) gives a writer that refuses the elements.
	 */
	private static MongoTypeWriter buildElementWriterFromTypeInfo(final TypeInfo parentTypeInfo, final String site) {
		final TypeInfo elementTi = resolveElementTypeInfo(parentTypeInfo);
		if (elementTi == null) {
			return refusingWriter(parentTypeInfo);
		}
		return buildWriter(elementTi, site);
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
	 *
	 * @param keyType the Java type of the map keys (e.g. String, Integer, Long, enum)
	 * @return a converter for serializing/deserializing map keys to/from strings
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
