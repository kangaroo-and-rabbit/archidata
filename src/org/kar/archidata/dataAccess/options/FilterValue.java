package org.kar.archidata.dataAccess.options;

import java.util.List;

import org.kar.archidata.dataAccess.QueryOption;

/** By default some element are not read like createAt and UpdatedAt. This option permit to read it. */
public class FilterValue extends QueryOption {
	public final List<String> filterValue;

	public FilterValue(final List<String> filterValue) {
		this.filterValue = filterValue;
	}

	public FilterValue(final String... filterValue) {
		this.filterValue = List.of(filterValue);
	}

	public List<String> getValues() {
		return this.filterValue;
	}
}
