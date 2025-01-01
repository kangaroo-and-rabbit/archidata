package org.kar.archidata.dataAccess.options;

public class OptionSpecifyType extends QueryOption {
	public final String name;
	public final Class<?> clazz;

	// To specify the type of an element if the model is a Object.
	public OptionSpecifyType(final String name, final Class<?> clazz) {
		this.clazz = clazz;
		this.name = name;
	}
}
