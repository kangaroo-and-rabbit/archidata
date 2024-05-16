package org.kar.archidata.externalRestApi.model;

import java.util.ArrayList;
import java.util.List;

public class ModelGroup {
	public final List<ClassModel> previousModel = new ArrayList<>();

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
