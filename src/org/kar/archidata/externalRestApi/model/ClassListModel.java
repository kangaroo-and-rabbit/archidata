package org.kar.archidata.externalRestApi.model;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Set;

public class ClassListModel extends ClassModel {
	public ClassModel valueModel;

	public ClassListModel(final ClassModel valueModel) {
		this.valueModel = valueModel;
	}

	public ClassListModel(final Class<?> clazz, final ModelGroup previousModel) throws IOException {
		this.valueModel = getModel(clazz, previousModel);
	}

	public ClassListModel(final Type model, final ModelGroup previousModel) throws IOException {
		this.valueModel = getModel(model, previousModel);
	}

	public ClassListModel(final ParameterizedType listType, final ModelGroup previousModel) throws IOException {
		final Type model = listType.getActualTypeArguments()[0];
		this.valueModel = getModel(model, previousModel);
	}

	@Override
	public String toString() {
		return "ClassListModel [valueModel=" + this.valueModel + "]";
	}

	@Override
	public void analyze(final ModelGroup group) throws IOException {
		throw new IOException("Analyze can not be done at this phase for List...");
	}

	@Override
	public Set<ClassModel> getAlls() {
		return this.valueModel.getAlls();
	}

	@Override
	public Set<ClassModel> getDependencyGroupModels() {
		return this.valueModel.getDependencyGroupModels();
	}
}
