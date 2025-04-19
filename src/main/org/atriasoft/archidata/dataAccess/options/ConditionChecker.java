package org.atriasoft.archidata.dataAccess.options;

import org.atriasoft.archidata.dataAccess.QueryItem;

/** Condition model apply to the check models. */
public class ConditionChecker extends QueryOption {
	public final QueryItem condition;

	public ConditionChecker(final QueryItem items) {
		this.condition = items;
	}

	public ConditionChecker(final Condition cond) {
		this.condition = cond.condition;
	}

	public ConditionChecker() {
		this.condition = null;
	}

	public Condition toCondition() {
		return new Condition(this.condition);
	}

}
