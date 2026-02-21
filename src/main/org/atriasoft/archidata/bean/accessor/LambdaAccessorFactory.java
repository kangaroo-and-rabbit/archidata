package org.atriasoft.archidata.bean.accessor;

import java.lang.invoke.CallSite;
import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory that creates high-performance {@link PropertyGetter} and {@link PropertySetter}
 * implementations using {@code LambdaMetafactory} when possible, falling back to
 * {@code MethodHandle} direct invocation.
 *
 * <p>Lambda-based accessors have near-native call performance (no reflection overhead per call),
 * while MethodHandle fallback is still ~3x faster than {@code Method.invoke()}.
 *
 * <p>Also provides typed accessors ({@link TypedPropertyGetter}, {@link TypedPropertySetter})
 * that preserve generic type information and avoid boxing for reference types.
 */
public final class LambdaAccessorFactory {
	private static final Logger LOGGER = LoggerFactory.getLogger(LambdaAccessorFactory.class);

	private LambdaAccessorFactory() {}

	// ========== GETTER from Method ==========

	/**
	 * Create a PropertyGetter from a getter Method (getXxx() or isXxx()).
	 * Tries LambdaMetafactory first, falls back to MethodHandle.
	 */
	public static PropertyGetter createGetter(final Method getter) {
		try {
			return createLambdaGetter(getter);
		} catch (final Throwable e) {
			LOGGER.debug("LambdaMetafactory failed for getter {}, falling back to MethodHandle", getter.getName(), e);
			return createMethodHandleGetter(getter);
		}
	}

	/**
	 * Create a PropertySetter from a setter Method (setXxx(value)).
	 * Tries LambdaMetafactory first, falls back to MethodHandle.
	 */
	public static PropertySetter createSetter(final Method setter) {
		try {
			return createLambdaSetter(setter);
		} catch (final Throwable e) {
			LOGGER.debug("LambdaMetafactory failed for setter {}, falling back to MethodHandle", setter.getName(), e);
			return createMethodHandleSetter(setter);
		}
	}

	// ========== GETTER/SETTER from Field ==========

	/**
	 * Create a PropertyGetter for a Field (direct field read).
	 * Uses MethodHandle for field access (LambdaMetafactory doesn't support fields directly).
	 */
	public static PropertyGetter createFieldGetter(final Field field) {
		try {
			final MethodHandles.Lookup lookup = MethodHandles.lookup();
			field.setAccessible(true);
			final MethodHandle handle = lookup.unreflectGetter(field);
			return instance -> handle.invoke(instance);
		} catch (final Throwable e) {
			LOGGER.debug("MethodHandle failed for field getter {}, falling back to reflection", field.getName(), e);
			field.setAccessible(true);
			return field::get;
		}
	}

	/**
	 * Create a PropertySetter for a Field (direct field write).
	 * Returns null if the field is final.
	 */
	public static PropertySetter createFieldSetter(final Field field) {
		if (Modifier.isFinal(field.getModifiers())) {
			return null;
		}
		try {
			final MethodHandles.Lookup lookup = MethodHandles.lookup();
			field.setAccessible(true);
			final MethodHandle handle = lookup.unreflectSetter(field);
			return (instance, value) -> handle.invoke(instance, value);
		} catch (final Throwable e) {
			LOGGER.debug("MethodHandle failed for field setter {}, falling back to reflection", field.getName(), e);
			field.setAccessible(true);
			return field::set;
		}
	}

	// ========== Lambda via LambdaMetafactory ==========

	@SuppressWarnings("unchecked")
	private static PropertyGetter createLambdaGetter(final Method getter) throws Throwable {
		final MethodHandles.Lookup lookup = MethodHandles.lookup();
		final MethodHandle handle = lookup.unreflect(getter);

		final CallSite site = LambdaMetafactory.metafactory(
				lookup,
				"apply",
				MethodType.methodType(Function.class),
				MethodType.methodType(Object.class, Object.class), // Function<Object, Object>
				handle,
				MethodType.methodType(getter.getReturnType(), getter.getDeclaringClass()));

		final Function<Object, Object> fn = (Function<Object, Object>) site.getTarget().invokeExact();
		return fn::apply;
	}

	@SuppressWarnings("unchecked")
	private static PropertySetter createLambdaSetter(final Method setter) throws Throwable {
		final MethodHandles.Lookup lookup = MethodHandles.lookup();
		final MethodHandle handle = lookup.unreflect(setter);

		final CallSite site = LambdaMetafactory.metafactory(
				lookup,
				"accept",
				MethodType.methodType(BiConsumer.class),
				MethodType.methodType(void.class, Object.class, Object.class), // BiConsumer<Object, Object>
				handle,
				MethodType.methodType(void.class, setter.getDeclaringClass(), setter.getParameterTypes()[0]));

		final BiConsumer<Object, Object> fn = (BiConsumer<Object, Object>) site.getTarget().invokeExact();
		return fn::accept;
	}

	// ========== MethodHandle fallback ==========

	private static PropertyGetter createMethodHandleGetter(final Method getter) {
		try {
			final MethodHandles.Lookup lookup = MethodHandles.lookup();
			final MethodHandle handle = lookup.unreflect(getter);
			return instance -> handle.invoke(instance);
		} catch (final IllegalAccessException e) {
			LOGGER.debug("MethodHandle unreflect failed for getter {}, using Method.invoke", getter.getName(), e);
			getter.setAccessible(true);
			return instance -> getter.invoke(instance);
		}
	}

	private static PropertySetter createMethodHandleSetter(final Method setter) {
		try {
			final MethodHandles.Lookup lookup = MethodHandles.lookup();
			final MethodHandle handle = lookup.unreflect(setter);
			return (instance, value) -> handle.invoke(instance, value);
		} catch (final IllegalAccessException e) {
			LOGGER.debug("MethodHandle unreflect failed for setter {}, using Method.invoke", setter.getName(), e);
			setter.setAccessible(true);
			return (instance, value) -> setter.invoke(instance, value);
		}
	}

	// ========== Typed Getter/Setter from Method ==========

	/**
	 * Create a TypedPropertyGetter from a getter Method (getXxx() or isXxx()).
	 * Uses LambdaMetafactory with concrete types to avoid boxing for reference types.
	 * Falls back to MethodHandle if LambdaMetafactory fails.
	 *
	 * @param <T> the bean type
	 * @param <V> the property value type
	 * @param getter the getter method
	 * @param beanType the bean class
	 * @param valueType the property value class
	 * @return a typed property getter
	 */
	@SuppressWarnings("unchecked")
	public static <T, V> TypedPropertyGetter<T, V> createTypedGetter(
			final Method getter,
			final Class<T> beanType,
			final Class<V> valueType) {
		try {
			final MethodHandles.Lookup lookup = MethodHandles.lookup();
			final MethodHandle handle = lookup.unreflect(getter);

			final CallSite site = LambdaMetafactory.metafactory(
					lookup,
					"apply",
					MethodType.methodType(Function.class),
					MethodType.methodType(Object.class, Object.class),
					handle,
					MethodType.methodType(getter.getReturnType(), getter.getDeclaringClass()));

			final Function<Object, Object> fn = (Function<Object, Object>) site.getTarget().invokeExact();
			return instance -> (V) fn.apply(instance);
		} catch (final Throwable e) {
			LOGGER.debug("LambdaMetafactory failed for typed getter {}, falling back to MethodHandle",
					getter.getName(), e);
			return createMethodHandleTypedGetter(getter);
		}
	}

	/**
	 * Create a TypedPropertySetter from a setter Method (setXxx(value)).
	 * Uses LambdaMetafactory with concrete types to avoid boxing for reference types.
	 * Falls back to MethodHandle if LambdaMetafactory fails.
	 *
	 * @param <T> the bean type
	 * @param <V> the property value type
	 * @param setter the setter method
	 * @param beanType the bean class
	 * @param valueType the property value class
	 * @return a typed property setter
	 */
	@SuppressWarnings("unchecked")
	public static <T, V> TypedPropertySetter<T, V> createTypedSetter(
			final Method setter,
			final Class<T> beanType,
			final Class<V> valueType) {
		try {
			final MethodHandles.Lookup lookup = MethodHandles.lookup();
			final MethodHandle handle = lookup.unreflect(setter);

			final CallSite site = LambdaMetafactory.metafactory(
					lookup,
					"accept",
					MethodType.methodType(BiConsumer.class),
					MethodType.methodType(void.class, Object.class, Object.class),
					handle,
					MethodType.methodType(void.class, setter.getDeclaringClass(), setter.getParameterTypes()[0]));

			final BiConsumer<Object, Object> fn = (BiConsumer<Object, Object>) site.getTarget().invokeExact();
			return (instance, value) -> fn.accept(instance, value);
		} catch (final Throwable e) {
			LOGGER.debug("LambdaMetafactory failed for typed setter {}, falling back to MethodHandle",
					setter.getName(), e);
			return createMethodHandleTypedSetter(setter);
		}
	}

	// ========== Typed Getter/Setter from Field ==========

	/**
	 * Create a TypedPropertyGetter for a Field (direct field read).
	 */
	@SuppressWarnings("unchecked")
	public static <T, V> TypedPropertyGetter<T, V> createTypedFieldGetter(
			final Field field,
			final Class<T> beanType,
			final Class<V> valueType) {
		try {
			final MethodHandles.Lookup lookup = MethodHandles.lookup();
			field.setAccessible(true);
			final MethodHandle handle = lookup.unreflectGetter(field);
			return instance -> (V) handle.invoke(instance);
		} catch (final Throwable e) {
			LOGGER.debug("MethodHandle failed for typed field getter {}, falling back to reflection",
					field.getName(), e);
			field.setAccessible(true);
			return instance -> (V) field.get(instance);
		}
	}

	/**
	 * Create a TypedPropertySetter for a Field (direct field write).
	 * Returns null if the field is final.
	 */
	public static <T, V> TypedPropertySetter<T, V> createTypedFieldSetter(
			final Field field,
			final Class<T> beanType,
			final Class<V> valueType) {
		if (Modifier.isFinal(field.getModifiers())) {
			return null;
		}
		try {
			final MethodHandles.Lookup lookup = MethodHandles.lookup();
			field.setAccessible(true);
			final MethodHandle handle = lookup.unreflectSetter(field);
			return (instance, value) -> handle.invoke(instance, value);
		} catch (final Throwable e) {
			LOGGER.debug("MethodHandle failed for typed field setter {}, falling back to reflection",
					field.getName(), e);
			field.setAccessible(true);
			return (instance, value) -> field.set(instance, value);
		}
	}

	// ========== Typed MethodHandle fallback ==========

	@SuppressWarnings("unchecked")
	private static <T, V> TypedPropertyGetter<T, V> createMethodHandleTypedGetter(final Method getter) {
		try {
			final MethodHandles.Lookup lookup = MethodHandles.lookup();
			final MethodHandle handle = lookup.unreflect(getter);
			return instance -> (V) handle.invoke(instance);
		} catch (final IllegalAccessException e) {
			LOGGER.debug("MethodHandle unreflect failed for typed getter {}, using Method.invoke",
					getter.getName(), e);
			getter.setAccessible(true);
			return instance -> (V) getter.invoke(instance);
		}
	}

	@SuppressWarnings("unchecked")
	private static <T, V> TypedPropertySetter<T, V> createMethodHandleTypedSetter(final Method setter) {
		try {
			final MethodHandles.Lookup lookup = MethodHandles.lookup();
			final MethodHandle handle = lookup.unreflect(setter);
			return (instance, value) -> handle.invoke(instance, value);
		} catch (final IllegalAccessException e) {
			LOGGER.debug("MethodHandle unreflect failed for typed setter {}, using Method.invoke",
					setter.getName(), e);
			setter.setAccessible(true);
			return (instance, value) -> setter.invoke(instance, value);
		}
	}
}
