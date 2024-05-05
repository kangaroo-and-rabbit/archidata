package org.kar.archidata.dataAccess.options;

/** By default some element are not read like createAt and UpdatedAt. This option permit to read it. */
public class CheckFunction extends QueryOption {
	private final CheckFunctionInterface checker;

	public CheckFunction(final CheckFunctionInterface checker) {
		this.checker = checker;
	}

	public CheckFunctionInterface getChecker() {
		return this.checker;
	}
}
