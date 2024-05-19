package org.kar.archidata.externalRestApi.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ClassEnumModel extends ClassModel {

	protected ClassEnumModel(final Class<?> clazz) {
		this.originClasses = clazz;
	}

	@Override
	public String toString() {
		final StringBuilder out = new StringBuilder();
		out.append("ClassEnumModel [");
		out.append(this.originClasses.getCanonicalName());
		out.append("]");
		return out.toString();
	}

	final List<String> listOfValues = new ArrayList<>();

	@Override
	public void analyze(final ModelGroup group) throws IOException {
		// TODO: check if we really need to have multiple type for enums ???
		final Class<?> clazz = this.originClasses;
		final Object[] arr = clazz.getEnumConstants();
		for (final Object elem : arr) {
			this.listOfValues.add(elem.toString());
		}
	}

	public List<String> getListOfValues() {
		return this.listOfValues;
	}

	@Override
	public Set<ClassModel> getAlls() {
		return Set.of(this);
	}
}
