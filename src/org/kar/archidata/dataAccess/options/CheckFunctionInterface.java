package org.kar.archidata.dataAccess.options;

import java.util.List;

/** By default some element are not read like createAt and UpdatedAt. This option permit to read it. */
public interface CheckFunctionInterface {
	/** This function implementation is design to check if the updated class is valid of not for insertion
	 * @param data The object that might be injected.
	 * @param filterValue List of fields that might be check. If null, then all column must be checked.
	 * @throws Exception Exception is generate if the data are incorrect. */
	void check(Object data, List<String> filterValue) throws Exception;

}
