package org.atriasoft.archidata.externalRestApi.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.ws.rs.core.Response;

/**
 * Container for all {@link ClassModel} instances discovered during API analysis.
 *
 * <p>Maintains a registry of class models, creating new ones on demand and
 * deduplicating models for classes that have already been registered.
 */
public class ModelGroup {
	/** The list of registered class models. */
	public List<ClassModel> models = new ArrayList<>();

	/** Constructs an empty model group. */
	public ModelGroup() {}

	/**
	 * Registers multiple classes as models.
	 * @param classes the list of classes to add
	 */
	public void addAll(final List<Class<?>> classes) {
		for (final Class<?> clazz : classes) {
			add(clazz);
		}
	}

	/**
	 * Registers a class as a model, returning an existing model if already registered.
	 * @param clazz the class to add (Response is mapped to Object, Number returns {@code null})
	 * @return the class model for the given class, or {@code null} for Number
	 */
	public ClassModel add(final Class<?> clazz) {
		final Class<?> modelClass = clazz == Response.class ? Object.class : clazz;
		if (modelClass == Number.class) {
			return null;
		}
		for (final ClassModel value : this.models) {
			if (value.isCompatible(modelClass)) {
				return value;
			}
		}
		if (modelClass.isEnum()) {
			final ClassModel elem = new ClassEnumModel(modelClass);
			this.models.add(elem);
			return elem;
		}
		// create new model:
		final ClassModel elem = new ClassObjectModel(modelClass);
		this.models.add(elem);
		return elem;
	}

	/**
	 * Returns all registered class models.
	 * @return the live list (not a copy): it must not be modified by the caller
	 */
	public List<ClassModel> getModels() {
		return this.models;
	}
}
