package org.kar.archidata.dataAccess.options;

import java.util.List;

/** By default some element are not read like createAt and UpdatedAt. This option permit to read it. */
public class CheckFunctionVoid implements CheckFunctionInterface {
	@Override
	public void check(final String baseName, Object data, List<String> filterValue) {

	}

}
