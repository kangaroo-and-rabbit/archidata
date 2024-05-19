package org.kar.archidata.externalRestApi;

import java.util.ArrayList;
import java.util.List;

import org.kar.archidata.externalRestApi.model.ClassModel;
import org.kar.archidata.externalRestApi.model.ModelGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnalyzeModel {
	static final Logger LOGGER = LoggerFactory.getLogger(AnalyzeModel.class);

	public static void fillModel(final List<ClassModel> models) throws Exception {
		final ModelGroup previousModel = new ModelGroup(models);
		final List<ClassModel> dones = new ArrayList<>();
		while (dones.size() < previousModel.previousModel.size()) {
			LOGGER.info("Do a cycle of annalyze : new model detected: {} < {}", dones.size(),
					previousModel.previousModel.size());
			final List<ClassModel> copyList = new ArrayList<>(previousModel.previousModel);
			for (final ClassModel model : copyList) {
				if (dones.contains(model)) {
					continue;
				}
				LOGGER.info("Analyze: {}", model);
				model.analyze(previousModel);
				dones.add(model);
			}
		}
	}

}
