package org.kar.archidata.externalRestApi.model;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

public class ClassMapModel extends ClassModel {
	public ClassModel keyModel;
	public ClassModel valueModel;
	
	public ClassMapModel(final ClassModel keyModel, final ClassModel valueModel) {
		this.keyModel = keyModel;
		this.valueModel = valueModel;
	}
	
	public ClassMapModel(final Type listTypeKey, final Type listTypeValue, final ModelGroup previousModel)
			throws IOException {
		this.keyModel = getModel(listTypeKey, previousModel);
		this.valueModel = getModel(listTypeValue, previousModel);
	}
	
	public ClassMapModel(final ParameterizedType listType, final ModelGroup previousModel) throws IOException {
		this.keyModel = getModel(listType.getActualTypeArguments()[0], previousModel);
		this.valueModel = getModel(listType.getActualTypeArguments()[1], previousModel);
	}
	
	@Override
	public String toString() {
		return "ClassMapModel [keyModel=" + this.keyModel + ", valueModel=" + this.valueModel + "]";
	}
	
	@Override
	public void analyze(final ModelGroup group) throws IOException {
		throw new IOException("Analyze can not be done at this phase for Map...");
	}
	
	@Override
	public Set<ClassModel> getAlls() {
		final Set<ClassModel> out = new HashSet<>(this.keyModel.getAlls());
		out.addAll(this.valueModel.getAlls());
		return out;
	}
	
	@Override
	public Set<ClassModel> getDependencyGroupModels() {
		final Set<ClassModel> out = new HashSet<>(this.valueModel.getDependencyGroupModels());
		out.addAll(this.keyModel.getDependencyGroupModels());
		return out;
	}
}
