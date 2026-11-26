package org.atriasoft.archidata.dataAccess.options;

/**
 * An update skips the fields annotated {@code @ApiReadOnly}; this option makes it write them too.
 */
public class ForceReadOnlyField extends QueryOption {
	/** Constructs an option to allow updating fields that are normally read-only. */
	public ForceReadOnlyField() {
		// default constructor
	}
}
