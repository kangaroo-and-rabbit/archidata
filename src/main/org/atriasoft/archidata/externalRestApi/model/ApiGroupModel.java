package org.atriasoft.archidata.externalRestApi.model;

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

/**
 * Represents a group of REST API endpoints derived from a single JAX-RS resource class.
 *
 * <p>Collects all annotated methods from the class and creates {@link ApiModel}
 * instances for each endpoint, sorted alphabetically by method name.
 */
public class ApiGroupModel {
	/** Logger instance. */
	static final Logger LOGGER = LoggerFactory.getLogger(ApiGroupModel.class);

	/** The base REST endpoint path for this group. */
	public String restEndPoint;
	/** The simple name of the JAX-RS resource class. */
	public String name;
	/** The original JAX-RS resource class. */
	public Class<?> originClass;
	/** The list of API endpoint models in this group. */
	public List<ApiModel> interfaces = new ArrayList<>();

	/**
	 * Finds an API endpoint by its method name.
	 * @param name the method name to search for
	 * @return the matching {@link ApiModel}, or {@code null} if not found
	 */
	public ApiModel getInterfaceNamed(final String name) {
		for (final ApiModel model : this.interfaces) {
			if (name.equals(model.name)) {
				return model;
			}
		}
		return null;
	}

	/**
	 * Constructs an API group model by introspecting all methods of the given class.
	 * @param clazz the JAX-RS resource class to introspect
	 * @param previousModel the model group for resolving parameter and return types
	 * @throws Exception if introspection fails
	 */
	public ApiGroupModel(final Class<?> clazz, final ModelGroup previousModel) throws Exception {
		this.originClass = clazz;
		// the basic path has no specific elements...
		this.restEndPoint = ApiTool.apiAnnotationGetPath(clazz);
		if (this.restEndPoint == null) {
			this.restEndPoint = "";
		}
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
				LOGGER.trace("    [{}] {} ==> No methode type @PATH, @GET ...", methodType,
						orderedElement.methodName());
				continue;
			}
			final ApiModel model = new ApiModel(clazz, orderedElement.method(), this.restEndPoint, consumes, produces,
					previousModel);
			this.interfaces.add(model);
		}
	}
}
