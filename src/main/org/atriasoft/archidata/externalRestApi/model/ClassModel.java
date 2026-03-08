package org.atriasoft.archidata.externalRestApi.model;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.atriasoft.archidata.annotation.apiGenerator.ApiGenerationMode;
import org.atriasoft.archidata.tools.AnnotationCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ClassModel {
	private static final Logger LOGGER = LoggerFactory.getLogger(ClassModel.class);
	protected boolean analyzeDone = false;
	protected Class<?> originClasses = null;
	protected ApiGenerationMode apiGenerationMode = AnnotationCreator.createAnnotation(ApiGenerationMode.class);
	protected Set<ClassModel> dependencyModels = new HashSet<>();

	public Class<?> getOriginClasses() {
		return this.originClasses;
	}

	public ApiGenerationMode getApiGenerationMode() {
		return this.apiGenerationMode;
	}

	protected boolean isCompatible(final Class<?> clazz) {
		return this.originClasses == clazz;
	}

	@Deprecated
	public Set<ClassModel> getDependencyModels() {
		return this.dependencyModels;
	}

	public abstract Set<ClassModel> getDependencyGroupModels();

	public static ClassModel getModel(final Type type, final ModelGroup previousModel) throws IOException {
		if (type instanceof final ParameterizedType paramType) {
			final Type[] typeArguments = paramType.getActualTypeArguments();
			final Type rawType = paramType.getRawType();
			if (rawType instanceof final Class<?> rawClass) {
				if (List.class.isAssignableFrom(rawClass)) {
					return new ClassListModel(typeArguments[0], previousModel);
				}
				if (Map.class.isAssignableFrom(rawClass)) {
					return new ClassMapModel(typeArguments[0], typeArguments[1], previousModel);
				}
			}
			throw new IOException("Fail to manage parametrized type... '" + rawType + "'");
		}
		return previousModel.add((Class<?>) type);
	}

	public static ClassModel getModelBase(
			final Class<?> clazz,
			final Type parameterizedType,
			final ModelGroup previousModel) throws IOException {
		/*
		if (clazz == List.class) {
			return new ClassListModel((ParameterizedType) parameterizedType, previousModel);
		}
		if (clazz == Map.class) {
			return new ClassMapModel((ParameterizedType) parameterizedType, previousModel);
		}
		return previousModel.add(clazz);
		*/
		return getModel(parameterizedType, previousModel);
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

	public abstract void analyze(final ModelGroup group) throws Exception;

	public abstract Set<ClassModel> getAlls();

	public List<String> getReadOnlyFields() {
		return List.of();
	}

	public List<String> getCreateFields() {
		return List.of();
	}

	public List<String> getUpdateFields() {
		return List.of();
	}

}
