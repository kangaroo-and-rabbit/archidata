package org.atriasoft.archidata.dataAccess.options;

import java.util.List;

/** Filter field that might be not updated. */
public class FilterOmit extends QueryOption {
	public final List<String> filterValue;

	public FilterOmit(final List<String> filterValue) {
		this.filterValue = filterValue;
	}

	public FilterOmit(final String... filterValue) {
		this.filterValue = List.of(filterValue);
	}

	public List<String> getValues() {
		return this.filterValue;
	}

}
