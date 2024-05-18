package org.kar.archidata.externalRestApi.model;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;

public class ClassMapModel extends ClassModel {
	public ClassModel keyModel;
	public ClassModel valueModel;

	public ClassMapModel(final ClassModel keyModel, final ClassModel valueModel) {
		this.keyModel = keyModel;
		this.valueModel = valueModel;
	}

	public ClassMapModel(final ParameterizedType listType, final ModelGroup previousModel) throws IOException {
		this.keyModel = getModel(listType.getActualTypeArguments()[0], previousModel);
		this.valueModel = getModel(listType.getActualTypeArguments()[1], previousModel);
	}

	@Override
	public String toString() {
		return "ClassMapModel [keyModel=" + this.keyModel + ", valueModel=" + this.valueModel + "]";
	}

}
