package org.atriasoft.archidata.externalRestApi.model;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.core.Response;

/**
 * Container for all {@link ClassModel} instances discovered during API analysis.
 *
 * <p>Maintains a registry of class models, creating new ones on demand and
 * deduplicating models for classes that have already been registered.
 */
public class ModelGroup {
	/** Logger instance. */
	static final Logger LOGGER = LoggerFactory.getLogger(ModelGroup.class);
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
	public ClassModel add(Class<?> clazz) {
		if (clazz == Response.class) {
			clazz = Object.class;
		}
		if (clazz == Number.class) {
			return null;
		}
		//LOGGER.trace("Search element {}", clazz.getCanonicalName());
		for (final ClassModel value : this.models) {
			if (value.isCompatible(clazz)) {
				//LOGGER.trace("  ==> return {}", value);
				return value;
			}
		}
		if (clazz.isEnum()) {
			final ClassModel elem = new ClassEnumModel(clazz);
			this.models.add(elem);
			//LOGGER.trace("  ==> return enum {}", elem);
			return elem;
		}
		// create new model:
		final ClassModel elem = new ClassObjectModel(clazz);
		this.models.add(elem);
		//LOGGER.trace("  ==> return object {}", elem);
		return elem;
	}

	/**
	 * Returns all registered class models.
	 * @return the list of class models
	 */
	public List<ClassModel> getModels() {
		return this.models;
	}
}
