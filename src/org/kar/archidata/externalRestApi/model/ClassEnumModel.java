package org.kar.archidata.externalRestApi.model;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.kar.archidata.annotation.ApiGenerationMode;
import org.kar.archidata.tools.AnnotationCreator;

public class ClassEnumModel extends ClassModel {
	protected ClassEnumModel(final Class<?> clazz) {
		this.originClasses = clazz;
		this.apiGenerationMode = AnnotationCreator.createAnnotation(ApiGenerationMode.class, "readable", true,
				"creatable", false, "updatable", false);
	}

	@Override
	public String toString() {
		final StringBuilder out = new StringBuilder();
		out.append("ClassEnumModel [");
		out.append(this.originClasses.getCanonicalName());
		out.append("]");
		return out.toString();
	}

	final Map<String, Object> listOfValues = new HashMap<>();

	@Override
	public void analyze(final ModelGroup group) throws IOException {
		if (this.analyzeDone) {
			return;
		}
		this.analyzeDone = true;
		final Class<?> clazz = this.originClasses;
		final Object[] constants = clazz.getEnumConstants();

		// Try to get a get Value element to serialize:
		try {
			final Method getValueMethod = clazz.getMethod("getValue");
			for (final Object constant : constants) {
				final String name = constant.toString();
				final Object value = getValueMethod.invoke(constant);
				this.listOfValues.put(name, value);
			}
			return;
		} catch (final Exception e) {
			//e.printStackTrace();
		}

		for (final Object elem : constants) {
			this.listOfValues.put(elem.toString(), elem.toString());
		}
	}

	public Map<String, Object> getListOfValues() {
		return this.listOfValues;
	}

	@Override
	public Set<ClassModel> getAlls() {
		return Set.of(this);
	}

	@Override
	public Set<ClassModel> getDependencyGroupModels() {
		return Set.of(this);
	}
}
