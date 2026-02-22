package org.atriasoft.archidata.tools;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

public class TypeUtils {
	private TypeUtils() {
		// Utility class
	}

	public static ParameterizedType listOf(final Class<?> clazz) {
		return new ParameterizedType() {
			@Override
			public Type[] getActualTypeArguments() {
				return new Type[] { clazz };
			}

			@Override
			public Type getRawType() {
				return List.class;
			}

			@Override
			public Type getOwnerType() {
				return null;
			}

			@Override
			public String toString() {
				return "List<" + clazz.getTypeName() + ">";
			}
		};
	}

	// this function is needed when '==' can fail between 2 Class due to distinct
	// classloader ==> in case of jakarta, hibernate...
	public static boolean isSameClass(Class<?> a, Class<?> b) {
		return a.getName().equals(b.getName());
	}
}
