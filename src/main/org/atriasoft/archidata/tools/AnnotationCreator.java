package org.atriasoft.archidata.tools;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * Factory for creating annotation instances at runtime using dynamic proxies.
 *
 * <p>Values are passed as key-value pairs; any unspecified method falls back to its default value.</p>
 */
public class AnnotationCreator {
	private AnnotationCreator() {
		// Utility class
	}

	/**
	 * Creates a runtime annotation instance backed by a dynamic proxy.
	 *
	 * @param <A> The annotation type.
	 * @param annotationClass The annotation class to instantiate.
	 * @param values Alternating method-name / value pairs (e.g. "readable", true, "creatable", false).
	 * @return A proxy instance implementing the given annotation type.
	 */
	@SuppressWarnings("unchecked")
	public static <A extends Annotation> A createAnnotation(final Class<A> annotationClass, final Object... values) {
		return (A) Proxy.newProxyInstance(annotationClass.getClassLoader(), new Class<?>[] { annotationClass },
				new InvocationHandler() {
					@Override
					public Object invoke(final Object proxy, final Method method, final Object[] args)
							throws Throwable {
						if ("annotationType".equals(method.getName())) {
							return annotationClass;
						}
						if ("toString".equals(method.getName())) {
							return "@" + annotationClass.getName() + values;
						}
						for (int i = 0; i < values.length; i += 2) {
							if (method.getName().equals(values[i])) {
								return values[i + 1];
							}
						}
						return method.getDefaultValue();
					}
				});
	}

}