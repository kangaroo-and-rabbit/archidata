package org.atriasoft.archidata.index;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.atriasoft.archidata.annotation.Index;
import org.atriasoft.archidata.annotation.Indexed;
import org.atriasoft.archidata.bean.exception.IntrospectionException;
import org.atriasoft.archidata.dataAccess.model.DbClassModel;
import org.atriasoft.archidata.dataAccess.model.DbFieldAction;
import org.atriasoft.archidata.dataAccess.model.DbPropertyDescriptor;
import org.atriasoft.archidata.exception.DataAccessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Collects the index declarations of an entity, from the three ways of declaring them:
 * <ul>
 *   <li>{@link Index} on the class — compound indexes, field names as strings;</li>
 *   <li>{@link Indexed} on a property — the single-key short form;</li>
 *   <li>{@link #declare(Class, IndexSpec...)} — the programmatic, type-safe form.</li>
 * </ul>
 * The three are equivalent and can be mixed: which one fits depends on how a project prefers to
 * keep its declarations. They are merged, checked against the entity model, and de-duplicated.
 *
 * <p>Two declarations describing the same indexed data (same keys, same partial filter) but
 * disagreeing on the options — one unique, the other not — are rejected: silently keeping one of
 * them would make the database depend on a resolution order nobody wrote down.
 *
 * <p>An unknown field name is rejected too: an index on a field that does not exist costs write
 * time and serves no query.
 */
public final class IndexRegistry {
	private static final Logger LOGGER = LoggerFactory.getLogger(IndexRegistry.class);

	/** Programmatic declarations, per entity. */
	private static final Map<Class<?>, List<IndexSpec>> DECLARED = new ConcurrentHashMap<>();
	/** Resolved declarations, per entity. Invalidated by {@link #declare} and {@link #clear}. */
	private static final Map<Class<?>, List<IndexSpec>> RESOLVED = new ConcurrentHashMap<>();

	private IndexRegistry() {
		// Utility class
	}

	/**
	 * Declare indexes programmatically, on top of what the annotations of the class say.
	 *
	 * <pre>{@code
	 * IndexRegistry.declare(User.class,
	 *     IndexSpec.asc(User::getCompanyId).thenDesc(User::getCreatedAt),
	 *     IndexSpec.asc(User::getEmail).unique());
	 * }</pre>
	 *
	 * @param clazz the entity the indexes belong to
	 * @param specs the index specifications
	 */
	public static void declare(final Class<?> clazz, final IndexSpec... specs) {
		if (clazz == null || specs == null || specs.length == 0) {
			return;
		}
		final List<IndexSpec> stored = DECLARED.computeIfAbsent(clazz, key -> new ArrayList<>());
		synchronized (stored) {
			for (final IndexSpec spec : specs) {
				stored.add(spec);
			}
		}
		RESOLVED.remove(clazz);
	}

	/**
	 * Forget every programmatic declaration and every resolution. Mainly useful in tests.
	 */
	public static void clear() {
		DECLARED.clear();
		RESOLVED.clear();
	}

	/**
	 * Forget the programmatic declarations of one entity.
	 *
	 * @param clazz the entity to clear
	 */
	public static void clear(final Class<?> clazz) {
		DECLARED.remove(clazz);
		RESOLVED.remove(clazz);
	}

	/**
	 * Resolve every index of an entity: merged, checked, de-duplicated.
	 *
	 * @param clazz the entity to inspect
	 * @return the indexes to maintain on the collection of this entity
	 * @throws DataAccessException if a declaration is invalid, names an unknown field, or
	 *         contradicts another one
	 */
	public static List<IndexSpec> resolve(final Class<?> clazz) throws DataAccessException {
		final List<IndexSpec> cached = RESOLVED.get(clazz);
		if (cached != null) {
			return cached;
		}
		final DbClassModel model;
		try {
			model = DbClassModel.of(clazz);
		} catch (final IntrospectionException ex) {
			throw new DataAccessException("Failed to introspect class: " + clazz.getSimpleName(), ex);
		}
		// LinkedHashMap: a stable order makes the synchronization logs comparable between runs.
		final Map<String, Declaration> byTarget = new LinkedHashMap<>();
		collectClassAnnotations(clazz, byTarget);
		collectPropertyAnnotations(clazz, model, byTarget);
		collectProgrammatic(clazz, byTarget);

		final List<IndexSpec> out = new ArrayList<>(byTarget.size());
		for (final Declaration declaration : byTarget.values()) {
			declaration.spec.validate(declaration.origin);
			checkFieldsExist(model, declaration);
			out.add(declaration.spec);
		}
		warnOnUnenforcedUnique(model, out);
		final List<IndexSpec> result = List.copyOf(out);
		RESOLVED.put(clazz, result);
		return result;
	}

	/** One specification with where it came from, to make the error messages actionable. */
	private record Declaration(
			IndexSpec spec,
			String origin) {}

	private static void collectClassAnnotations(final Class<?> clazz, final Map<String, Declaration> byTarget)
			throws DataAccessException {
		// Walked explicitly, base class first: for a *repeatable* annotation,
		// getAnnotationsByType() stops at the first class carrying one and never looks at the
		// parents, so a child declaring its own @Index would silently lose the inherited ones.
		final List<Class<?>> hierarchy = new ArrayList<>();
		for (Class<?> current = clazz; current != null && current != Object.class; current = current.getSuperclass()) {
			hierarchy.add(0, current);
		}
		for (final Class<?> current : hierarchy) {
			collectDeclaredClassAnnotations(current, byTarget);
		}
	}

	private static void collectDeclaredClassAnnotations(final Class<?> clazz, final Map<String, Declaration> byTarget)
			throws DataAccessException {
		for (final Index annotation : clazz.getDeclaredAnnotationsByType(Index.class)) {
			IndexSpec spec = null;
			for (final String declaredKey : annotation.value()) {
				final IndexKey key = IndexKey.parse(declaredKey);
				spec = spec == null ? (key.ascending() ? IndexSpec.asc(key.path()) : IndexSpec.desc(key.path()))
						: (key.ascending() ? spec.thenAsc(key.path()) : spec.thenDesc(key.path()));
			}
			if (spec == null) {
				throw new DataAccessException("@Index on " + clazz.getSimpleName() + " declares no field");
			}
			spec = applyOptions(spec, annotation.name(), annotation.unique(), annotation.sparse(),
					annotation.expireAfterSeconds(), annotation.partialFilter());
			merge(byTarget, spec, "@Index on " + clazz.getSimpleName());
		}
	}

	private static void collectPropertyAnnotations(
			final Class<?> clazz,
			final DbClassModel model,
			final Map<String, Declaration> byTarget) throws DataAccessException {
		for (final DbPropertyDescriptor desc : model.getAllFields()) {
			final Indexed annotation = desc.getProperty().getAnnotation(Indexed.class);
			if (annotation == null) {
				continue;
			}
			final String path = desc.getDbFieldName();
			IndexSpec spec = annotation.ascending() ? IndexSpec.asc(path) : IndexSpec.desc(path);
			spec = applyOptions(spec, annotation.name(), annotation.unique(), annotation.sparse(),
					annotation.expireAfterSeconds(), annotation.partialFilter());
			merge(byTarget, spec, "@Indexed on " + clazz.getSimpleName() + "." + desc.getProperty().getName());
		}
	}

	private static void collectProgrammatic(final Class<?> clazz, final Map<String, Declaration> byTarget)
			throws DataAccessException {
		final List<IndexSpec> stored = DECLARED.get(clazz);
		if (stored == null) {
			return;
		}
		synchronized (stored) {
			for (final IndexSpec spec : stored) {
				merge(byTarget, spec, "IndexRegistry.declare(" + clazz.getSimpleName() + ")");
			}
		}
	}

	private static IndexSpec applyOptions(
			final IndexSpec base,
			final String name,
			final boolean unique,
			final boolean sparse,
			final long expireAfterSeconds,
			final String partialFilter) {
		IndexSpec spec = base;
		if (name != null && !name.isBlank()) {
			spec = spec.name(name);
		}
		if (unique) {
			spec = spec.unique();
		}
		if (sparse) {
			spec = spec.sparse();
		}
		if (expireAfterSeconds >= 0) {
			spec = spec.expireAfterSeconds(expireAfterSeconds);
		}
		if (partialFilter != null && !partialFilter.isBlank()) {
			spec = spec.partialFilter(partialFilter);
		}
		return spec;
	}

	/** Add a specification, or fail if another declaration already describes the same data differently. */
	private static void merge(final Map<String, Declaration> byTarget, final IndexSpec spec, final String origin)
			throws DataAccessException {
		final Declaration previous = byTarget.get(spec.targetSignature());
		if (previous == null) {
			byTarget.put(spec.targetSignature(), new Declaration(spec, origin));
			return;
		}
		if (previous.spec.equals(spec)) {
			// Same index declared twice: harmless, keep the first origin.
			return;
		}
		throw new DataAccessException(
				"Contradictory index declarations on the same fields " + spec.toKeysDocument().toJson() + ": "
						+ previous.origin + " says [" + previous.spec.definitionSignature() + "] while " + origin
						+ " says [" + spec.definitionSignature() + "]");
	}

	/** Reject a key naming a field the entity does not have — a typo must not become a useless index. */
	private static void checkFieldsExist(final DbClassModel model, final Declaration declaration)
			throws DataAccessException {
		for (final IndexKey key : declaration.spec.getKeys()) {
			if (model.findByFieldPath(key.path()) == null) {
				throw new DataAccessException(declaration.origin + " references the unknown field '" + key.topLevel()
						+ "' on " + model.getTableName());
			}
		}
	}

	/**
	 * Warn about a {@code @Column(unique = true)} that nothing enforces. {@code @Column} carries
	 * JPA metadata archidata does not act upon, so the constraint only exists if an index declares
	 * it. Primary keys are excluded: MongoDB gives them a unique {@code _id_} index natively.
	 */
	private static void warnOnUnenforcedUnique(final DbClassModel model, final List<IndexSpec> specs) {
		for (final DbPropertyDescriptor desc : model.getAllFields()) {
			if (!desc.isUnique() || desc.getAction() == DbFieldAction.PRIMARY_KEY) {
				continue;
			}
			if (isCoveredByUniqueIndex(specs, desc.getDbFieldName())) {
				continue;
			}
			LOGGER.warn(
					"{}.{}: @Column(unique = true) is not enforced by the database, archidata does not act upon it. "
							+ "Declare @Indexed(unique = true) (or @Index(value = \"{}\", unique = true)) to enforce it.",
					model.getTableName(), desc.getDbFieldName(), desc.getDbFieldName());
		}
	}

	/** Tells whether a unique index starts with the given field — enough to enforce its unicity. */
	private static boolean isCoveredByUniqueIndex(final List<IndexSpec> specs, final String fieldName) {
		for (final IndexSpec spec : specs) {
			if (spec.isUnique() && !spec.getKeys().isEmpty() && spec.getKeys().get(0).path().equals(fieldName)) {
				return true;
			}
		}
		return false;
	}
}
