package org.atriasoft.archidata.tools;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.atriasoft.archidata.annotation.apiGenerator.ApiGenerationMode;

public class AnnotationCreator {
	private AnnotationCreator() {
		// Utility class
	}

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

	public static void main(final String[] args) {
		final ApiGenerationMode myAnnotation = AnnotationCreator.createAnnotation(ApiGenerationMode.class, "readable",
				true, "creatable", false, "updatable", false);

		System.out.println("readable: " + myAnnotation.read()); // Output: example
		System.out.println("creatable: " + myAnnotation.create()); // Output: 100
		System.out.println("updatable: " + myAnnotation.update()); // Output: 100
		final ApiGenerationMode myAnnotation2 = AnnotationCreator.createAnnotation(ApiGenerationMode.class);

		System.out.println("readable: " + myAnnotation2.read()); // Output: example
		System.out.println("creatable: " + myAnnotation2.create()); // Output: 100
		System.out.println("updatable: " + myAnnotation2.update()); // Output: 100
	}
}