package org.kar.archidata.dataAccess.options;

import java.util.List;

import org.kar.archidata.annotation.AnnotationTools;
import org.kar.archidata.dataAccess.DataAccess;
import org.kar.archidata.dataAccess.QueryOptions;

/** By default some element are not read like createAt and UpdatedAt. This option permit to read it. */
public interface CheckFunctionInterface {
	/** This function implementation is design to check if the updated class is valid of not for insertion
	 * @param baseName NAme of the object to be precise with the use of what fail.
	 * @param data The object that might be injected.
	 * @param filterValue List of fields that might be check. If null, then all column must be checked.
	 * @throws Exception Exception is generate if the data are incorrect. */
	void check(
			final DataAccess ioDb,
			final String baseName,
			Object data,
			List<String> filterValue,
			final QueryOptions options) throws Exception;

	default void checkAll(final DataAccess ioDb, final String baseName, final Object data, final QueryOptions options)
			throws Exception {
		check(ioDb, baseName, data, AnnotationTools.getAllFieldsNames(data.getClass()), options);
	}

}
