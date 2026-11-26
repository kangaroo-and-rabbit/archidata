package org.atriasoft.archidata.externalRestApi;

import org.atriasoft.archidata.externalRestApi.model.ClassModel;

/**
 * Path and type-name helpers shared by the OpenAPI and Draw.io generators.
 */
final class ApiNaming {

	private ApiNaming() {
		// utility class
	}

	/**
	 * Normalizes a REST path by collapsing double slashes and removing trailing slashes.
	 *
	 * @param path the REST path to normalize
	 * @return the normalized path
	 */
	static String normalizePath(final String path) {
		String normalized = path.replaceAll("//+", "/");
		if (normalized.length() > 1 && normalized.endsWith("/")) {
			normalized = normalized.substring(0, normalized.length() - 1);
		}
		return normalized;
	}

	/**
	 * Gets the simple name of the class behind a model, or the full name for an anonymous class.
	 *
	 * @param model the model to name
	 * @return the name, or {@code null} when the model has no origin class
	 */
	static String simpleName(final ClassModel model) {
		final Class<?> clazz = model.getOriginClasses();
		if (clazz == null) {
			return null;
		}
		final String name = clazz.getSimpleName();
		if (name.isEmpty()) {
			return clazz.getName();
		}
		return name;
	}
}
