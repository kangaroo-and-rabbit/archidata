package org.atriasoft.archidata.dataAccess.options;

import java.util.List;

import org.atriasoft.archidata.annotation.AnnotationTools;
import org.atriasoft.archidata.dataAccess.DBAccess;
import org.atriasoft.archidata.dataAccess.QueryOptions;

/** By default some element are not read like createAt and UpdatedAt. This option permit to read it. */
public interface CheckFunctionInterface {
	/** This function implementation is design to check if the updated class is valid of not for insertion
	 * @param baseName Name of the object to be precise with the use of what fail.
	 * @param data The object that might be injected.
	 * @param modifiedValue List of fields that might be check. If null, then all column must be checked.
	 * @throws Exception Exception is generate if the data are incorrect. */
	void check(
			final DBAccess ioDb,
			final String baseName,
			Object data,
			List<String> modifiedValue,
			final QueryOptions options) throws Exception;

	default void checkAll(final DBAccess ioDb, final String baseName, final Object data, final QueryOptions options)
			throws Exception {
		check(ioDb, baseName, data, AnnotationTools.getAllFieldsNames(data.getClass()), options);
	}

}
