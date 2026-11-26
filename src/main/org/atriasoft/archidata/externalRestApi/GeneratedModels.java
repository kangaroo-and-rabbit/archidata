package org.atriasoft.archidata.externalRestApi;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Function;

import org.atriasoft.archidata.externalRestApi.model.ClassModel;

/**
 * Structure shared by the generators that map the analyzed models to their own elements: the
 * table of the native types (written by each generator as data) and the loop that adds every
 * other model.
 */
final class GeneratedModels {

	private GeneratedModels() {
		// utility class
	}

	/**
	 * Adds one native element when the analyzed API uses at least one of the given classes.
	 *
	 * @param <E> the element type of the generator
	 * @param api the analyzed API
	 * @param elements the elements of the generator, completed in place
	 * @param classes the Java classes mapped to the element
	 * @param factory builds the element from the matching models
	 */
	static <E> void addNative(
			final AnalyzeApi api,
			final List<E> elements,
			final List<Class<?>> classes,
			final Function<List<ClassModel>, E> factory) {
		final List<ClassModel> models = api.getCompatibleModels(classes);
		if (models != null) {
			elements.add(factory.apply(models));
		}
	}

	/**
	 * Adds an element for every model of the API that no element handles yet.
	 *
	 * @param <E> the element type of the generator
	 * @param api the analyzed API
	 * @param elements the elements of the generator, completed in place
	 * @param isCompatible tells whether an element already handles a model
	 * @param factory builds the element of a model
	 */
	static <E> void addRemaining(
			final AnalyzeApi api,
			final List<E> elements,
			final BiPredicate<E, ClassModel> isCompatible,
			final Function<ClassModel, E> factory) {
		for (final ClassModel model : api.getAllModel()) {
			boolean alreadyExist = false;
			for (final E elem : elements) {
				if (isCompatible.test(elem, model)) {
					alreadyExist = true;
					break;
				}
			}
			if (alreadyExist) {
				continue;
			}
			elements.add(factory.apply(model));
		}
	}
}
