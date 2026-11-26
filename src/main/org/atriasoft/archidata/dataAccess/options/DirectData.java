package org.atriasoft.archidata.dataAccess.options;

/** Writes the object as it is: on an insert the primary key of the object is kept (it must be set) and
 * the {@code @CreationTimestamp}, {@code @UpdateTimestamp} and {@code @DataDeleted} fields are not filled
 * by the data layer; on an update the {@code @UpdateTimestamp} field is not refreshed.
 */
public class DirectData extends QueryOption {
	/** Constructs an option to bypass the automatic primary key, timestamp and deleted flag values. */
	public DirectData() {
		// default constructor
	}
}
