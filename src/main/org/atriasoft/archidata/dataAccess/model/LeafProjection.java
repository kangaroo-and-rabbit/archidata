package org.atriasoft.archidata.dataAccess.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.atriasoft.archidata.bean.TypeInfo;
import org.atriasoft.archidata.bean.exception.IntrospectionException;
import org.atriasoft.archidata.dataAccess.model.DeclaredTypeRules.Kind;
import org.atriasoft.archidata.dataAccess.model.codec.MongoFieldCodec;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds the projection paths of one read field down to the leaf fields of its embedded objects, so that a
 * sub-field the read does not return ({@code @DataNotRead} without {@code ReadAllColumn}, {@code @DataEncrypt}
 * when the decryption is disabled) is not transferred from the database. Two kinds of position are transferred
 * whole, such sub-fields included, and the Java read drops them: a {@code Map} (always, without a log) and a part
 * cut by the depth or the path limit (with a WARN).
 *
 * <p>Rules, decided by the declared type (the same model the codec reads with):
 * <ul>
 * <li>a value, an {@code org.bson} type, a class archidata does not model, a {@code Map}: the path itself (a map
 * is included whole, its keys are dynamic: the protected sub-fields of its values are transferred, no WARN is
 * written, the Java read drops them);</li>
 * <li>an embedded object: replaced by the paths of the sub-fields the read returns, recursively; when it has
 * none, a sentinel path ({@code field._archidata_presence_}) keeps it present (MongoDB answers an empty
 * sub-document for an existing one, nothing for {@code null});</li>
 * <li>a {@code List} / {@code Set} (nested lists included) of embedded objects: projected down to the leaves only
 * when the read hides a sub-field of the elements (MongoDB then drops the {@code null} elements), included
 * whole otherwise;</li>
 * <li>a relation of an embedded object is never read back: left out;</li>
 * <li>below {@value #MAX_DEPTH} levels under the top-level field, or beyond {@value #MAX_PATHS} paths for one
 * top-level field, the position is included whole: the Java read still drops what it does not return. When the
 * part included whole holds a sub-field the read does not return, a WARN names the entity field (once per entity
 * class and field); otherwise a DEBUG log says so.</li>
 * </ul>
 *
 * <p>A field the read decodes with another class than its declared one ({@code OptionSpecifyType}) is projected by
 * the leaf fields of the read class, less every sub-field the declared (stored) class protects at the same key
 * ({@link #appendWholeAs}); a list is developed as soon as the declared class protects a sub-field of its elements,
 * also under a key the read class does not declare. {@link #restrict} then removes from the value read the
 * sub-fields the declared class protects that the projection had to include whole, before the other class decodes
 * it.
 *
 * <p>The paths of a whole field depend only on its declared type (and read type) and on the two read flags: they
 * are computed once per (type, read type, {@code readAllColumns}, decryption enabled) and kept, relative to the
 * field, in a bounded cache (at most {@value #MAX_CACHED_TYPES} entries; beyond, they are computed on each read).
 * The DEBUG logs of a whole field are therefore written once per key, naming the first field reached. A dotted
 * sub-path ({@code FilterValue("a.b")}) is resolved on each read, segment by segment whatever its length; the
 * embedded object it reaches is developed with the same depth limit. The cache is emptied with the model cache
 * ({@link DbClassModel#clearCache}, {@link DbClassModel#setAddOns}), and so is the memory of the WARNs written.</p>
 */
final class LeafProjection {
	private static final Logger LOGGER = LoggerFactory.getLogger(LeafProjection.class);

	/** Segment of the path that keeps an embedded object present when none of its sub-fields is read. */
	static final String PRESENCE_SEGMENT = "_archidata_presence_";
	/** Levels below the top-level field developed into sub-fields; deeper positions are included whole. */
	static final int MAX_DEPTH = 8;
	/** Paths one top-level field may develop into; beyond, the field is included whole. */
	static final int MAX_PATHS = 256;
	/** Entries kept by the cache of the whole-field paths. */
	static final int MAX_CACHED_TYPES = 4096;

	/**
	 * Key of the cache: everything the paths of a whole field depend on.
	 *
	 * @param type the type the field is read with
	 * @param stored the declared type when the read uses another one, else {@code null}
	 * @param readAllColumns the {@code ReadAllColumn} flag
	 * @param decryptEnabled the decryption flag
	 */
	private record WholeKey(
			TypeInfo type,
			TypeInfo stored,
			boolean readAllColumns,
			boolean decryptEnabled) {}

	/**
	 * Paths of a whole field, relative to the field ({@code ""} is the field itself, else {@code ".a.b"}), and how
	 * a part included whole holds a sub-field the read does not return ({@code null} when none does).
	 *
	 * @param suffixes the paths, relative to the field
	 * @param cut the first part included whole that holds a sub-field the read does not return, or {@code null}
	 */
	private record WholePaths(
			List<String> suffixes,
			Cut cut) {}

	/**
	 * A part of a field included whole although it holds a sub-field the read does not return.
	 *
	 * @param suffix the position included whole (relative to the field in the cache, absolute while computing)
	 * @param reason why it is included whole, for the WARN
	 */
	private record Cut(
			String suffix,
			String reason) {}

	/** Paths of a whole field, by key. */
	private static final Map<WholeKey, WholePaths> WHOLE_CACHE = new ConcurrentHashMap<>();

	/** Entity fields already warned about (a part transferred whole although the read hides a sub-field of it). */
	private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

	/** Thrown (without stack trace) when a field develops into more than {@link #MAX_PATHS} paths. */
	private static final class TooManyPaths extends RuntimeException {
		private static final long serialVersionUID = 1L;

		TooManyPaths() {
			super(null, null, false, false);
		}
	}

	private final boolean readAllColumns;
	private final boolean decryptEnabled;
	private int count = 0;
	/** The first position included whole although it holds a sub-field the read does not return. */
	private Cut cut = null;

	private LeafProjection(final boolean readAllColumns, final boolean decryptEnabled) {
		this.readAllColumns = readAllColumns;
		this.decryptEnabled = decryptEnabled;
	}

	/**
	 * Appends the projection paths of a whole field.
	 *
	 * @param site the entity field, for the WARN of a part included whole ({@code "Field com.app.Account.tree"})
	 * @param path the path of the field in the document
	 * @param type the declared type of the field
	 * @param readAllColumns {@code true} when the read returns the {@code @DataNotRead} fields
	 * @param decryptEnabled {@code true} when the read decrypts the {@code @DataEncrypt} fields
	 * @param out the paths accumulated so far
	 */
	static void appendWhole(
			final String site,
			final String path,
			final TypeInfo type,
			final boolean readAllColumns,
			final boolean decryptEnabled,
			final List<String> out) {
		appendWholeFor(site, path, type, null, readAllColumns, decryptEnabled, out);
	}

	/**
	 * Appends the projection paths of a whole field that the read decodes with another class than its declared one
	 * ({@code OptionSpecifyType}): the leaf fields of the read type, less every sub-field the declared (stored) type
	 * protects. The two types are walked side by side on the document keys: a sub-field is left out when the read
	 * type, or the declared type at the same key, does not return it; a sub-field the declared type does not know is
	 * projected by the read type alone; a position the read type takes whole (a value, a {@code Map}, a
	 * {@code Document}) while the declared type hides a sub-field below it is projected by the declared type.
	 *
	 * @param site the entity field, for the WARN of a part included whole
	 * @param path the path of the field in the document
	 * @param stored the declared type of the field, which the values were written with
	 * @param read the type the read decodes the field with
	 * @param readAllColumns {@code true} when the read returns the {@code @DataNotRead} fields
	 * @param decryptEnabled {@code true} when the read decrypts the {@code @DataEncrypt} fields
	 * @param out the paths accumulated so far
	 */
	static void appendWholeAs(
			final String site,
			final String path,
			final TypeInfo stored,
			final TypeInfo read,
			final boolean readAllColumns,
			final boolean decryptEnabled,
			final List<String> out) {
		appendWholeFor(site, path, read, stored, readAllColumns, decryptEnabled, out);
	}

	private static void appendWholeFor(
			final String site,
			final String path,
			final TypeInfo read,
			final TypeInfo stored,
			final boolean readAllColumns,
			final boolean decryptEnabled,
			final List<String> out) {
		final WholePaths whole = wholePaths(path, read, stored, readAllColumns, decryptEnabled);
		if (whole.cut() != null) {
			warnOnce(site, path + whole.cut().suffix(), stored != null ? stored : read, whole.cut().reason());
		}
		for (final String suffix : whole.suffixes()) {
			out.add(path + suffix);
		}
	}

	/** The relative paths of a whole field, from the cache or computed (and cached while there is room). */
	private static WholePaths wholePaths(
			final String path,
			final TypeInfo read,
			final TypeInfo stored,
			final boolean readAllColumns,
			final boolean decryptEnabled) {
		final WholeKey key = new WholeKey(read, stored, readAllColumns, decryptEnabled);
		final WholePaths cached = WHOLE_CACHE.get(key);
		if (cached != null) {
			return cached;
		}
		final WholePaths computed = computeWholePaths(path, read, stored, readAllColumns, decryptEnabled);
		if (WHOLE_CACHE.size() >= MAX_CACHED_TYPES) {
			return computed;
		}
		final WholePaths previous = WHOLE_CACHE.putIfAbsent(key, computed);
		return previous != null ? previous : computed;
	}

	private static WholePaths computeWholePaths(
			final String path,
			final TypeInfo read,
			final TypeInfo stored,
			final boolean readAllColumns,
			final boolean decryptEnabled) {
		final LeafProjection projection = new LeafProjection(readAllColumns, decryptEnabled);
		final List<String> paths = new ArrayList<>();
		try {
			projection.expand(path, read, stored, 0, paths);
		} catch (final TooManyPaths ex) {
			if (projection.hides(read, stored)) {
				return new WholePaths(List.of(""),
						new Cut("", "it develops into more than " + MAX_PATHS + " projection paths"));
			}
			LOGGER.debug("Field '{}' (declared {}) develops into more than {} projection paths: projected whole", path,
					typeName(stored != null ? stored : read), MAX_PATHS);
			return new WholePaths(List.of(""), null);
		}
		// Every path starts with the one of the field: kept relative, so that another field of the same type uses it.
		final List<String> suffixes = new ArrayList<>(paths.size());
		for (final String one : paths) {
			suffixes.add(one.substring(path.length()));
		}
		final Cut cut = projection.cut == null ? null
				: new Cut(projection.cut.suffix().substring(path.length()), projection.cut.reason());
		return new WholePaths(List.copyOf(suffixes), cut);
	}

	/** Empties the cache of the whole-field paths and the memory of the WARNs (called with the model cache). */
	static void clearCache() {
		WHOLE_CACHE.clear();
		WARNED.clear();
	}

	/**
	 * Appends the projection paths of a dotted sub-path of a field ({@code FilterValue("address.city")}). The
	 * sub-path is resolved segment by segment in the model: through embedded objects, the elements of the lists
	 * (a list does not consume a segment) and the keys of the maps. Below a value, a free-form type or a class
	 * archidata does not model, the rest of the path is pushed as it is. A segment naming a sub-field the read
	 * does not return, or no sub-field at all, is replaced by the sentinel path of its embedded object, which
	 * stays present and empty, as the Java read rebuilt it before.
	 *
	 * @param site the entity field, for the WARN of a part included whole
	 * @param path the path of the field in the document
	 * @param type the declared type of the field
	 * @param subPath the part of the requested path after the field name (without the leading dot)
	 * @param readAllColumns {@code true} when the read returns the {@code @DataNotRead} fields
	 * @param decryptEnabled {@code true} when the read decrypts the {@code @DataEncrypt} fields
	 * @param out the paths accumulated so far
	 */
	static void appendSubPath(
			final String site,
			final String path,
			final TypeInfo type,
			final String subPath,
			final boolean readAllColumns,
			final boolean decryptEnabled,
			final List<String> out) {
		appendSubPathFor(site, path, type, null, subPath, readAllColumns, decryptEnabled, out);
	}

	/**
	 * Appends the projection paths of a dotted sub-path of a field that the read decodes with another class than
	 * its declared one ({@code OptionSpecifyType}): resolved in the read type, a segment the declared type protects at
	 * the same key being replaced by the sentinel path as one the read type protects; the position reached is
	 * developed as {@link #appendWholeAs} does.
	 *
	 * @param site the entity field, for the WARN of a part included whole
	 * @param path the path of the field in the document
	 * @param stored the declared type of the field, which the values were written with
	 * @param read the type the read decodes the field with
	 * @param subPath the part of the requested path after the field name (without the leading dot)
	 * @param readAllColumns {@code true} when the read returns the {@code @DataNotRead} fields
	 * @param decryptEnabled {@code true} when the read decrypts the {@code @DataEncrypt} fields
	 * @param out the paths accumulated so far
	 */
	static void appendSubPathAs(
			final String site,
			final String path,
			final TypeInfo stored,
			final TypeInfo read,
			final String subPath,
			final boolean readAllColumns,
			final boolean decryptEnabled,
			final List<String> out) {
		appendSubPathFor(site, path, read, stored, subPath, readAllColumns, decryptEnabled, out);
	}

	private static void appendSubPathFor(
			final String site,
			final String path,
			final TypeInfo read,
			final TypeInfo stored,
			final String subPath,
			final boolean readAllColumns,
			final boolean decryptEnabled,
			final List<String> out) {
		final LeafProjection projection = new LeafProjection(readAllColumns, decryptEnabled);
		final List<String> paths = new ArrayList<>();
		projection.resolve(path, read, stored, subPath.split("\\.", -1), 0, 0, paths);
		if (projection.cut != null) {
			warnOnce(site, projection.cut.suffix(), stored != null ? stored : read, projection.cut.reason());
		}
		out.addAll(paths);
	}

	/**
	 * Removes from a value read from the database the sub-fields the read of its declared type would not return
	 * ({@code @DataNotRead} without {@code ReadAllColumn}, {@code @DataEncrypt} when the decryption is disabled), at
	 * any depth: lists, sets and map values included, whatever the projection had to include whole (a {@code Map},
	 * a part cut by the depth or the path limit). The keys the declared type does not know are kept. Used before a
	 * value is decoded with another class than the declared one ({@code OptionSpecifyType}), which may declare such a
	 * sub-field without protection. An embedded class that cannot be analysed gives an empty document (fail closed).
	 *
	 * @param value the value read from the database (not modified)
	 * @param type the declared type of the field
	 * @param readAllColumns {@code true} when the read returns the {@code @DataNotRead} fields
	 * @param decryptEnabled {@code true} when the read decrypts the {@code @DataEncrypt} fields
	 * @return the value without the sub-fields the declared type does not return
	 */
	static Object restrict(
			final Object value,
			final TypeInfo type,
			final boolean readAllColumns,
			final boolean decryptEnabled) {
		return new LeafProjection(readAllColumns, decryptEnabled).restrict(value, type);
	}

	private Object restrict(final Object value, final TypeInfo type) {
		if (value == null || type == null) {
			return value;
		}
		final Kind kind = DeclaredTypeRules.kindOf(type);
		if (kind == Kind.LIST_OR_SET) {
			if (!(value instanceof final List<?> list)) {
				return value;
			}
			final TypeInfo element = DeclaredTypeRules.elementOf(type);
			final List<Object> out = new ArrayList<>(list.size());
			for (final Object item : list) {
				out.add(restrict(item, element));
			}
			return out;
		}
		if (kind == Kind.MAP) {
			if (!(value instanceof final Map<?, ?> map)) {
				return value;
			}
			final TypeInfo element = DeclaredTypeRules.elementOf(type);
			final Document out = new Document();
			for (final Map.Entry<?, ?> entry : map.entrySet()) {
				out.put(String.valueOf(entry.getKey()), restrict(entry.getValue(), element));
			}
			return out;
		}
		if (kind != Kind.EMBEDDED || !(value instanceof final Map<?, ?> doc)) {
			return value;
		}
		final Document out = new Document();
		final DbClassModel model = modelOf(type.rawType());
		if (model == null) {
			// Nothing is known of the class: nothing is kept (fail closed).
			return out;
		}
		for (final Map.Entry<?, ?> entry : doc.entrySet()) {
			out.put(String.valueOf(entry.getKey()), entry.getValue());
		}
		for (final DbPropertyDescriptor desc : model.getAllFields()) {
			final String key = desc.getDbFieldName();
			if (!out.containsKey(key)) {
				continue;
			}
			if (isHidden(desc)) {
				out.remove(key);
			} else {
				out.put(key, restrict(out.get(key), desc.getProperty().getTypeInfo()));
			}
		}
		return out;
	}

	/**
	 * Removes the duplicates and every path below another one of the list: MongoDB refuses a projection naming
	 * both {@code a} and {@code a.b} (path collision), and the widest already holds the other.
	 *
	 * @param paths the projection paths
	 * @return the paths, each once, none below another, in their first order
	 */
	static List<String> normalize(final List<String> paths) {
		final Set<String> all = new HashSet<>(paths);
		final Set<String> out = new LinkedHashSet<>();
		for (final String path : paths) {
			if (!hasAncestorIn(path, all)) {
				out.add(path);
			}
		}
		return new ArrayList<>(out);
	}

	private static boolean hasAncestorIn(final String path, final Set<String> all) {
		int dot = path.indexOf('.');
		while (dot >= 0) {
			if (all.contains(path.substring(0, dot))) {
				return true;
			}
			dot = path.indexOf('.', dot + 1);
		}
		return false;
	}

	/** WARN once per entity field: a part of it is transferred whole although the read hides a sub-field of it. */
	private static void warnOnce(final String site, final String position, final TypeInfo type, final String reason) {
		final String name = site == null ? "Field '" + position + "'" : site;
		if (!WARNED.add(name)) {
			return;
		}
		LOGGER.warn("{} (declared {}) is projected whole at '{}' because {}, and it holds sub-fields this read does not"
				+ " return (@DataNotRead, @DataEncrypt): they are transferred from the database, then dropped"
				+ " by the Java read", name, typeName(type), position, reason);
	}

	private static String typeName(final TypeInfo type) {
		return type.genericType() != null ? type.genericType().getTypeName() : type.rawType().getName();
	}

	private void add(final String path, final List<String> out) {
		this.count++;
		if (this.count > MAX_PATHS) {
			throw new TooManyPaths();
		}
		out.add(path);
	}

	/**
	 * Develops a position by the type it is read with.
	 *
	 * @param stored the declared type at the same position when the read uses another type, else {@code null}
	 * @return {@code true} when a sub-field the read does not return is below the position (left out, or included
	 *         whole by a cut)
	 */
	private boolean expand(
			final String path,
			final TypeInfo type,
			final TypeInfo stored,
			final int depth,
			final List<String> out) {
		final Kind kind = DeclaredTypeRules.kindOf(type);
		if (kind == Kind.EMBEDDED) {
			return expandEmbedded(path, type.rawType(), stored, depth, out);
		}
		if (kind == Kind.LIST_OR_SET) {
			return expandList(path, type, stored, depth, out);
		}
		return expandByStored(path, stored, depth, out);
	}

	/**
	 * A position the read type takes whole: projected by the declared type when it hides a sub-field below, whole
	 * otherwise.
	 */
	private boolean expandByStored(final String path, final TypeInfo stored, final int depth, final List<String> out) {
		if (stored != null && hidesBelow(stored, new HashSet<>())) {
			return expand(path, stored, null, depth, out);
		}
		add(path, out);
		return false;
	}

	private boolean expandList(
			final String path,
			final TypeInfo type,
			final TypeInfo stored,
			final int depth,
			final List<String> out) {
		TypeInfo element = type;
		while (element != null && DeclaredTypeRules.kindOf(element) == Kind.LIST_OR_SET) {
			element = DeclaredTypeRules.elementOf(element);
		}
		if (element == null || DeclaredTypeRules.kindOf(element) != Kind.EMBEDDED) {
			return expandByStored(path, stored, depth, out);
		}
		// MongoDB applies a sub-path to every element (nested arrays included), but drops the null elements: only
		// worth it when the elements hold something this read does not return.
		final int mark = this.count;
		final List<String> leaves = new ArrayList<>();
		if (expandEmbedded(path, element.rawType(), stored, depth, leaves)) {
			out.addAll(leaves);
			return true;
		}
		this.count = mark;
		add(path, out);
		return false;
	}

	private boolean expandEmbedded(
			final String path,
			final Class<?> raw,
			final TypeInfo stored,
			final int depth,
			final List<String> out) {
		if (depth >= MAX_DEPTH) {
			final boolean hides = hides(TypeInfo.ofRaw(raw), stored);
			if (hides) {
				if (this.cut == null) {
					this.cut = new Cut(path, "it is deeper than " + MAX_DEPTH + " levels");
				}
			} else {
				LOGGER.debug("Embedded object '{}' is deeper than {} levels: projected whole", path, MAX_DEPTH);
			}
			add(path, out);
			return hides;
		}
		final DbClassModel model = modelOf(raw);
		if (model == null) {
			return expandByStored(path, stored, depth, out);
		}
		final TypeInfo storedPosition = unwrapLists(stored);
		final boolean isRecord = model.getClassModel().isRecord();
		final Set<String> readKeys = new HashSet<>();
		boolean hidden = false;
		boolean any = false;
		for (final DbPropertyDescriptor desc : model.getAllFields()) {
			final MongoFieldCodec codec = readCodec(desc, isRecord);
			if (codec == null) {
				continue;
			}
			readKeys.add(codec.getDbFieldName());
			final DbPropertyDescriptor storedDesc = storedField(storedPosition, codec.getDbFieldName());
			if (isHidden(desc) || (storedDesc != null && isHidden(storedDesc))) {
				hidden = true;
				continue;
			}
			any = true;
			hidden |= expand(path + "." + codec.getDbFieldName(), desc.getProperty().getTypeInfo(),
					storedChild(storedPosition, storedDesc), depth + 1, out);
		}
		if (!any) {
			add(path + "." + PRESENCE_SEGMENT, out);
		}
		// A sub-field the declared class protects under a key the read class does not declare is left out too: it
		// counts, so that a list holding such elements is developed instead of being included whole.
		return hidden || storedHidesOutside(storedPosition, readKeys);
	}

	/**
	 * Tells whether the declared (stored) type of a position protects a sub-field, at any depth, under a key the read
	 * class does not declare. A map counts by its values; a class that cannot be analysed counts as protecting one
	 * (fail closed).
	 */
	private boolean storedHidesOutside(final TypeInfo storedPosition, final Set<String> readKeys) {
		if (storedPosition == null) {
			return false;
		}
		final Kind kind = DeclaredTypeRules.kindOf(storedPosition);
		if (kind == Kind.MAP) {
			return hidesBelow(storedPosition, new HashSet<>());
		}
		if (kind != Kind.EMBEDDED) {
			return false;
		}
		final DbClassModel model = modelOf(storedPosition.rawType());
		if (model == null) {
			return true;
		}
		for (final DbPropertyDescriptor desc : model.getAllFields()) {
			if (readKeys.contains(desc.getDbFieldName())) {
				continue;
			}
			if (isHidden(desc) || hidesBelow(desc.getProperty().getTypeInfo(), new HashSet<>())) {
				return true;
			}
		}
		return false;
	}

	/** Tells whether the read type or the declared type of a position hides a sub-field below it. */
	private boolean hides(final TypeInfo read, final TypeInfo stored) {
		return hidesBelow(read, new HashSet<>()) || hidesBelow(stored, new HashSet<>());
	}

	/**
	 * Tells whether a position of this declared type holds, at any depth, a sub-field this read does not return.
	 * A class that cannot be analysed counts as holding one (fail closed).
	 */
	private boolean hidesBelow(final TypeInfo type, final Set<Class<?>> visited) {
		if (type == null) {
			return false;
		}
		final Kind kind = DeclaredTypeRules.kindOf(type);
		if (kind == Kind.LIST_OR_SET || kind == Kind.MAP) {
			return hidesBelow(DeclaredTypeRules.elementOf(type), visited);
		}
		if (kind != Kind.EMBEDDED || !visited.add(type.rawType())) {
			return false;
		}
		final DbClassModel model = modelOf(type.rawType());
		if (model == null) {
			return true;
		}
		final boolean isRecord = model.getClassModel().isRecord();
		for (final DbPropertyDescriptor desc : model.getAllFields()) {
			if (readCodec(desc, isRecord) == null) {
				continue;
			}
			if (isHidden(desc) || hidesBelow(desc.getProperty().getTypeInfo(), visited)) {
				return true;
			}
		}
		return false;
	}

	/** The type itself, or the element type of a (nested) list or set. */
	private static TypeInfo unwrapLists(final TypeInfo type) {
		TypeInfo current = type;
		while (current != null && DeclaredTypeRules.kindOf(current) == Kind.LIST_OR_SET) {
			current = DeclaredTypeRules.elementOf(current);
		}
		return current;
	}

	/** The sub-field of the declared embedded class stored under a document key, or {@code null}. */
	private static DbPropertyDescriptor storedField(final TypeInfo storedPosition, final String key) {
		if (storedPosition == null || DeclaredTypeRules.kindOf(storedPosition) != Kind.EMBEDDED) {
			return null;
		}
		final DbClassModel model = modelOf(storedPosition.rawType());
		if (model == null) {
			return null;
		}
		for (final DbPropertyDescriptor desc : model.getAllFields()) {
			if (key.equals(desc.getDbFieldName())) {
				return desc;
			}
		}
		return null;
	}

	/** The declared type under a document key: of the sub-field, or of the values of a map; else {@code null}. */
	private static TypeInfo storedChild(final TypeInfo storedPosition, final DbPropertyDescriptor storedDesc) {
		if (storedDesc != null) {
			return storedDesc.getProperty().getTypeInfo();
		}
		if (storedPosition != null && DeclaredTypeRules.kindOf(storedPosition) == Kind.MAP) {
			return DeclaredTypeRules.elementOf(storedPosition);
		}
		return null;
	}

	/**
	 * Resolves a dotted sub-path, from the segment {@code index}, and appends its projection paths. When the position
	 * it reaches develops into more than {@link #MAX_PATHS} paths, the position is appended whole (WARN when it holds
	 * a sub-field this read does not return, DEBUG otherwise).
	 */
	private void resolve(
			final String start,
			final TypeInfo type,
			final TypeInfo stored,
			final String[] segments,
			final int fromIndex,
			final int fromDepth,
			final List<String> out) {
		final StringBuilder position = new StringBuilder(start);
		TypeInfo current = type;
		TypeInfo storedCurrent = stored;
		int depth = fromDepth;
		int index = fromIndex;
		while (index < segments.length) {
			final Kind kind = DeclaredTypeRules.kindOf(current);
			if (kind == Kind.LIST_OR_SET) {
				// A list does not consume a segment: MongoDB applies the rest of the path to every element.
				current = DeclaredTypeRules.elementOf(current);
				continue;
			}
			final TypeInfo storedPosition = unwrapLists(storedCurrent);
			if (kind == Kind.MAP) {
				final DbPropertyDescriptor storedDesc = storedField(storedPosition, segments[index]);
				if (storedDesc != null && isHidden(storedDesc)) {
					add(position + "." + PRESENCE_SEGMENT, out);
					return;
				}
				position.append('.').append(segments[index]);
				current = DeclaredTypeRules.elementOf(current);
				storedCurrent = storedChild(storedPosition, storedDesc);
				depth++;
				index++;
				continue;
			}
			if (kind != Kind.EMBEDDED) {
				if (storedCurrent != null && hidesBelow(storedCurrent, new HashSet<>())) {
					// Not modeled below by the read type, the declared type hides a sub-field: resolved by it.
					resolve(position.toString(), storedCurrent, null, segments, index, depth, out);
					return;
				}
				// Not modeled below: pushed as it is.
				add(position + "." + String.join(".", Arrays.copyOfRange(segments, index, segments.length)), out);
				return;
			}
			final DbPropertyDescriptor desc = findReadField(current.rawType(), segments[index]);
			final DbPropertyDescriptor storedDesc = storedField(storedPosition, segments[index]);
			if (desc == null || (storedDesc != null && isHidden(storedDesc))) {
				add(position + "." + PRESENCE_SEGMENT, out);
				return;
			}
			position.append('.').append(desc.getCodec().getDbFieldName());
			current = desc.getProperty().getTypeInfo();
			storedCurrent = storedChild(storedPosition, storedDesc);
			depth++;
			index++;
		}
		final String path = position.toString();
		final List<String> developed = new ArrayList<>();
		try {
			expand(path, current, storedCurrent, depth, developed);
		} catch (final TooManyPaths ex) {
			if (hides(current, storedCurrent)) {
				this.cut = new Cut(path, "it develops into more than " + MAX_PATHS + " projection paths");
			} else {
				LOGGER.debug("Path '{}' develops into more than {} projection paths: projected whole", path, MAX_PATHS);
			}
			out.add(path);
			return;
		}
		out.addAll(developed);
	}

	/** The sub-field of an embedded class this read returns under a document key, or {@code null}. */
	private DbPropertyDescriptor findReadField(final Class<?> raw, final String key) {
		final DbClassModel model = modelOf(raw);
		if (model == null) {
			return null;
		}
		final boolean isRecord = model.getClassModel().isRecord();
		for (final DbPropertyDescriptor desc : model.getAllFields()) {
			final MongoFieldCodec codec = readCodec(desc, isRecord);
			if (codec != null && key.equals(codec.getDbFieldName())) {
				return isHidden(desc) ? null : desc;
			}
		}
		return null;
	}

	/** The codec the read of an embedded object uses for a sub-field, or {@code null} when it is not read back. */
	private static MongoFieldCodec readCodec(final DbPropertyDescriptor desc, final boolean isRecord) {
		if (desc.getAddOn() != null) {
			// A relation of an embedded object is written but never read back.
			return null;
		}
		final MongoFieldCodec codec = desc.getCodec();
		if (codec == null || (!isRecord && !codec.canSetValue())) {
			return null;
		}
		return codec;
	}

	/** Tells whether this read leaves a sub-field unset (the Java read would drop it). */
	private boolean isHidden(final DbPropertyDescriptor desc) {
		return (desc.isNotRead() && !this.readAllColumns) || (desc.isEncrypted() && !this.decryptEnabled);
	}

	private static DbClassModel modelOf(final Class<?> raw) {
		try {
			return DbClassModel.of(raw);
		} catch (final IntrospectionException ex) {
			LOGGER.debug("Can not analyse the embedded type {}: {}", raw.getName(), ex.getMessage());
			return null;
		}
	}
}
