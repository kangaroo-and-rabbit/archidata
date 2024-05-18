package org.kar.archidata.externalRestApi.model;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModelGroup {
	static final Logger LOGGER = LoggerFactory.getLogger(ModelGroup.class);
	public List<ClassModel> previousModel = new ArrayList<>();

	public ModelGroup() {}

	public ModelGroup(final List<ClassModel> models) {
		this.previousModel = models;
	}

	public ClassModel add(final Class<?> clazz) {
		//LOGGER.trace("Search element {}", clazz.getCanonicalName());
		for (final ClassModel value : this.previousModel) {
			if (value.isCompatible(clazz)) {
				//LOGGER.trace("  ==> return {}", value);
				return value;
			}
		}
		if (clazz.isEnum()) {
			final ClassModel elem = new ClassEnumModel(clazz);
			this.previousModel.add(elem);
			//LOGGER.trace("  ==> return enum {}", elem);
			return elem;
		}
		// create new model:
		final ClassModel elem = new ClassObjectModel(clazz);
		this.previousModel.add(elem);
		//LOGGER.trace("  ==> return object {}", elem);
		return elem;
	}
}
