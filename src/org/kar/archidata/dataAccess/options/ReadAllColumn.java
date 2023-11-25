package org.kar.archidata.dataAccess.options;

import org.kar.archidata.dataAccess.QueryOption;

/** By default some element are not read like createAt and UpdatedAt. This option permit to read it. */
public class ReadAllColumn extends QueryOption {
	public ReadAllColumn() {}
}
