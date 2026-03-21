package org.atriasoft.archidata.dataAccess.options;

/** By default some element are not read like createAt and UpdatedAt. This option permit to read it. */
public class ReadAllColumn extends QueryOption {
	/** Constructs an option to read all columns including createAt and updateAt. */
	public ReadAllColumn() {
		// default constructor
	}
}
