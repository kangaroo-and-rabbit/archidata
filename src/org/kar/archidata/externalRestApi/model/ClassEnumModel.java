package org.kar.archidata.externalRestApi.model;

public class ClassEnumModel extends ClassModel {

	protected ClassEnumModel(final Class<?> clazz) {
		this.originClasses.add(clazz);
	}

	@Override
	public String toString() {
		final StringBuilder out = new StringBuilder();
		out.append("ClassEnumModel [");
		for (final Class<?> elem : this.originClasses) {
			out.append(elem.getCanonicalName());
		}
		out.append("]");
		return out.toString();
	}
}
