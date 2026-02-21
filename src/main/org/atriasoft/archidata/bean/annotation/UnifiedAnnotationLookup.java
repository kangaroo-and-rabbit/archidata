package org.atriasoft.archidata.bean.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Unified annotation lookup that merges annotations from multiple sources
 * (field, getter method, setter method, and their interface equivalents)
 * into a single consolidated view.
 *
 * <p>Priority order (first found wins for single-value lookups):
 * <ol>
 *   <li>Field annotations</li>
 *   <li>Getter method annotations (direct)</li>
 *   <li>Setter method annotations (direct)</li>
 *   <li>Getter method annotations on interfaces</li>
 *   <li>Setter method annotations on interfaces</li>
 * </ol>
 *
 * <p>This class is stateless and produces immutable annotation maps.
 */
public final class UnifiedAnnotationLookup {

	private UnifiedAnnotationLookup() {}

	/**
	 * Build a consolidated annotation cache for a property, merging annotations from
	 * the field, getter, and setter (including their interface declarations).
	 *
	 * @param field  the field (may be null if property is method-only)
	 * @param getter the getter method (may be null)
	 * @param setter the setter method (may be null)
	 * @return unmodifiable map of annotation class → first annotation instance found
	 */
	public static Map<Class<? extends Annotation>, Annotation> buildAnnotationCache(
			final Field field,
			final Method getter,
			final Method setter) {
		final Map<Class<? extends Annotation>, Annotation> cache = new LinkedHashMap<>();

		// 1. Field annotations (highest priority)
		if (field != null) {
			for (final Annotation ann : field.getDeclaredAnnotations()) {
				cache.putIfAbsent(ann.annotationType(), ann);
			}
		}

		// 2. Getter method annotations (direct)
		if (getter != null) {
			for (final Annotation ann : getter.getDeclaredAnnotations()) {
				cache.putIfAbsent(ann.annotationType(), ann);
			}
		}

		// 3. Setter method annotations (direct)
		if (setter != null) {
			for (final Annotation ann : setter.getDeclaredAnnotations()) {
				cache.putIfAbsent(ann.annotationType(), ann);
			}
		}

		// 4. Getter method annotations on interfaces
		if (getter != null) {
			collectInterfaceAnnotations(getter, cache);
		}

		// 5. Setter method annotations on interfaces
		if (setter != null) {
			collectInterfaceAnnotations(setter, cache);
		}

		return Collections.unmodifiableMap(cache);
	}

	/**
	 * Get a specific annotation from the unified cache.
	 *
	 * @param cache           the annotation cache built by {@link #buildAnnotationCache}
	 * @param annotationClass the annotation type to look for
	 * @return the annotation instance, or null if not present
	 */
	@SuppressWarnings("unchecked")
	public static <A extends Annotation> A get(
			final Map<Class<? extends Annotation>, Annotation> cache,
			final Class<A> annotationClass) {
		return (A) cache.get(annotationClass);
	}

	/**
	 * Check if an annotation is present in the unified cache.
	 */
	public static boolean has(
			final Map<Class<? extends Annotation>, Annotation> cache,
			final Class<? extends Annotation> annotationClass) {
		return cache.containsKey(annotationClass);
	}

	/**
	 * Collect all annotations of a specific type across all sources (field, getter, setter,
	 * interfaces). Unlike {@link #get}, this returns ALL instances, not just the first.
	 */
	public static <A extends Annotation> List<A> getAll(
			final Field field,
			final Method getter,
			final Method setter,
			final Class<A> annotationClass) {
		final List<A> result = new ArrayList<>();

		if (field != null) {
			final A[] anns = field.getDeclaredAnnotationsByType(annotationClass);
			Collections.addAll(result, anns);
		}

		if (getter != null) {
			final A[] anns = getter.getDeclaredAnnotationsByType(annotationClass);
			Collections.addAll(result, anns);
			collectInterfaceAnnotations(getter, annotationClass, result);
		}

		if (setter != null) {
			final A[] anns = setter.getDeclaredAnnotationsByType(annotationClass);
			Collections.addAll(result, anns);
			collectInterfaceAnnotations(setter, annotationClass, result);
		}

		return result;
	}

	// --- Internal helpers ---

	/**
	 * Walk interfaces of the method's declaring class and collect annotations
	 * from matching interface methods.
	 */
	private static void collectInterfaceAnnotations(
			final Method method,
			final Map<Class<? extends Annotation>, Annotation> cache) {
		final Class<?> declaringClass = method.getDeclaringClass();
		for (final Class<?> iface : declaringClass.getInterfaces()) {
			try {
				final Method ifaceMethod = iface.getMethod(method.getName(), method.getParameterTypes());
				for (final Annotation ann : ifaceMethod.getDeclaredAnnotations()) {
					cache.putIfAbsent(ann.annotationType(), ann);
				}
			} catch (final NoSuchMethodException ignored) {
				// Interface does not declare this method
			}
		}
	}

	/**
	 * Walk interfaces and collect annotations of a specific type from matching methods.
	 */
	private static <A extends Annotation> void collectInterfaceAnnotations(
			final Method method,
			final Class<A> annotationClass,
			final List<A> result) {
		final Class<?> declaringClass = method.getDeclaringClass();
		for (final Class<?> iface : declaringClass.getInterfaces()) {
			try {
				final Method ifaceMethod = iface.getMethod(method.getName(), method.getParameterTypes());
				final A[] anns = ifaceMethod.getDeclaredAnnotationsByType(annotationClass);
				Collections.addAll(result, anns);
			} catch (final NoSuchMethodException ignored) {
				// Interface does not declare this method
			}
		}
	}
}
