package org.atriasoft.archidata.externalRestApi.model;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.atriasoft.archidata.annotation.apiGenerator.ApiGenerationMode;
import org.atriasoft.archidata.tools.AnnotationCreator;

/**
 * Represents an enum type model extracted from a Java enum class for API generation.
 *
 * <p>Stores the enum constant names and their associated values (either from
 * a {@code getValue()} method or the constant name itself).
 */
public class ClassEnumModel extends ClassModel {
	/**
	 * Constructs an enum model for the given enum class.
	 * @param clazz the Java enum class to model
	 */
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

	final Map<String, Object> listOfValues = new TreeMap<>();

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

	/**
	 * Returns the map of enum constant names to their serialized values.
	 * @return the enum values map
	 */
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
