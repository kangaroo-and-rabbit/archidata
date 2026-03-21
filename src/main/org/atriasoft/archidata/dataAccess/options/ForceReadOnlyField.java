package org.atriasoft.archidata.dataAccess.options;

/**
 * The value marked as readOnly is not updatable by default by the basic update access, but we can force it with the option
 */
public class ForceReadOnlyField extends QueryOption {
	/** Constructs an option to allow updating fields that are normally read-only. */
	public ForceReadOnlyField() {
		// default constructor
	}
}
