package org.atriasoft.archidata.dataAccess.options;

/** On an insert, keeps the primary key already set in the object instead of generating one (the insert
 * fails when it is {@code null}). {@link DirectData} implies it. */
public class DirectPrimaryKey extends QueryOption {
	/** Constructs an option to allow user-specified primary keys during insertion. */
	public DirectPrimaryKey() {
		// default constructor
	}
}
