package org.kar.archidata.externalRestApi.model;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class ClassModel {
	public List<Class<?>> originClasses = new ArrayList<>();

	protected boolean isCompatible(final Class<?> clazz) {
		return this.originClasses.contains(clazz);
	}

	public ClassModel getModel(final Type type, final ModelGroup previousModel) throws IOException {
		if (type == List.class) {
			if (type instanceof final ParameterizedType parameterizedType) {
				return new ClassListModel(parameterizedType, previousModel);
			} else {
				throw new IOException("Fail to manage parametrized type...");
			}
		}
		if (type == Map.class) {
			if (type instanceof final ParameterizedType parameterizedType) {
				return new ClassMapModel(parameterizedType, previousModel);
			} else {
				throw new IOException("Fail to manage parametrized type...");
			}
		}
		return previousModel.add((Class<?>) type);
	}
	
}
