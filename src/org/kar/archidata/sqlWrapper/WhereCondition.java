package org.kar.archidata.sqlWrapper;

public record WhereCondition(
		String key,
		String comparator,
		Object Value) {
	
}
