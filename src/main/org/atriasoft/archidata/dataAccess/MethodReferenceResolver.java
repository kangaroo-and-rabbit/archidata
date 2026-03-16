package org.atriasoft.archidata.dataAccess;

import java.io.Serializable;
import java.lang.invoke.SerializedLambda;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

import org.atriasoft.archidata.dataAccess.model.DbClassModel;
import org.atriasoft.archidata.dataAccess.model.DbPropertyDescriptor;

/**
 * Resolves serializable method references (getter/setter) to their corresponding
 * MongoDB database field names using the existing {@link DbClassModel} cache.
 *
 * <p>This class caches the resolved field names per method reference to avoid
 * repeated {@code SerializedLambda} extraction and model lookups.
 *
 * @see Filters
 */
public final class MethodReferenceResolver {

	/** Cache: "implClass::implMethodName" -> resolved DB field name. */
	private static final ConcurrentHashMap<String, String> CACHE = new ConcurrentHashMap<>();

	private MethodReferenceResolver() {}

	/**
	 * Resolve a getter method reference to its database field name.
	 *
	 * @param getter a serializable getter reference (e.g. User::getName)
	 * @return the database field name (e.g. "name" or "custom_name" if @Column(name) is set)
	 * @throws IllegalArgumentException if the method reference cannot be resolved
	 */
	public static <T, R> String resolveFieldName(final SerializableFunction<T, R> getter) {
		return resolveFromSerializable(getter);
	}

	/**
	 * Resolve a setter method reference to its database field name.
	 *
	 * @param setter a serializable setter reference (e.g. User::setName)
	 * @return the database field name
	 * @throws IllegalArgumentException if the method reference cannot be resolved
	 */
	public static <T, V> String resolveFieldName(final SerializableBiConsumer<T, V> setter) {
		return resolveFromSerializable(setter);
	}

	private static String resolveFromSerializable(final Serializable lambda) {
		final SerializedLambda serialized = extractSerializedLambda(lambda);
		final String cacheKey = serialized.getImplClass() + "::" + serialized.getImplMethodName();

		return CACHE.computeIfAbsent(cacheKey, key -> {
			final String implMethodName = serialized.getImplMethodName();
			final String implClassName = serialized.getImplClass().replace('/', '.');

			// 1. Extract property name from method name
			final String propertyName = extractPropertyName(implMethodName);

			// 2. Load the declaring class
			final Class<?> declaringClass;
			try {
				declaringClass = Class.forName(implClassName);
			} catch (final ClassNotFoundException e) {
				throw new IllegalArgumentException("Cannot load class for method reference: " + implClassName, e);
			}

			// 3. Look up via DbClassModel -> DbPropertyDescriptor
			try {
				final DbClassModel dbModel = DbClassModel.of(declaringClass);
				final DbPropertyDescriptor descriptor = dbModel.findByPropertyName(propertyName);
				if (descriptor != null) {
					return descriptor.getDbFieldName();
				}
			} catch (final Exception e) {
				throw new IllegalArgumentException("Cannot resolve DB field name for method reference: " + implClassName
						+ "::" + implMethodName + " (property: " + propertyName + ")", e);
			}

			throw new IllegalArgumentException("Cannot resolve DB field name for method reference: " + implClassName
					+ "::" + implMethodName + " (property: " + propertyName + ")");
		});
	}

	private static SerializedLambda extractSerializedLambda(final Serializable lambda) {
		try {
			final Method writeReplace = lambda.getClass().getDeclaredMethod("writeReplace");
			writeReplace.setAccessible(true);
			return (SerializedLambda) writeReplace.invoke(lambda);
		} catch (final Exception e) {
			throw new IllegalArgumentException(
					"Cannot extract SerializedLambda from method reference. "
							+ "Ensure you pass a method reference (e.g. User::getName), " + "not a lambda expression.",
					e);
		}
	}

	/**
	 * Extract the Java property name from a getter/setter method name.
	 * <ul>
	 * <li>getFirstName -&gt; firstName</li>
	 * <li>isActive -&gt; active</li>
	 * <li>setName -&gt; name</li>
	 * <li>name -&gt; name (record accessor or direct field)</li>
	 * </ul>
	 *
	 * <p>The decapitalization logic matches {@code ClassModel.decapitalize()}:
	 * "URL" stays as "URL" (consecutive uppercase), "Name" becomes "name".
	 */
	public static String extractPropertyName(final String methodName) {
		String stripped;
		if (methodName.startsWith("get") && methodName.length() > 3 && Character.isUpperCase(methodName.charAt(3))) {
			stripped = methodName.substring(3);
		} else if (methodName.startsWith("is") && methodName.length() > 2
				&& Character.isUpperCase(methodName.charAt(2))) {
			stripped = methodName.substring(2);
		} else if (methodName.startsWith("set") && methodName.length() > 3
				&& Character.isUpperCase(methodName.charAt(3))) {
			stripped = methodName.substring(3);
		} else {
			// Direct field reference or record accessor — use as-is
			return methodName;
		}
		return decapitalize(stripped);
	}

	private static String decapitalize(final String name) {
		if (name.isEmpty()) {
			return name;
		}
		// Keep consecutive uppercase as-is (e.g. "URL" -> "URL", not "uRL")
		if (name.length() > 1 && Character.isUpperCase(name.charAt(1))) {
			return name;
		}
		return Character.toLowerCase(name.charAt(0)) + name.substring(1);
	}

	/** Clear the cache. Useful for testing. */
	public static void clearCache() {
		CACHE.clear();
	}
}
