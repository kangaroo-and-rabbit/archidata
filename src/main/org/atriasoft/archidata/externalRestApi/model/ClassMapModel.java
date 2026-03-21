package org.atriasoft.archidata.externalRestApi.model;

import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a Map type model with separate key and value class models.
 *
 * <p>Used to model {@code Map<K, V>} fields in API generation.
 */
public class ClassMapModel extends ClassModel {
	/** The class model for the map key type. */
	public ClassModel keyModel;
	/** The class model for the map value type. */
	public ClassModel valueModel;

	/**
	 * Constructs a map model from pre-resolved key and value models.
	 * @param keyModel the class model for the key type
	 * @param valueModel the class model for the value type
	 */
	public ClassMapModel(final ClassModel keyModel, final ClassModel valueModel) {
		this.keyModel = keyModel;
		this.valueModel = valueModel;
	}

	/**
	 * Constructs a map model by resolving the key and value types.
	 * @param listTypeKey the generic type of the map key
	 * @param listTypeValue the generic type of the map value
	 * @param previousModel the model group for resolving types
	 * @throws IOException if type resolution fails
	 */
	public ClassMapModel(final Type listTypeKey, final Type listTypeValue, final ModelGroup previousModel)
			throws IOException {
		this.keyModel = getModel(listTypeKey, previousModel);
		this.valueModel = getModel(listTypeValue, previousModel);
	}

	/**
	 * Constructs a map model from a parameterized type (e.g., {@code Map<String, Integer>}).
	 * @param listType the parameterized map type
	 * @param previousModel the model group for resolving types
	 * @throws IOException if type resolution fails
	 */
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
