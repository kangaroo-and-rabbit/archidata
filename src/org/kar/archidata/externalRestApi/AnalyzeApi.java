package org.kar.archidata.externalRestApi;

import java.util.ArrayList;
import java.util.List;

import org.kar.archidata.externalRestApi.model.ApiGroupModel;
import org.kar.archidata.externalRestApi.model.ClassModel;
import org.kar.archidata.externalRestApi.model.ModelGroup;

public class AnalyzeApi {
	public List<ApiGroupModel> apiModels = new ArrayList<>();
	public List<ClassModel> classModels = new ArrayList<>();

	public void createApi(final List<Class<?>> classs) throws Exception {
		final ModelGroup previousModel = new ModelGroup(this.classModels);
		for (final Class<?> clazz : classs) {
			final ApiGroupModel parsed = new ApiGroupModel(clazz, previousModel);
			this.apiModels.add(parsed);
		}

	}

}
