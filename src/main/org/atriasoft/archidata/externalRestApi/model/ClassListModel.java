package org.atriasoft.archidata.externalRestApi.model;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Set;

/**
 * Represents a List type model wrapping a single element class model.
 *
 * <p>Used to model {@code List<T>} fields in API generation.
 */
public class ClassListModel extends ClassModel {
	/** The class model for the list element type. */
	public ClassModel valueModel;

	/**
	 * Constructs a list model from a pre-resolved element model.
	 * @param valueModel the class model for the list element type
	 */
	public ClassListModel(final ClassModel valueModel) {
		this.valueModel = valueModel;
	}

	/**
	 * Constructs a list model by resolving the element class.
	 * @param clazz the element class
	 * @param previousModel the model group for resolving types
	 * @throws IOException if type resolution fails
	 */
	public ClassListModel(final Class<?> clazz, final ModelGroup previousModel) throws IOException {
		this.valueModel = getModel(clazz, previousModel);
	}

	/**
	 * Constructs a list model by resolving the element type.
	 * @param model the generic element type
	 * @param previousModel the model group for resolving types
	 * @throws IOException if type resolution fails
	 */
	public ClassListModel(final Type model, final ModelGroup previousModel) throws IOException {
		this.valueModel = getModel(model, previousModel);
	}

	/**
	 * Constructs a list model from a parameterized type (e.g., {@code List<String>}).
	 * @param listType the parameterized list type
	 * @param previousModel the model group for resolving types
	 * @throws IOException if type resolution fails
	 */
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
