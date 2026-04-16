package org.atriasoft.archidata.dataAccess.options;

/** This permit to insert or update data without modification of the _id, `@CreateAt` or `@UpdateAt`
 */
public class DirectData extends QueryOption {
	/** Constructs an option to bypass automatic _id, createAt, and updateAt modifications. */
	public DirectData() {
		// default constructor
	}
}
