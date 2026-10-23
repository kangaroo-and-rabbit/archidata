package org.atriasoft.archidata.dataAccess.model;

import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.atriasoft.archidata.bean.TypeInfo;
import org.atriasoft.archidata.bean.exception.IntrospectionException;
import org.atriasoft.archidata.exception.DataAccessException;
import org.bson.types.ObjectId;

/**
 * How archidata stores a field, decided by its <strong>declared</strong> type, and the types it refuses.
 *
 * <p>An embedded object is written and read with the model of its declared type (the type of the field, the
 * element type of a {@code List} / {@code Set}, the value type of a {@code Map}), at any depth: a value of a
 * subclass is stored with the fields of the declared type only. A declared type that does not tell which
 * fields are stored ({@code Object}, an interface, an abstract class, a raw or unresolved {@code List} /
 * {@code Set} / {@code Map}, another kind of collection) is therefore refused when the model of an entity is
 * analysed ({@link DbClassModel#of(Class)}), with a message naming the class, the field, the declared type
 * and the replacement. One exemption: a primary key declared {@code Object} (its type is given on each call
 * by an {@code OptionSpecifyType}), whose value is checked when it is written.
 *
 * <p>The same rules are shared by the codec, the field path cursor of the filter guard, the TRACE masking and
 * the change journal, and by the projection of the reads.
 */
public final class DeclaredTypeRules {

	/** How a declared type is stored. */
	public enum Kind {
		/** A single BSON value: a primitive or boxed type, {@code String}, an id, a date or time, an enum. */
		VALUE,
		/** A BSON type ({@code org.bson.Document}...): free-form content, stored and read as it is. */
		FREE_FORM,
		/**
		 * A class of the JDK or of Jackson that is not a value type ({@code BigDecimal}, {@code LocalDateTime},
		 * {@code Optional}...), or an array of such classes or of values: not modeled by archidata, neither walked
		 * nor refused. A value of a class of the application is refused there when it is written, and a path below
		 * such a position is unsafe for the filter guard.
		 */
		OPAQUE,
		/** A {@code List} or a {@code Set} whose element type is known. */
		LIST_OR_SET,
		/** A {@code Map} whose value type is known (the keys are stored as strings). */
		MAP,
		/** A concrete class of the application: an embedded document, written and read by its declared fields. */
		EMBEDDED,
		/** Refused: {@code Object}, an interface or an abstract class (it can hold any class). */
		POLYMORPHIC,
		/** Refused: a raw {@code List} / {@code Set} / {@code Map}, or one whose element type cannot be resolved. */
		UNRESOLVED,
		/**
		 * Refused: a collection that is neither a {@code List} nor a {@code Set}, or an array of a class of the
		 * application or of a type that can hold any class.
		 */
		UNSUPPORTED_COLLECTION;

		/**
		 * Tells whether a persisted field declared with this kind is refused.
		 *
		 * @return {@code true} for {@link #POLYMORPHIC}, {@link #UNRESOLVED} and {@link #UNSUPPORTED_COLLECTION}
		 */
		public boolean isRefused() {
			return this == POLYMORPHIC || this == UNRESOLVED || this == UNSUPPORTED_COLLECTION;
		}
	}

	/** Types written as a single BSON value by the codec (besides the primitives and the enums). */
	private static final Set<Class<?>> VALUE_CLASSES = Set.of(Long.class, Integer.class, Float.class, Double.class,
			Boolean.class, Short.class, Character.class, String.class, ObjectId.class, UUID.class, Date.class,
			Instant.class, OffsetDateTime.class, LocalDate.class, LocalTime.class);

	/** Packages whose classes are never modeled as embedded objects. */
	private static final String[] NOT_MODELED_PACKAGES = { "java.", "javax.", "jdk.", "sun.", "com.sun.",
			"com.fasterxml." };

	/** Package of the BSON types, accepted as free-form content. */
	private static final String BSON_PACKAGE = "org.bson.";

	/** Segment written in a path of a refusal message for the key of a map. */
	private static final String MAP_KEY_SEGMENT = "<key>";

	private DeclaredTypeRules() {}

	/**
	 * Tells how a field of this declared type is stored.
	 *
	 * @param typeInfo the declared type
	 * @return its kind
	 */
	public static Kind kindOf(final TypeInfo typeInfo) {
		final Class<?> raw = typeInfo.rawType();
		if (raw.isPrimitive() || raw.isEnum() || VALUE_CLASSES.contains(raw)) {
			return Kind.VALUE;
		}
		if (raw.getName().startsWith(BSON_PACKAGE)) {
			return Kind.FREE_FORM;
		}
		if (raw.isArray()) {
			// Written as a whole by its own class, never element by element: an array of modeled (or of any)
			// classes would not be stored by its declared component type.
			final Kind component = kindOf(TypeInfo.ofRaw(innermostComponent(raw)));
			return component == Kind.EMBEDDED || component.isRefused() ? Kind.UNSUPPORTED_COLLECTION : Kind.OPAQUE;
		}
		if (typeInfo.isList() || typeInfo.isSet()) {
			return elementOf(typeInfo) == null ? Kind.UNRESOLVED : Kind.LIST_OR_SET;
		}
		if (typeInfo.isMap()) {
			return elementOf(typeInfo) == null ? Kind.UNRESOLVED : Kind.MAP;
		}
		if (typeInfo.isCollection() || raw == Iterable.class) {
			return Kind.UNSUPPORTED_COLLECTION;
		}
		if (raw == Object.class || raw.isInterface() || Modifier.isAbstract(raw.getModifiers())) {
			return Kind.POLYMORPHIC;
		}
		if (!isApplicationClass(raw)) {
			return Kind.OPAQUE;
		}
		return Kind.EMBEDDED;
	}

	/**
	 * Gives the element type of a {@code List} / {@code Set}, or the value type of a {@code Map}.
	 *
	 * @param parent the type of the list, set or map
	 * @return the element (value) type, or {@code null} when it is unknown: a raw type, a wildcard
	 *         ({@code List<?>}, {@code List<? extends X>}), a type variable ({@code List<T>}) or a generic array
	 */
	public static TypeInfo elementOf(final TypeInfo parent) {
		final Type generic = parent.genericType();
		if (generic instanceof final ParameterizedType pt) {
			final Type[] args = pt.getActualTypeArguments();
			final int index = parent.isMap() ? 1 : 0;
			if (args.length <= index) {
				return null;
			}
			final Type arg = args[index];
			if (arg instanceof Class<?> || arg instanceof ParameterizedType) {
				return TypeInfo.fromType(arg);
			}
			return null;
		}
		final Class<?> elementType = parent.elementType();
		if (elementType == null || parent.isArray()) {
			return null;
		}
		return TypeInfo.ofRaw(elementType);
	}

	/**
	 * Tells whether a class is one of the application, modeled field by field when it is embedded: not a class
	 * of the JDK, of BSON or of Jackson, not a primitive type and not an array.
	 *
	 * @param clazz the class
	 * @return {@code true} for a class of the application
	 */
	public static boolean isApplicationClass(final Class<?> clazz) {
		if (clazz.isPrimitive() || clazz.isArray()) {
			return false;
		}
		final String name = clazz.getName();
		if (name.startsWith(BSON_PACKAGE)) {
			return false;
		}
		for (final String prefix : NOT_MODELED_PACKAGES) {
			if (name.startsWith(prefix)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Gives the component type of an array, through every dimension ({@code Foo[][]} gives {@code Foo}).
	 *
	 * @param clazz a class, an array class or not
	 * @return the innermost component type, or the class itself when it is not an array
	 */
	public static Class<?> innermostComponent(final Class<?> clazz) {
		Class<?> out = clazz;
		while (out.isArray()) {
			out = out.getComponentType();
		}
		return out;
	}

	// ========== Primary key declared Object ==========

	/** Classes a primary key declared {@code Object} may hold. */
	private static final Set<Class<?>> PRIMARY_KEY_VALUE_CLASSES = Set.of(ObjectId.class, UUID.class, String.class,
			Long.class, Integer.class, Short.class);

	/**
	 * Tells whether a field is a primary key declared {@code Object}: its type is given on each call by an
	 * {@code OptionSpecifyType}, so it is exempt from the refusal of the polymorphic types, and its value is
	 * checked when it is written ({@link #checkPrimaryKeyValue(Object, String)}).
	 *
	 * @param desc the field
	 * @return {@code true} for an {@code @Id} declared {@code java.lang.Object}
	 */
	public static boolean isUntypedPrimaryKey(final DbPropertyDescriptor desc) {
		return desc.getAction() == DbFieldAction.PRIMARY_KEY && desc.getProperty().getType() == Object.class;
	}

	/**
	 * Checks the value written in a primary key declared {@code Object}: an {@code ObjectId}, a {@code UUID}, a
	 * {@code String}, a {@code Long}, an {@code Integer} or a {@code Short}. Any other value (a class of the
	 * application would be stored with all its fields) is refused.
	 *
	 * @param value the value of the key (not {@code null})
	 * @param site how the field is named in the message ({@code "Field com.app.Owner._id"})
	 * @throws DataAccessException when the value is of another class
	 */
	public static void checkPrimaryKeyValue(final Object value, final String site) throws DataAccessException {
		if (!PRIMARY_KEY_VALUE_CLASSES.contains(value.getClass())) {
			throw new DataAccessException(site + " is a primary key declared java.lang.Object but holds a "
					+ value.getClass().getName() + ": such a key only takes an ObjectId, a UUID, a String, a Long, an"
					+ " Integer or a Short");
		}
	}

	// ========== Refusal at model analysis ==========

	/**
	 * Checks the declared type of every persisted field of an entity, at the root and in every embedded class
	 * reachable from it (fields of embedded classes, element types of lists and sets, value types of maps, at any
	 * depth). The relation fields (handled by an add-on), the fields that are not written (no getter) and a
	 * primary key declared {@code Object} ({@link #isUntypedPrimaryKey}) are skipped. A class that is not one of
	 * the application (a JDK class modeled by a writer) is not checked. An
	 * abstract class or an interface of the application is checked like a concrete one: it may be the root of
	 * a filter check, of the TRACE masking or of the journal.
	 *
	 * @param model the model of the entity
	 * @throws DataAccessException on the first refused field, with a message naming the class, the field, the
	 *         declared type and the replacement
	 * @throws IntrospectionException if the model of an embedded class cannot be built
	 */
	static void checkDeclaredTypes(final DbClassModel model) throws DataAccessException, IntrospectionException {
		final Class<?> root = model.getClassModel().getClassType();
		if (!isModeledRoot(root)) {
			return;
		}
		final Set<Class<?>> visited = new HashSet<>();
		visited.add(root);
		checkFields(root, model, "", visited);
	}

	/**
	 * Tells whether a class is modeled field by field as the root of a document: a class of the application,
	 * concrete, abstract or an interface (not a value, an enum or a collection).
	 *
	 * @param root the class
	 * @return {@code true} when its fields are walked and checked
	 */
	static boolean isModeledRoot(final Class<?> root) {
		if (!isApplicationClass(root)) {
			return false;
		}
		final Kind kind = kindOf(TypeInfo.ofRaw(root));
		return kind == Kind.EMBEDDED || kind == Kind.POLYMORPHIC;
	}

	private static void checkFields(
			final Class<?> root,
			final DbClassModel model,
			final String prefix,
			final Set<Class<?>> visited) throws DataAccessException, IntrospectionException {
		for (final DbPropertyDescriptor desc : model.getAllFields()) {
			if (desc.getCodec() == null || desc.getAddOn() != null || isUntypedPrimaryKey(desc)) {
				continue;
			}
			final TypeInfo declared = desc.getProperty().getTypeInfo();
			checkType(new Site(root, model, desc, declared), prefix + desc.getDbFieldName(), declared, null, visited);
		}
	}

	/**
	 * @param role {@code null} for the type of the field itself, {@code "element"} or {@code "value"} for the
	 *        element type of a list or a set, or the value type of a map
	 */
	private static void checkType(
			final Site site,
			final String path,
			final TypeInfo current,
			final String role,
			final Set<Class<?>> visited) throws DataAccessException, IntrospectionException {
		final Kind kind = kindOf(current);
		switch (kind) {
			case LIST_OR_SET -> checkType(site, path, elementOf(current), "element", visited);
			case MAP -> checkType(site, path + "." + MAP_KEY_SEGMENT, elementOf(current), "value", visited);
			case EMBEDDED -> {
				final Class<?> raw = current.rawType();
				if (visited.add(raw)) {
					checkFields(site.root(), DbClassModel.ofUnchecked(raw), path + ".", visited);
				}
			}
			case POLYMORPHIC, UNRESOLVED, UNSUPPORTED_COLLECTION -> throw new DataAccessException(
					refusalMessage(site, path, current, role, kind));
			default -> {
				// VALUE, FREE_FORM, OPAQUE: nothing below is modeled.
			}
		}
	}

	/** The field being checked, for the refusal message. */
	private record Site(
			Class<?> root,
			DbClassModel owner,
			DbPropertyDescriptor field,
			TypeInfo declared) {}

	private static String refusalMessage(
			final Site site,
			final String path,
			final TypeInfo offending,
			final String role,
			final Kind kind) {
		final StringBuilder out = new StringBuilder();
		out.append("Unsupported field type on ").append(site.root().getName()).append(": field ")
				.append(site.owner().getClassModel().getSimpleName()).append('.')
				.append(site.field().getProperty().getName()).append(" (path ").append(path).append(") is declared ")
				.append(site.declared().genericType().getTypeName());
		if (role == null) {
			out.append(", which ");
		} else {
			out.append(", whose ").append(role).append(" type ").append(offending.genericType().getTypeName())
					.append(' ');
		}
		switch (kind) {
			case POLYMORPHIC -> out.append(
					"can hold any class (Object, an interface or an abstract class), and archidata stores and reads"
							+ " an embedded value by its declared type only; declare a concrete class (or a List, Set or"
							+ " Map of one), or org.bson.Document for free-form content");
			case UNRESOLVED -> {
				final String inner = offending.isMap() ? "value" : "element";
				if (offending.genericType() instanceof ParameterizedType) {
					out.append("has an ").append(inner)
							.append(" type that cannot be resolved (a wildcard or a type variable)");
				} else {
					out.append("is a raw type whose ").append(inner).append(" type is unknown");
				}
				out.append(", and archidata stores and reads a value by its declared type only; declare the ")
						.append(inner).append(" type as a concrete class (List<MyClass>, Set<MyClass>,")
						.append(" Map<String, MyClass>), or use org.bson.Document for free-form content");
			}
			default -> {
				if (offending.isArray()) {
					out.append("is an array of a class of the application (or of a type that can hold any class),"
							+ " which archidata does not store element by element: only List, Set and Map are stored as"
							+ " collections; declare a List or a Set");
				} else {
					out.append("is a collection that is neither a List nor a Set: only List, Set and Map are stored"
							+ " as collections; declare a List or a Set");
				}
			}
		}
		out.append(" (see MIGRATION.md)");
		return out.toString();
	}
}
