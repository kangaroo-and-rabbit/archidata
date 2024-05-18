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

	public static ClassModel getModel(final Type type, final ModelGroup previousModel) throws IOException {
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

	public static ClassModel getModelBase(
			final Class<?> clazz,
			final Type parameterizedType,
			final ModelGroup previousModel) throws IOException {
		if (clazz == List.class) {
			return new ClassListModel((ParameterizedType) parameterizedType, previousModel);
		}
		if (clazz == Map.class) {
			return new ClassMapModel((ParameterizedType) parameterizedType, previousModel);
		}
		return previousModel.add(clazz);
	}

	public static ClassModel getModel(final Class<?> type, final ModelGroup previousModel) throws IOException {
		if (type == List.class) {
			throw new IOException("Fail to manage parametrized type...");
		}
		if (type == Map.class) {
			throw new IOException("Fail to manage parametrized type...");
		}
		return previousModel.add(type);
	}

}
