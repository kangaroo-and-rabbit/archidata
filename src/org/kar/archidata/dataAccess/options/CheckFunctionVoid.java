package org.kar.archidata.dataAccess.options;

import java.util.List;

import org.kar.archidata.dataAccess.DBAccess;
import org.kar.archidata.dataAccess.QueryOptions;

/** By default some element are not read like createAt and UpdatedAt. This option permit to read it. */
public class CheckFunctionVoid implements CheckFunctionInterface {

	@Override
	public void check(
			final DBAccess ioDb,
			final String baseName,
			final Object data,
			final List<String> modifiedValue,
			final QueryOptions options,
			final TypeOfCheck checkMode) throws Exception {

	}

}
