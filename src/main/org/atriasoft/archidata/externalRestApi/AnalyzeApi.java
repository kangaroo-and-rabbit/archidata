package org.atriasoft.archidata.externalRestApi;

import java.util.ArrayList;
import java.util.List;

import org.atriasoft.archidata.externalRestApi.model.ApiGroupModel;
import org.atriasoft.archidata.externalRestApi.model.ClassModel;
import org.atriasoft.archidata.externalRestApi.model.ModelGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Analyzes JAX-RS API classes and their associated data models.
 *
 * <p>This class serves as the central entry point for introspecting REST API
 * endpoints and their data models. It collects API groups and class models,
 * then performs iterative analysis to resolve all model dependencies.
 */
public class AnalyzeApi {
	/** Logger instance. */
	static final Logger LOGGER = LoggerFactory.getLogger(AnalyzeApi.class);
	/** List of analyzed API groups. */
	protected final List<ApiGroupModel> apiModels = new ArrayList<>();
	/** Container for all discovered class models. */
	protected final ModelGroup modelGroup = new ModelGroup();

	/**
	 * Registers multiple data model classes and analyzes them.
	 * @param classes the list of model classes to add
	 * @throws Exception if analysis fails
	 */
	public void addAllModel(final List<Class<?>> classes) throws Exception {
		this.modelGroup.addAll(classes);
		analyzeModels();
	}

	/**
	 * Registers a single data model class and analyzes it.
	 * @param clazz the model class to add
	 * @throws Exception if analysis fails
	 */
	public void addModel(final Class<?> clazz) throws Exception {
		this.modelGroup.add(clazz);
		analyzeModels();
	}

	/**
	 * Registers a single REST API class and analyzes its models.
	 * @param clazz the JAX-RS resource class to add
	 * @throws Exception if analysis fails
	 */
	public void addApi(final Class<?> clazz) throws Exception {
		this.apiModels.add(new ApiGroupModel(clazz, this.modelGroup));
		analyzeModels();
	}

	/**
	 * Registers multiple REST API classes and analyzes their models.
	 * @param classes the list of JAX-RS resource classes to add
	 * @throws Exception if analysis fails
	 */
	public void addAllApi(final List<Class<?>> classes) throws Exception {
		for (final Class<?> clazz : classes) {
			this.apiModels.add(new ApiGroupModel(clazz, this.modelGroup));
		}
		analyzeModels();
	}

	/**
	 * Returns all registered API group models.
	 * @return the list of API group models
	 */
	public List<ApiGroupModel> getAllApi() {
		return this.apiModels;
	}

	/**
	 * Returns all registered class models.
	 * @return the list of class models
	 */
	public List<ClassModel> getAllModel() {
		return this.modelGroup.getModels();
	}

	private void analyzeModels() throws Exception {
		final List<ClassModel> dones = new ArrayList<>();
		while (dones.size() < getAllModel().size()) {
			final List<ClassModel> copyList = new ArrayList<>(this.modelGroup.getModels());
			for (final ClassModel model : copyList) {
				if (dones.contains(model)) {
					continue;
				}
				LOGGER.debug("Analyze: {}", model);
				model.analyze(this.modelGroup);
				dones.add(model);
			}
		}
	}

	/**
	 * Finds class models whose origin class matches one of the given classes.
	 * @param search the list of classes to search for
	 * @return the matching class models, or {@code null} if none found
	 */
	public List<ClassModel> getCompatibleModels(final List<Class<?>> search) {
		final List<ClassModel> out = new ArrayList<>();
		for (final ClassModel model : getAllModel()) {
			if (search.contains(model.getOriginClasses())) {
				out.add(model);
			}
		}
		if (out.isEmpty()) {
			return null;
		}
		return out;
	}

	/**
	 * Logs all registered API groups at INFO level.
	 */
	public void displayAllApi() {
		LOGGER.info("List all API:");
		for (final ApiGroupModel model : getAllApi()) {
			LOGGER.info("    - {}: {}", model.name, model.getClass().getCanonicalName());
		}

	}

	/**
	 * Logs all registered class models at INFO level.
	 */
	public void displayAllModel() {
		LOGGER.info("List all Model:");
		for (final ClassModel model : getAllModel()) {
			final StringBuilder out = new StringBuilder();
			for (final ClassModel classModel : model.getAlls()) {
				out.append(classModel.getOriginClasses().getCanonicalName());
				out.append(",");
			}
			LOGGER.info("    - {}", out.toString());
		}
	}

}
