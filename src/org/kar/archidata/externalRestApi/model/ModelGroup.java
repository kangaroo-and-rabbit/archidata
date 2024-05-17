package org.kar.archidata.externalRestApi.model;

import java.util.ArrayList;
import java.util.List;

public class ModelGroup {
	public List<ClassModel> previousModel = new ArrayList<>();
	
	public ModelGroup() {}

	public ModelGroup(final List<ClassModel> models) {
		this.previousModel = models;
	}
	
	public ClassModel add(final Class<?> clazz) {
		for (final ClassModel value : this.previousModel) {
			if (value.isCompatible(clazz)) {
				return value;
			}
		}
		if (clazz.isEnum()) {
			final ClassModel elem = new ClassEnumModel(clazz);
		}
		// create new model:

		return null;
	}
}
