package org.kar.archidata.externalRestApi.model;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Temporary class that permit to order the list of API.
record OrderedElement(
		String methodName,
		Method method) {}

public class ApiGroupModel {
	static final Logger LOGGER = LoggerFactory.getLogger(ApiGroupModel.class);

	// Name of the REST end-point name
	public String restEndPoint;
	// Name of the Class
	String name;
	// Origin class reference
	Class<?> originClass;
	// List of all API
	public List<ApiModel> interfaces;

	public ApiGroupModel(final Class<?> clazz, final ModelGroup previousModel) throws Exception {
		this.originClass = clazz;
		// the basic path has no specific elements...
		this.restEndPoint = ApiTool.apiAnnotationGetPath(clazz);
		this.name = clazz.getSimpleName();
		
		final List<String> consumes = ApiTool.apiAnnotationGetConsumes(clazz);
		final List<String> produces = ApiTool.apiAnnotationProduces(clazz);

		// Get all method to order it. This permit to stabilize the generation.
		// JAVA has dynamic allocation of member order, then we need to order it?.
		final List<OrderedElement> orderedElements = new ArrayList<>();
		for (final Method method : clazz.getDeclaredMethods()) {
			final String methodName = method.getName();
			orderedElements.add(new OrderedElement(methodName, method));
		}
		final Comparator<OrderedElement> comparator = Comparator.comparing(OrderedElement::methodName);
		Collections.sort(orderedElements, comparator);
		
		for (final OrderedElement orderedElement : orderedElements) {
			// Check if the path is available
			final RestTypeRequest methodType = ApiTool.apiAnnotationGetTypeRequest2(orderedElement.method());
			if (methodType == null) {
				LOGGER.info("    [{}] {} ==> No methode type @PATH, @GET ...", methodType, orderedElement.methodName());
				continue;
			}
			final ApiModel model = new ApiModel(clazz, orderedElement.method(), this.restEndPoint, consumes, produces,
					previousModel);
			this.interfaces.add(model);
		}
	}
}
