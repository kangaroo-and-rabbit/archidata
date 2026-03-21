package org.atriasoft.archidata.tools;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;

/**
 * Utility class for Java reflection type operations.
 */
public class TypeUtils {
	private TypeUtils() {
		// Utility class
	}

	/**
	 * Creates a {@link ParameterizedType} representing {@code List<clazz>}.
	 * @param clazz The element type of the list.
	 * @return A {@link ParameterizedType} for {@code List<clazz>}.
	 */
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

	/**
	 * Compares two {@link Class} instances by their fully qualified name.
	 *
	 * <p>This is needed when {@code ==} can fail between two Class objects loaded by distinct
	 * classloaders (e.g. Jakarta, Hibernate).</p>
	 * @param a The first class.
	 * @param b The second class.
	 * @return {@code true} if both classes have the same fully qualified name.
	 */
	public static boolean isSameClass(final Class<?> a, final Class<?> b) {
		return a.getName().equals(b.getName());
	}
}
