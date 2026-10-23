package org.atriasoft.archidata.dataAccess.model;

import org.atriasoft.archidata.bean.TypeInfo;
import org.atriasoft.archidata.bean.exception.IntrospectionException;
import org.atriasoft.archidata.dataAccess.model.codec.MongoCodecFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Position inside the document of an entity, followed segment by segment through the embedded objects, the
 * lists, the sets and the maps. It is the one shared way to know where the protected fields
 * ({@code @DataNotRead}, {@code @DataEncrypt}) of embedded objects are: the filter guard
 * ({@code Filters.checkFieldName}), the TRACE log masking and the change journal all walk the model with it.
 *
 * <p>It follows the same type rules as {@link MongoCodecFactory#hasProtectedSubFields(TypeInfo)} and the
 * sub-object reader: a field typed by a class that is not a plain value is an embedded object, its fields
 * are resolved by their database name; after a list or a set the cursor is on the element type; after a
 * map, a segment is a key (any string) and the cursor is on the value type. The model is walked lazily, so a
 * recursive class costs nothing, and the number of steps is bounded by {@link #MAX_DEPTH}.
 *
 * <p>Two ways to step, because a segment does not mean the same thing in a path and in a document:
 * <ul>
 * <li>{@link #step(String)} / {@link #path(String)}: a segment of a dotted path as written in a filter or
 * a sort. After a list, a numeric segment is an array index and is skipped.</li>
 * <li>{@link #member(String)}: a key of a document read from, or written to, the database: never an
 * index.</li>
 * </ul>
 *
 * <p>Protection follows the <strong>declared</strong> type, as the codec does: an embedded value is written and
 * read with the fields of its declared type only ({@link DeclaredTypeRules}), so a field added by a subclass is
 * never stored, and a model holding a type that can hold any class ({@code Object}, an interface, an abstract
 * class) or a raw list, set or map is refused when it is analysed. Such positions cannot be met any more; the
 * cursor still answers {@link Status#UNSAFE} for them (defence in depth, fail closed). A key stored before
 * (by an older version, which wrote the fields of the subclass) is unknown to the model: {@link Status#UNKNOWN}.
 *
 * <p>The cursor is immutable. Once it is not {@link Status#OK}, or is free-form, a further step returns
 * the same cursor.
 */
public final class FieldPathCursor {
	private static final Logger LOGGER = LoggerFactory.getLogger(FieldPathCursor.class);

	/** Maximum number of steps followed from the root: a deeper position is {@link Status#UNSAFE}. */
	public static final int MAX_DEPTH = 64;

	/** Outcome of the steps made so far. */
	public enum Status {
		/** The position is valid (possibly free-form). */
		OK,
		/** The last segment names no field of an embedded or entity class that is modeled. */
		UNKNOWN,
		/** A protected field ({@code @DataNotRead} or {@code @DataEncrypt}) was reached. */
		PROTECTED,
		/**
		 * The path is too deep, ambiguous, or below a type archidata does not model (a polymorphic type, a class of
		 * the JDK that is not a value, an array): it must be treated like a protected one (fail closed).
		 */
		UNSAFE
	}

	private final TypeInfo type;
	private final DbPropertyDescriptor field;
	private final Status status;
	private final boolean freeForm;
	private final int depth;

	private FieldPathCursor(final TypeInfo type, final DbPropertyDescriptor field, final Status status,
			final boolean freeForm, final int depth) {
		this.type = type;
		this.field = field;
		this.status = status;
		this.freeForm = freeForm;
		this.depth = depth;
	}

	/**
	 * Starts at the document of an entity.
	 *
	 * @param entity the entity class
	 * @return the cursor on the root of its documents
	 * @throws IntrospectionException if the model of the entity cannot be analysed
	 */
	public static FieldPathCursor of(final Class<?> entity) throws IntrospectionException {
		DbClassModel.of(entity);
		return new FieldPathCursor(TypeInfo.ofRaw(entity), null, Status.OK, false, 0);
	}

	/**
	 * Outcome of the steps made so far.
	 *
	 * @return the status
	 */
	public Status status() {
		return this.status;
	}

	/**
	 * Descriptor of the field reached by the last step: the field that was found, also when it is
	 * {@link Status#PROTECTED}. {@code null} at the root, after an array index or a map key, and when the
	 * last step did not name a field.
	 *
	 * @return the descriptor, or {@code null}
	 */
	public DbPropertyDescriptor field() {
		return this.field;
	}

	/**
	 * Number of steps made from the root. {@code 0} on the root itself.
	 *
	 * @return the depth
	 */
	public int depth() {
		return this.depth;
	}

	/**
	 * Tells whether the position holds free-form content (a value, {@code org.bson.Document}, a relation key, a
	 * primary key declared {@code Object}):
	 * any further segment is accepted and nothing below is known as protected.
	 *
	 * @return {@code true} for a free-form position
	 */
	public boolean isFreeForm() {
		return this.freeForm;
	}

	/**
	 * Tells whether the position holds, at any depth, a protected field: it is an embedded object (or a
	 * list, a set, a map of them) that has {@code @DataNotRead} or {@code @DataEncrypt} fields, at any depth.
	 * Comparing, sorting or projecting such a position as a whole would reach those fields.
	 *
	 * <p>A position the model cannot resolve below (a polymorphic type, a raw list, set or map) cannot be met:
	 * a model holding one is refused when it is analysed ({@link DeclaredTypeRules}). Should one be reached all
	 * the same, it counts as holding protected fields (fail closed).
	 *
	 * @return {@code true} when protected fields lie below the position
	 */
	public boolean holdsProtectedSubFields() {
		if (this.status != Status.OK || this.freeForm || this.type == null) {
			return false;
		}
		return MongoCodecFactory.hasProtectedSubFields(this.type);
	}

	/**
	 * Follows a whole dotted path, with the segment rules of {@link #step(String)}. The walk stops at the
	 * first segment that is not {@link Status#OK}.
	 *
	 * @param dottedPath the path, segments separated by dots
	 * @return the cursor at the end of the path
	 */
	public FieldPathCursor path(final String dottedPath) {
		FieldPathCursor cursor = this;
		for (final String segment : dottedPath.split("\\.", -1)) {
			cursor = cursor.step(segment);
		}
		return cursor;
	}

	/**
	 * Follows one segment of a dotted path: after a list or a set, a numeric segment is an array index
	 * (skipped), any other one is looked for in the element type; after a map, the segment is a key.
	 *
	 * @param segment the path segment
	 * @return the cursor after the segment
	 */
	public FieldPathCursor step(final String segment) {
		return advance(segment, true);
	}

	/**
	 * Follows one key of a document: a key is never an array index, so after a list or a set it is looked
	 * for in the element type.
	 *
	 * @param key the document key
	 * @return the cursor after the key
	 */
	public FieldPathCursor member(final String key) {
		return advance(key, false);
	}

	private FieldPathCursor advance(final String segment, final boolean pathMode) {
		if (this.status != Status.OK || this.freeForm) {
			return this;
		}
		if (this.depth >= MAX_DEPTH) {
			return new FieldPathCursor(this.type, null, Status.UNSAFE, false, this.depth);
		}
		final TypeInfo current = this.type;
		if (isExplicitFreeForm(current)) {
			return freeForm(null);
		}
		if (current.isCollection()) {
			final TypeInfo element = elementOf(current);
			if (element == null) {
				// Raw list or set: each element is written by its own class, nothing can be told.
				return new FieldPathCursor(current, null, Status.UNSAFE, false, this.depth + 1);
			}
			if (pathMode && isIndex(segment)) {
				// "list.0" may also be the key "0" of a map element: only safe when both readings are alike.
				if (element.isMap() && MongoCodecFactory.hasProtectedSubFields(element)) {
					return new FieldPathCursor(element, null, Status.UNSAFE, false, this.depth + 1);
				}
				return new FieldPathCursor(element, null, Status.OK, false, this.depth + 1);
			}
			return new FieldPathCursor(element, null, Status.OK, false, this.depth + 1).advance(segment, pathMode);
		}
		if (current.isMap()) {
			final TypeInfo value = elementOf(current);
			if (value == null) {
				// Raw map: each value is written by its own class, nothing can be told.
				return new FieldPathCursor(current, null, Status.UNSAFE, false, this.depth + 1);
			}
			return new FieldPathCursor(value, null, Status.OK, false, this.depth + 1);
		}
		final DeclaredTypeRules.Kind kind = DeclaredTypeRules.kindOf(current);
		// The root may be an abstract entity class (or an interface) of the application: it is modeled, and its
		// fields checked, like a concrete one.
		final boolean modeledRoot = this.depth == 0 && DeclaredTypeRules.isModeledRoot(current.rawType());
		if (kind.isRefused() && !modeledRoot) {
			// Refused when the model is analysed, so not reachable: fail closed all the same.
			return new FieldPathCursor(current, null, Status.UNSAFE, false, this.depth + 1);
		}
		if (kind == DeclaredTypeRules.Kind.OPAQUE) {
			// A JDK or Jackson class archidata does not model (BigDecimal, an array...): what it holds is not
			// known, so nothing below it can be told safe (a value of a class of the application is refused there
			// when it is written).
			return new FieldPathCursor(current, null, Status.UNSAFE, false, this.depth + 1);
		}
		if (kind != DeclaredTypeRules.Kind.EMBEDDED && !modeledRoot) {
			return freeForm(null);
		}
		final DbClassModel model;
		try {
			model = DbClassModel.of(current.rawType());
		} catch (final Exception ex) {
			// Not reachable from an accepted model (an embedded class that cannot be analysed fails its root):
			// nothing is known below, fail closed all the same.
			LOGGER.debug("Can not analyse the type {}: {}", current.rawType().getName(), ex.getMessage());
			return new FieldPathCursor(current, null, Status.UNSAFE, false, this.depth + 1);
		}
		for (final DbPropertyDescriptor desc : model.getAllFields()) {
			if (!segment.equals(desc.getDbFieldName())) {
				continue;
			}
			if (desc.isNotRead() || desc.isEncrypted()) {
				return new FieldPathCursor(desc.getProperty().getTypeInfo(), desc, Status.PROTECTED, false,
						this.depth + 1);
			}
			if (desc.getAddOn() != null) {
				// A relation is stored as a key: nothing to resolve below it.
				return freeForm(desc);
			}
			if (DeclaredTypeRules.isUntypedPrimaryKey(desc)) {
				// A primary key declared Object holds a key value (checked when it is written).
				return freeForm(desc);
			}
			return new FieldPathCursor(desc.getProperty().getTypeInfo(), desc, Status.OK, false, this.depth + 1);
		}
		return new FieldPathCursor(current, null, Status.UNKNOWN, false, this.depth);
	}

	private static TypeInfo elementOf(final TypeInfo parent) {
		try {
			return DeclaredTypeRules.elementOf(parent);
		} catch (final RuntimeException ex) {
			LOGGER.debug("Can not resolve the element type of {}: {}", parent.genericType(), ex.getMessage());
			return null;
		}
	}

	private FieldPathCursor freeForm(final DbPropertyDescriptor desc) {
		return new FieldPathCursor(null, desc, Status.OK, true, this.depth + 1);
	}

	private static boolean isIndex(final String segment) {
		if (segment.isEmpty()) {
			return false;
		}
		for (int iii = 0; iii < segment.length(); iii++) {
			final char chr = segment.charAt(iii);
			if (chr < '0' || chr > '9') {
				return false;
			}
		}
		return true;
	}

	/** Explicit free-form types, accepted whatever they hold: the BSON types ({@code org.bson.*}, such as {@code Document}). */
	private static boolean isExplicitFreeForm(final TypeInfo typeInfo) {
		return DeclaredTypeRules.kindOf(typeInfo) == DeclaredTypeRules.Kind.FREE_FORM;
	}
}
