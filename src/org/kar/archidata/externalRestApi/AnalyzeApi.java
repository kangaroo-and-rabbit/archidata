package org.kar.archidata.externalRestApi;

import java.util.ArrayList;
import java.util.List;

import org.kar.archidata.externalRestApi.model.ApiGroupModel;
import org.kar.archidata.externalRestApi.model.ClassModel;
import org.kar.archidata.externalRestApi.model.ModelGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnalyzeApi {
	static final Logger LOGGER = LoggerFactory.getLogger(AnalyzeApi.class);
	protected final List<ApiGroupModel> apiModels = new ArrayList<>();
	protected final ModelGroup modelGroup = new ModelGroup();

	public void addAllModel(final List<Class<?>> classes) throws Exception {
		this.modelGroup.addAll(classes);
		analyzeModels();
	}

	public void addModel(final Class<?> clazz) throws Exception {
		this.modelGroup.add(clazz);
		analyzeModels();
	}

	public void addApi(final Class<?> clazz) throws Exception {
		this.apiModels.add(new ApiGroupModel(clazz, this.modelGroup));
		analyzeModels();
	}

	public void addAllApi(final List<Class<?>> classes) throws Exception {
		for (final Class<?> clazz : classes) {
			this.apiModels.add(new ApiGroupModel(clazz, this.modelGroup));
		}
		analyzeModels();
	}

	public List<ApiGroupModel> getAllApi() {
		return this.apiModels;
	}

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
				LOGGER.info("Analyze: {}", model);
				model.analyze(this.modelGroup);
				dones.add(model);
			}
		}
	}

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

	public void displayAllApi() {
		LOGGER.info("List all API:");
		for (final ApiGroupModel model : getAllApi()) {
			LOGGER.info("    - {}: {}", model.name, model.getClass().getCanonicalName());
		}

	}

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
