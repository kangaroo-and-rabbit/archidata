package org.atriasoft.archidata.dataAccess;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Bounds the expansion of the relation fields typed with an entity ({@code @ManyToOneDoc Node parent},
 * {@code @OneToManyDoc List<Child> children}...).
 *
 * <p>Each loaded relation runs its own read, which expands the relations of the loaded entities in turn.
 * Two limits keep a read from recursing without end or exploding into thousands of queries:
 * <ul>
 * <li>a maximum depth ({@value #DEFAULT_MAX_DEPTH} by default, {@link #setMaxDepth(int)}): beyond it, the
 * relation fields of the entities read are left unloaded ({@code null});</li>
 * <li>cycle detection: an entity already being expanded higher on the same chain (A to B to A) is read
 * again, but its own relation fields are left unloaded.</li>
 * </ul>
 * The state is kept per thread, for the duration of the top-level read.
 */
public final class RelationExpansion {
	/** Default maximum number of nested relation levels expanded by a single read. */
	public static final int DEFAULT_MAX_DEPTH = 5;

	private static volatile int maxDepth = DEFAULT_MAX_DEPTH;

	private static final ThreadLocal<State> STATE = ThreadLocal.withInitial(State::new);

	/** Expansion state of the current thread. */
	private static final class State {
		private int depth = 0;
		private final Set<String> path = new HashSet<>();
	}

	private RelationExpansion() {
		// Utility class
	}

	/**
	 * Returns the maximum number of nested relation levels expanded by a single read.
	 *
	 * @return the maximum depth
	 */
	public static int getMaxDepth() {
		return maxDepth;
	}

	/**
	 * Sets the maximum number of nested relation levels expanded by a single read (application-wide).
	 *
	 * @param depth the new maximum, {@code >= 0} ({@code 0} never expands a relation)
	 * @throws IllegalArgumentException if {@code depth} is negative
	 */
	public static void setMaxDepth(final int depth) {
		if (depth < 0) {
			throw new IllegalArgumentException("Relation expansion depth must be >= 0, got: " + depth);
		}
		maxDepth = depth;
	}

	/**
	 * Tells whether the relations of an entity read now may be expanded.
	 *
	 * @param clazz the entity class
	 * @param primaryKey the primary key of the entity, may be {@code null}
	 * @return {@code false} when the maximum depth is reached or the entity is already expanded on this chain
	 */
	static boolean canExpand(final Class<?> clazz, final Object primaryKey) {
		final State state = STATE.get();
		if (state.depth >= maxDepth) {
			return false;
		}
		return primaryKey == null || !state.path.contains(key(clazz, primaryKey));
	}

	/**
	 * Enters one expansion level for the given entities (call {@link #leave} in a {@code finally} block).
	 *
	 * @param clazz the entity class
	 * @param primaryKeys the primary keys of the entities whose relations are expanded
	 * @return the keys added to the chain, to pass to {@link #leave}
	 */
	static List<String> enter(final Class<?> clazz, final List<Object> primaryKeys) {
		final State state = STATE.get();
		state.depth++;
		final List<String> added = new ArrayList<>();
		for (final Object primaryKey : primaryKeys) {
			final String key = key(clazz, primaryKey);
			if (state.path.add(key)) {
				added.add(key);
			}
		}
		return added;
	}

	/**
	 * Leaves an expansion level entered with {@link #enter}.
	 *
	 * @param added the keys returned by {@link #enter}
	 */
	static void leave(final List<String> added) {
		final State state = STATE.get();
		state.path.removeAll(added);
		state.depth--;
		if (state.depth <= 0) {
			STATE.remove();
		}
	}

	private static String key(final Class<?> clazz, final Object primaryKey) {
		return clazz.getName() + "#" + primaryKey;
	}
}
