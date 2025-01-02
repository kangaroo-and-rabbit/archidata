package org.kar.archidata.dataAccess.options;

public class OptionSpecifyType extends QueryOption {
	public final String name;
	public final Class<?> clazz;
	public final boolean isList;

	// To specify the type of an element if the model is a Object.
	public OptionSpecifyType(final String name, final Class<?> clazz) {
		this.clazz = clazz;
		this.name = name;
		this.isList = false;
	}

	public OptionSpecifyType(final String name, final Class<?> clazz, final boolean isList) {
		this.clazz = clazz;
		this.name = name;
		this.isList = isList;
	}
}
