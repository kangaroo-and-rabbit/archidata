package org.atriasoft.archidata.externalRestApi.model;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.atriasoft.archidata.annotation.AnnotationTools;
import org.atriasoft.archidata.annotation.apiGenerator.ApiGenerationMode;
import org.atriasoft.archidata.tools.AnnotationCreator;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Represents an enum type model extracted from a Java enum class for API generation.
 *
 * <p>Stores the enum constant names and their associated values (either from
 * the value of the method carrying {@code @JsonValue}, or the constant name itself).
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

		// Jackson writes the name of a constant, unless a no-argument method carries @JsonValue: only then the
		// value of this method is what the clients send and receive.
		final Method valueMethod = findJsonValueMethod(clazz);
		if (valueMethod != null) {
			try {
				for (final Object constant : constants) {
					this.listOfValues.put(constant.toString(), valueMethod.invoke(constant));
				}
				return;
			} catch (final ReflectiveOperationException e) {
				// not callable: the name of the constant is its value.
				this.listOfValues.clear();
			}
		}

		for (final Object elem : constants) {
			this.listOfValues.put(elem.toString(), elem.toString());
		}
	}

	/** The public no-argument method of an enum that carries {@code @JsonValue}, or {@code null}. */
	private static Method findJsonValueMethod(final Class<?> clazz) {
		for (final Method method : clazz.getMethods()) {
			if (method.getParameterCount() != 0 || method.getReturnType() == void.class) {
				continue;
			}
			for (final JsonValue annotation : AnnotationTools.getAnnotationsIncludingInterfaces(method,
					JsonValue.class)) {
				if (annotation.value()) {
					return method;
				}
			}
		}
		return null;
	}

	/**
	 * Returns the map of enum constant names to their serialized values.
	 * @return a read-only view of the enum values map
	 */
	public Map<String, Object> getListOfValues() {
		return Collections.unmodifiableMap(this.listOfValues);
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
