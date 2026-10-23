package org.atriasoft.archidata.db;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.atriasoft.archidata.annotation.DataEncrypt;
import org.atriasoft.archidata.annotation.DataNotRead;
import org.atriasoft.archidata.dataAccess.model.DeclaredTypeRules;
import org.atriasoft.archidata.exception.ProtectedFieldEncodingException;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.ClassModel;
import org.bson.codecs.pojo.ClassModelBuilder;
import org.bson.codecs.pojo.Convention;
import org.bson.codecs.pojo.Conventions;
import org.bson.codecs.pojo.PropertyModelBuilder;
import org.bson.codecs.pojo.PropertySerialization;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Keeps the codecs of the MongoDB driver from writing a field marked {@link DataNotRead} or {@link DataEncrypt}.
 *
 * <p>archidata writes an entity through its own codecs, which apply these annotations. The codecs of the driver
 * (automatic POJO codec, record codec) ignore them: an application object handed to the driver directly (a typed
 * collection, a value of a {@code Document}, of {@code Updates} or of {@code Filters}) would be written with its
 * {@code @DataNotRead} fields and its {@code @DataEncrypt} fields in clear. The client of {@link DbIoMongo} refuses
 * that encoding with a {@link ProtectedFieldEncodingException}:
 * <ul>
 * <li>a POJO is refused by a convention of the POJO codec: when its class holds such a field, at any depth reachable
 * through the declared types of the properties the driver writes, every property but a plain primary key throws when
 * the driver is about to write it, its value {@code null} included. The properties are those of the class model of the
 * driver (default conventions): a public field, a field or a property with a public getter (checked through the
 * field and the return type of the getter), of the class or of a superclass. A field the driver does not write
 * (transient, marked {@code @BsonIgnore}, private without getter) is not checked. A field typed by a type variable is
 * checked by the class of its value, when that value is encoded;</li>
 * <li>a record is refused by a wrapper of the codec registry, on the same rule; the record codec writes every
 * component.</li>
 * </ul>
 *
 * <p>Decoding is unchanged: the convention only replaces the serialization test of the properties (used by the
 * encoder alone, the inline flag of {@code @BsonExtraElements} is kept), and the record wrapper hands decoding to
 * the record codec. A class whose properties cannot be inspected is refused. The client that
 * {@code ChangeNotificationManager} opens for its change streams has no such guard: it only reads.
 */
final class ProtectedFieldCodecGuard {
	private static final Logger LOGGER = LoggerFactory.getLogger(ProtectedFieldCodecGuard.class);

	/** Classes of the driver, never models of the application. */
	private static final String MONGODB_PACKAGE = "com.mongodb.";

	/** Remedy given by every refusal. */
	private static final String REMEDY = "Write the object through DataAccess (insert, updateById...), which applies"
			+ " these annotations, or pass a Document of plain values.";

	/**
	 * A protected field reached from a class.
	 *
	 * @param path dotted path of the field from the class
	 * @param declaringClass class declaring the field
	 * @param fieldName name of the field
	 * @param annotation {@link DataNotRead} or {@link DataEncrypt}; {@code null} when the fields could not be
	 *            inspected
	 * @param failure why the fields of {@code declaringClass} could not be inspected, or {@code null}
	 */
	private record Finding(
			String path,
			Class<?> declaringClass,
			String fieldName,
			Class<? extends Annotation> annotation,
			String failure) {}

	/** First protected field reached from each class (empty when none), computed once per class. */
	private static final ClassValue<Optional<Finding>> FINDINGS = new ClassValue<>() {
		@Override
		protected Optional<Finding> computeValue(final Class<?> type) {
			return inspect(type, "");
		}
	};

	private ProtectedFieldCodecGuard() {}

	/**
	 * Gives the conventions of the automatic POJO codec: the default ones of the driver, then the refusal of the
	 * protected fields.
	 *
	 * @return the conventions, in the order they apply
	 */
	static List<Convention> conventions() {
		final List<Convention> out = new ArrayList<>(Conventions.DEFAULT_CONVENTIONS);
		out.add(new RefuseProtectedFields());
		return Collections.unmodifiableList(out);
	}

	/**
	 * Wraps a codec registry so that the codec of a record holding a protected field refuses to encode it.
	 *
	 * @param registry the registry of the client
	 * @return the guarded registry
	 */
	static CodecRegistry guardRecords(final CodecRegistry registry) {
		return CodecRegistries.fromProviders(new RecordGuard(registry));
	}

	// ========== Inspection ==========

	private static Optional<Finding> findingOf(final Class<?> type) {
		if (!isInspected(type)) {
			return Optional.empty();
		}
		return FINDINGS.get(type);
	}

	/** Tells whether a class is a model of the application, whose fields the driver writes one by one. */
	private static boolean isInspected(final Class<?> type) {
		return DeclaredTypeRules.isApplicationClass(type) && !type.isEnum() && !type.isInterface()
				&& !type.isAnnotation() && !type.getName().startsWith(MONGODB_PACKAGE);
	}

	private static Optional<Finding> inspect(final Type start, final String path) {
		try {
			return walkType(start, path, new HashSet<>());
		} catch (final RuntimeException | LinkageError ex) {
			final Class<?> raw = start instanceof final Class<?> clazz ? clazz : Object.class;
			LOGGER.debug("The fields reached from {} could not be inspected for protected fields", start, ex);
			return Optional.of(new Finding(path, raw, null, null, ex.toString()));
		}
	}

	private static Optional<Finding> walkType(final Type type, final String path, final Set<Class<?>> visited) {
		for (final Class<?> reached : reachedClasses(type)) {
			final Optional<Finding> found = walk(reached, path, visited);
			if (found.isPresent()) {
				return found;
			}
		}
		return Optional.empty();
	}

	private static Optional<Finding> walk(final Class<?> type, final String prefix, final Set<Class<?>> visited) {
		if (!isInspected(type) || !visited.add(type)) {
			return Optional.empty();
		}
		final List<WrittenProperty> properties = writtenProperties(type);
		for (final WrittenProperty property : properties) {
			if (property.annotation() != null) {
				return Optional.of(property.finding(prefix));
			}
		}
		for (final WrittenProperty property : properties) {
			final Optional<Finding> below = property.findingBelow(prefix, visited);
			if (below.isPresent()) {
				return below;
			}
		}
		return Optional.empty();
	}

	/**
	 * A property the codec of the driver writes.
	 *
	 * @param name name of the property
	 * @param declaringClass class declaring its field, or the inspected class for a property without field
	 * @param annotation {@link DataNotRead} or {@link DataEncrypt} on the property, or {@code null}
	 * @param types declared types of its value (its field, its public getter); empty when it has neither, which the
	 *            driver cannot write
	 */
	private record WrittenProperty(
			String name,
			Class<?> declaringClass,
			Class<? extends Annotation> annotation,
			List<Type> types) {
		/** The finding of the property itself, marked {@link DataNotRead} or {@link DataEncrypt}. */
		Finding finding(final String prefix) {
			return new Finding(join(prefix, this.name), this.declaringClass, this.name, this.annotation, null);
		}

		/** The first protected field reached through the declared types of the property. */
		Optional<Finding> findingBelow(final String prefix, final Set<Class<?>> visited) {
			// Without a field nor a public getter, the accessor of the driver throws before writing anything.
			final String path = join(prefix, this.name);
			for (final Type type : this.types) {
				final Optional<Finding> below = walkType(type, path, visited);
				if (below.isPresent()) {
					return below;
				}
			}
			return Optional.empty();
		}
	}

	/**
	 * The properties the codec of the driver writes for a class: every component of a record (the record codec
	 * writes them all); for another class, the readable properties of the class model the POJO codec builds with
	 * the default conventions of the driver (a public field, or a field with a public getter, or a public getter,
	 * of the class or of a superclass). A transient field, a field marked {@code @BsonIgnore} and a private field
	 * without getter are left out: the driver does not write them.
	 */
	private static List<WrittenProperty> writtenProperties(final Class<?> type) {
		final List<WrittenProperty> out = new ArrayList<>();
		if (type.isRecord()) {
			for (final RecordComponent component : type.getRecordComponents()) {
				final Field field = fieldNamed(type, component.getName());
				Class<? extends Annotation> annotation = field != null ? protection(field) : null;
				if (annotation == null) {
					annotation = protection(Arrays.asList(component.getAnnotations()));
				}
				out.add(new WrittenProperty(component.getName(), type, annotation,
						List.of(component.getGenericType())));
			}
			return out;
		}
		final ClassModelBuilder<?> builder = ClassModel.builder(type);
		for (final Convention convention : Conventions.DEFAULT_CONVENTIONS) {
			convention.apply(builder);
		}
		for (final PropertyModelBuilder<?> property : builder.getPropertyModelBuilders()) {
			if (property.isReadable()) {
				out.add(writtenProperty(type, property));
			}
		}
		return out;
	}

	/** A property of the class model of the POJO codec, with its protection and its declared types. */
	private static WrittenProperty writtenProperty(final Class<?> type, final PropertyModelBuilder<?> property) {
		final String name = property.getName();
		final Field field = fieldNamed(type, name);
		Class<? extends Annotation> annotation = protection(property.getReadAnnotations());
		if (annotation == null) {
			annotation = protection(property.getWriteAnnotations());
		}
		if (annotation == null && field != null) {
			annotation = protection(field);
		}
		return new WrittenProperty(name, field != null ? field.getDeclaringClass() : type, annotation,
				declaredTypes(type, name));
	}

	private static Class<? extends Annotation> protection(final Field field) {
		if (field.isAnnotationPresent(DataNotRead.class)) {
			return DataNotRead.class;
		}
		if (field.isAnnotationPresent(DataEncrypt.class)) {
			return DataEncrypt.class;
		}
		return null;
	}

	private static Class<? extends Annotation> protection(final List<Annotation> annotations) {
		for (final Annotation annotation : annotations) {
			if (annotation.annotationType() == DataNotRead.class || annotation.annotationType() == DataEncrypt.class) {
				return annotation.annotationType();
			}
		}
		return null;
	}

	/** Every class named by a type: the class itself, the type arguments, the bounds, the array components. */
	private static Set<Class<?>> reachedClasses(final Type type) {
		final Set<Class<?>> out = new LinkedHashSet<>();
		collect(type, out, new HashSet<>());
		return out;
	}

	private static void collect(final Type type, final Set<Class<?>> out, final Set<Type> seen) {
		if (type == null || !seen.add(type)) {
			return;
		}
		if (type instanceof final Class<?> clazz) {
			out.add(DeclaredTypeRules.innermostComponent(clazz));
		} else if (type instanceof final ParameterizedType parameterized) {
			collect(parameterized.getRawType(), out, seen);
			for (final Type argument : parameterized.getActualTypeArguments()) {
				collect(argument, out, seen);
			}
		} else if (type instanceof final GenericArrayType array) {
			collect(array.getGenericComponentType(), out, seen);
		} else if (type instanceof final WildcardType wildcard) {
			for (final Type bound : wildcard.getUpperBounds()) {
				collect(bound, out, seen);
			}
			for (final Type bound : wildcard.getLowerBounds()) {
				collect(bound, out, seen);
			}
		} else if (type instanceof final TypeVariable<?> variable) {
			for (final Type bound : variable.getBounds()) {
				collect(bound, out, seen);
			}
		}
	}

	private static String join(final String prefix, final String name) {
		return prefix.isEmpty() ? name : prefix + "." + name;
	}

	/** The field of this name in the class or its superclasses, or {@code null}. */
	private static Field fieldNamed(final Class<?> type, final String name) {
		for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
			for (final Field field : current.getDeclaredFields()) {
				if (field.getName().equals(name) && !Modifier.isStatic(field.getModifiers())) {
					return field;
				}
			}
		}
		return null;
	}

	/** The declared types of a property: its field and its public getter, those found. */
	private static List<Type> declaredTypes(final Class<?> type, final String name) {
		final List<Type> out = new ArrayList<>();
		final Field field = fieldNamed(type, name);
		if (field != null) {
			out.add(field.getGenericType());
		}
		for (final Method method : type.getMethods()) {
			if (method.getParameterCount() == 0 && !Modifier.isStatic(method.getModifiers())
					&& name.equals(getterPropertyName(method.getName()))) {
				out.add(method.getGenericReturnType());
			}
		}
		return out;
	}

	/**
	 * The property a getter names, by the rule of the driver: {@code get} or {@code is} followed by an upper case
	 * character, lowered; {@code null} for another name.
	 */
	private static String getterPropertyName(final String methodName) {
		for (final String prefix : List.of("get", "is")) {
			if (methodName.length() > prefix.length() && methodName.startsWith(prefix)
					&& Character.isUpperCase(methodName.charAt(prefix.length()))) {
				return Character.toLowerCase(methodName.charAt(prefix.length()))
						+ methodName.substring(prefix.length() + 1);
			}
		}
		return null;
	}

	/** The protected field a property is, or leads to through its declared types. */
	private static Optional<Finding> propertyFinding(final Class<?> type, final PropertyModelBuilder<?> property) {
		final String name = property.getName();
		try {
			final WrittenProperty written = writtenProperty(type, property);
			if (written.annotation() != null) {
				return Optional.of(written.finding(""));
			}
			return written.findingBelow("", new HashSet<>());
		} catch (final RuntimeException | LinkageError ex) {
			// The convention must not throw: the POJO codec would then neither encode nor decode the class.
			LOGGER.debug("The property {}.{} could not be inspected for protected fields", type.getName(), name, ex);
			return Optional.of(new Finding(name, type, name, null, ex.toString()));
		}
	}

	private static String message(final Class<?> type, final Finding finding) {
		if (finding.annotation() == null) {
			return "The MongoDB driver refuses to encode " + type.getName() + ": the fields reached through '"
					+ finding.path() + "' could not be inspected (" + finding.failure()
					+ "), so they cannot be checked for @DataNotRead or @DataEncrypt. " + REMEDY;
		}
		final String consequence = finding.annotation() == DataEncrypt.class ? "the value would be written in clear"
				: "the value would be written like any other field";
		return "The MongoDB driver refuses to encode " + type.getName() + ": its field '" + finding.path() + "' ("
				+ finding.declaringClass().getName() + "." + finding.fieldName() + ") is marked @"
				+ finding.annotation().getSimpleName() + ", which the codecs of the driver ignore (" + consequence
				+ "). " + REMEDY;
	}

	// ========== POJO: convention ==========

	/** Convention of the POJO codec: each property of a class holding a protected field refuses to be written. */
	private static final class RefuseProtectedFields implements Convention {
		@Override
		public void apply(final ClassModelBuilder<?> classModelBuilder) {
			final Class<?> type = classModelBuilder.getType();
			if (!isInspected(type)) {
				return;
			}
			final Optional<Finding> classFinding = findingOf(type);
			final String idName = classModelBuilder.getIdPropertyName();
			final List<String> refused = new ArrayList<>();
			for (final PropertyModelBuilder<?> property : classModelBuilder.getPropertyModelBuilders()) {
				final Optional<Finding> own = propertyFinding(type, property);
				final Optional<Finding> applied;
				if (own.isPresent()) {
					applied = own;
				} else if (property.getName().equals(idName)) {
					// A plain primary key is written first: leave it, the next property refuses.
					applied = Optional.empty();
				} else {
					applied = classFinding;
				}
				if (applied.isPresent()) {
					refuse(property, message(type, applied.get()));
					refused.add(applied.get().path());
				}
			}
			if (refused.isEmpty()) {
				LOGGER.debug("MongoDB driver codec for application class {}: no protected field", type.getName());
			} else {
				LOGGER.debug("MongoDB driver codec for application class {}: refuses to encode it ({})", type.getName(),
						classFinding.map(Finding::path).orElse(refused.get(0)));
			}
		}

		private static <T> void refuse(final PropertyModelBuilder<T> property, final String message) {
			property.propertySerialization(new RefusingSerialization<>(property.getPropertySerialization(), message));
		}
	}

	/**
	 * Serialization test that refuses every value.
	 *
	 * @param previous the serialization it replaces, kept for its inline flag (read by the decoder)
	 * @param message the refusal
	 */
	private record RefusingSerialization<T>(
			PropertySerialization<T> previous,
			String message)
			implements PropertySerialization<T> {
		@Override
		public boolean shouldSerialize(final T value) {
			throw new ProtectedFieldEncodingException(this.message);
		}

		@Override
		public boolean inline() {
			return this.previous != null && this.previous.inline();
		}
	}

	// ========== Record: registry wrapper ==========

	/** Provider over the registry of the client: the codec of a record holding a protected field refuses to encode. */
	private static final class RecordGuard implements CodecProvider {
		private final CodecRegistry delegate;

		RecordGuard(final CodecRegistry delegate) {
			this.delegate = delegate;
		}

		@Override
		public <T> Codec<T> get(final Class<T> clazz, final CodecRegistry registry) {
			return guard(clazz, this.delegate.get(clazz, registry));
		}

		@Override
		public <T> Codec<T> get(final Class<T> clazz, final List<Type> typeArguments, final CodecRegistry registry) {
			return guard(clazz, this.delegate.get(clazz, typeArguments, registry));
		}

		private static <T> Codec<T> guard(final Class<T> clazz, final Codec<T> codec) {
			if (codec == null || !clazz.isRecord() || !isInspected(clazz)) {
				return codec;
			}
			final Optional<Finding> finding = findingOf(clazz);
			if (finding.isEmpty()) {
				LOGGER.debug("MongoDB driver codec for application record {}: no protected field", clazz.getName());
				return codec;
			}
			LOGGER.debug("MongoDB driver codec for application record {}: refuses to encode it ({})", clazz.getName(),
					finding.get().path());
			return new RefusingEncoderCodec<>(codec, message(clazz, finding.get()));
		}
	}

	/**
	 * Codec that refuses to encode and decodes through the codec it wraps.
	 *
	 * @param delegate the codec of the driver
	 * @param message the refusal
	 */
	private record RefusingEncoderCodec<T>(
			Codec<T> delegate,
			String message)
			implements Codec<T> {
		@Override
		public void encode(final BsonWriter writer, final T value, final EncoderContext encoderContext) {
			throw new ProtectedFieldEncodingException(this.message);
		}

		@Override
		public T decode(final BsonReader reader, final DecoderContext decoderContext) {
			return this.delegate.decode(reader, decoderContext);
		}

		@Override
		public Class<T> getEncoderClass() {
			return this.delegate.getEncoderClass();
		}
	}
}
