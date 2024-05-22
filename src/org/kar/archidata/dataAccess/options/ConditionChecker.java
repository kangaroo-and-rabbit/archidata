package org.kar.archidata.dataAccess.options;

import org.kar.archidata.dataAccess.QueryItem;

/** Condition model apply to the check models. */
public class ConditionChecker extends QueryOption {
	public final QueryItem condition;

	public ConditionChecker(final QueryItem items) {
		this.condition = items;
	}

	public ConditionChecker() {
		this.condition = null;
	}

	public Condition toCondition() {
		return new Condition(this.condition);
	}

}
