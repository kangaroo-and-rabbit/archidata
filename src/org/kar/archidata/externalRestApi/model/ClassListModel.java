package org.kar.archidata.externalRestApi.model;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class ClassListModel extends ClassModel {
	public ClassModel valueModel;

	public ClassListModel(final ClassModel valueModel) {
		this.valueModel = valueModel;
	}

	public ClassListModel(final Class<?> clazz, final ModelGroup previousModel) throws IOException {
		this.valueModel = getModel(clazz, previousModel);
	}

	public ClassListModel(final ParameterizedType listType, final ModelGroup previousModel) throws IOException {
		final Type model = listType.getActualTypeArguments()[0];
		this.valueModel = getModel(model, previousModel);
	}

	@Override
	public String toString() {
		return "ClassListModel [valueModel=" + this.valueModel + "]";
	}

}
