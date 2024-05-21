package org.kar.archidata.externalRestApi.model;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.core.Response;

public class ModelGroup {
	static final Logger LOGGER = LoggerFactory.getLogger(ModelGroup.class);
	public List<ClassModel> models = new ArrayList<>();

	public ModelGroup() {}

	public void addAll(final List<Class<?>> classes) {
		for (final Class<?> clazz : classes) {
			add(clazz);
		}
	}

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

	public List<ClassModel> getModels() {
		return this.models;
	}
}
