package org.atriasoft.archidata.index;

import java.util.Collections;
import java.util.List;

/**
 * What a synchronization would do, collection by collection. Produced by
 * {@link IndexEngine#plan()} without touching anything, so it can be reviewed — on a large
 * collection, creating an index is not free.
 *
 * @param actions every action, in the order they would be applied
 */
public record IndexPlan(
		List<IndexAction> actions) {

	/** Compact constructor making the action list unmodifiable. */
	public IndexPlan {
		actions = List.copyOf(actions);
	}

	/**
	 * The actions that would modify the database.
	 *
	 * @return the creations, replacements and drops
	 */
	public List<IndexAction> changes() {
		return this.actions.stream().filter(IndexAction::isChange).toList();
	}

	/**
	 * The actions of one kind.
	 *
	 * @param kind the wanted kind
	 * @return the matching actions
	 */
	public List<IndexAction> of(final IndexAction.Kind kind) {
		return this.actions.stream().filter(action -> action.kind() == kind).toList();
	}

	/**
	 * Tells whether the database already matches the code.
	 *
	 * @return {@code true} when nothing would be modified
	 */
	public boolean isUpToDate() {
		return changes().isEmpty();
	}

	/**
	 * A readable, multi-line description of the plan, meant for a log or a console.
	 *
	 * @return the description
	 */
	public String describe() {
		if (this.actions.isEmpty()) {
			return "no managed collection";
		}
		final StringBuilder out = new StringBuilder();
		String currentCollection = null;
		for (final IndexAction action : this.actions) {
			if (!action.collectionName().equals(currentCollection)) {
				currentCollection = action.collectionName();
				out.append(currentCollection).append(':').append(System.lineSeparator());
			}
			out.append("    ").append(action.kind()).append(' ').append(action.indexName());
			if (action.spec() != null) {
				out.append(' ').append(action.spec().toKeysDocument().toJson());
			}
			if (action.detail() != null && !action.detail().isEmpty()) {
				out.append("  <- ").append(action.detail());
			}
			out.append(System.lineSeparator());
		}
		return out.toString();
	}

	/**
	 * An empty plan.
	 *
	 * @return a plan with no action
	 */
	public static IndexPlan empty() {
		return new IndexPlan(Collections.emptyList());
	}
}
