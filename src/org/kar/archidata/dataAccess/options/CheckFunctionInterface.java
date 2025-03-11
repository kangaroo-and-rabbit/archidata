package org.kar.archidata.dataAccess.options;

import java.util.List;

import org.kar.archidata.annotation.AnnotationTools;
import org.kar.archidata.dataAccess.DBAccess;
import org.kar.archidata.dataAccess.QueryOptions;

/** By default some element are not read like createAt and UpdatedAt. This option permit to read it. */
public interface CheckFunctionInterface {
	public enum TypeOfCheck {
		DEFAULT, //
		CREATE_MODE, //
		UPDATE_MODE, //
	}

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
			final QueryOptions options,
			TypeOfCheck checkMode) throws Exception;

	default void check(final Object data) throws Exception {
		check(null, "", data, null, null, TypeOfCheck.DEFAULT);
	}

	default void check(final Object data, final TypeOfCheck checkMode) throws Exception {
		check(null, "", data, null, null, checkMode);
	}

	default void check(final String baseName, final Object data) throws Exception {
		check(null, baseName, data, null, null, TypeOfCheck.DEFAULT);
	}

	default void check(final DBAccess ioDb, final String baseName, final Object data) throws Exception {
		check(ioDb, baseName, data, null, null, TypeOfCheck.DEFAULT);
	}

	default void check(final DBAccess ioDb, final String baseName, final Object data, final List<String> modifiedValue)
			throws Exception {
		check(ioDb, baseName, data, modifiedValue, null, TypeOfCheck.DEFAULT);
	}

	default void check(
			final DBAccess ioDb,
			final String baseName,
			final Object data,
			final List<String> modifiedValue,
			final QueryOptions options) throws Exception {
		check(ioDb, baseName, data, modifiedValue, options, TypeOfCheck.DEFAULT);
	}

	default void checkAll(final DBAccess ioDb, final String baseName, final Object data, final QueryOptions options)
			throws Exception {
		check(ioDb, baseName, data, AnnotationTools.getAllFieldsNames(data.getClass()), options);
	}

	default void checkAll(
			final DBAccess ioDb,
			final String baseName,
			final Object data,
			final QueryOptions options,
			final TypeOfCheck checkMode) throws Exception {
		check(ioDb, baseName, data, AnnotationTools.getAllFieldsNames(data.getClass()), options, checkMode);
	}

}
