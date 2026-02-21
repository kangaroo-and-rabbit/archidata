package org.atriasoft.archidata.bean;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.atriasoft.archidata.bean.accessor.LambdaAccessorFactory;
import org.atriasoft.archidata.bean.accessor.PropertyGetter;
import org.atriasoft.archidata.bean.accessor.PropertySetter;
import org.atriasoft.archidata.bean.exception.IntrospectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread-safe, cached introspection model of a Java class.
 *
 * <p>ClassModel is the central entry point for the bean introspection library.
 * It analyzes a class once (constructors, fields, methods), builds
 * {@link PropertyDescriptor}s with unified annotation caches and lambda accessors,
 * then caches the result globally.
 *
 * <p>Usage: {@code ClassModel model = ClassModel.of(MyClass.class);}
 *
 * <p>Supports regular classes, Records, Enums, and inner static classes.
 */
public final class ClassModel {
	private static final Logger LOGGER = LoggerFactory.getLogger(ClassModel.class);

	/** Global thread-safe cache: one ClassModel per Class. */
	private static final ConcurrentHashMap<Class<?>, ClassModel> CACHE = new ConcurrentHashMap<>();

	private final Class<?> clazz;
	private final String simpleName;
	private final boolean isRecord;
	private final boolean isEnum;
	private final boolean isInnerClass;

	private final List<PropertyDescriptor> properties;
	private final Map<String, PropertyDescriptor> propertiesByName;
	private final List<ConstructorDescriptor> constructors;
	private final ConstructorDescriptor defaultConstructor;

	// Class-level annotation cache
	private final Map<Class<? extends Annotation>, Annotation> classAnnotationCache;

	// ========== Public API ==========

	/**
	 * Get or create the ClassModel for a given class (thread-safe, cached).
	 */
	public static ClassModel of(final Class<?> clazz) throws IntrospectionException {
		final ClassModel existing = CACHE.get(clazz);
		if (existing != null) {
			return existing;
		}
		// computeIfAbsent can throw unchecked, so we handle the checked exception
		try {
			return CACHE.computeIfAbsent(clazz, cls -> {
				try {
					return new ClassModel(cls);
				} catch (final IntrospectionException e) {
					throw new RuntimeException(e);
				}
			});
		} catch (final RuntimeException e) {
			if (e.getCause() instanceof final IntrospectionException ie) {
				throw ie;
			}
			throw e;
		}
	}

	/**
	 * Clear the global cache. Useful for testing or hot-reload scenarios.
	 */
	public static void clearCache() {
		CACHE.clear();
	}

	public Class<?> getClassType() {
		return this.clazz;
	}

	public String getSimpleName() {
		return this.simpleName;
	}

	public boolean isRecord() {
		return this.isRecord;
	}

	public boolean isEnum() {
		return this.isEnum;
	}

	public boolean isInnerClass() {
		return this.isInnerClass;
	}

	/** All discovered properties, sorted by name. */
	public List<PropertyDescriptor> getProperties() {
		return this.properties;
	}

	/** O(1) property lookup by name. Returns null if not found. */
	public PropertyDescriptor getProperty(final String name) {
		return this.propertiesByName.get(name);
	}

	/** All constructors, sorted by parameter count descending. */
	public List<ConstructorDescriptor> getConstructors() {
		return this.constructors;
	}

	/** The no-arg constructor, or null if none exists. */
	public ConstructorDescriptor getDefaultConstructor() {
		return this.defaultConstructor;
	}

	// --- Class-level annotation API ---

	public <A extends Annotation> boolean hasClassAnnotation(final Class<A> annotationClass) {
		return this.classAnnotationCache.containsKey(annotationClass);
	}

	@SuppressWarnings("unchecked")
	public <A extends Annotation> A getClassAnnotation(final Class<A> annotationClass) {
		return (A) this.classAnnotationCache.get(annotationClass);
	}

	// --- Property search by annotation ---

	/**
	 * Find the first property that has the given annotation.
	 */
	public <A extends Annotation> PropertyDescriptor findPropertyWithAnnotation(final Class<A> annotationClass) {
		for (final PropertyDescriptor p : this.properties) {
			if (p.hasAnnotation(annotationClass)) {
				return p;
			}
		}
		return null;
	}

	/**
	 * Find all properties that have the given annotation.
	 */
	public <A extends Annotation> List<PropertyDescriptor> findPropertiesWithAnnotation(final Class<A> annotationClass) {
		final List<PropertyDescriptor> result = new ArrayList<>();
		for (final PropertyDescriptor p : this.properties) {
			if (p.hasAnnotation(annotationClass)) {
				result.add(p);
			}
		}
		return result;
	}

	// --- Object creation ---

	/**
	 * Create a new instance using the default (no-arg) constructor.
	 *
	 * @throws IntrospectionException if no default constructor or instantiation fails
	 */
	public Object newInstance() throws IntrospectionException {
		if (this.defaultConstructor == null) {
			throw new IntrospectionException("No default constructor for " + this.clazz.getCanonicalName());
		}
		return this.defaultConstructor.newInstance();
	}

	/**
	 * Create a new instance using the best matching constructor for the given values,
	 * then set remaining values via setters.
	 *
	 * @param values property name → value map
	 * @return the new instance with all available values set
	 * @throws IntrospectionException if creation fails
	 */
	public Object newInstance(final Map<String, Object> values) throws IntrospectionException {
		// Try to find the constructor that matches the most parameters
		ConstructorDescriptor bestConstructor = null;
		int bestMatchCount = -1;

		for (final ConstructorDescriptor cd : this.constructors) {
			if (cd.isNoArg()) {
				continue;
			}
			int matchCount = 0;
			for (final String paramName : cd.parameterNames()) {
				if (values.containsKey(paramName)) {
					matchCount++;
				}
			}
			if (matchCount > bestMatchCount) {
				bestMatchCount = matchCount;
				bestConstructor = cd;
			}
		}

		Object instance;
		final Map<String, Object> remaining = new LinkedHashMap<>(values);

		if (bestConstructor != null && bestMatchCount > 0) {
			// Build constructor arguments
			final Object[] args = new Object[bestConstructor.parameterCount()];
			for (int i = 0; i < bestConstructor.parameterNames().length; i++) {
				args[i] = values.get(bestConstructor.parameterNames()[i]);
				remaining.remove(bestConstructor.parameterNames()[i]);
			}
			instance = bestConstructor.newInstance(args);
		} else if (this.defaultConstructor != null) {
			instance = this.defaultConstructor.newInstance();
		} else {
			throw new IntrospectionException("No suitable constructor for " + this.clazz.getCanonicalName());
		}

		// Set remaining values via setters
		for (final Map.Entry<String, Object> entry : remaining.entrySet()) {
			final PropertyDescriptor prop = this.propertiesByName.get(entry.getKey());
			if (prop != null && prop.canWrite() && !prop.isReadOnly()) {
				prop.setValue(instance, entry.getValue());
			}
		}

		return instance;
	}

	// ========== Private constructor — introspection algorithm ==========

	private ClassModel(final Class<?> clazz) throws IntrospectionException {
		this.clazz = clazz;
		this.simpleName = clazz.getSimpleName();
		this.isRecord = Record.class.isAssignableFrom(clazz);
		this.isEnum = clazz.isEnum();
		this.isInnerClass = clazz.getNestHost() != clazz && !Modifier.isStatic(clazz.getModifiers());

		LOGGER.trace("Introspecting class: '{}'", clazz.getCanonicalName());

		// 1. Build class-level annotation cache (class + interfaces)
		this.classAnnotationCache = buildClassAnnotationCache(clazz);

		// 2. Parse constructors
		final List<ConstructorDescriptor> ctors = parseConstructors(clazz);
		this.constructors = Collections.unmodifiableList(ctors);
		this.defaultConstructor = ctors.stream().filter(ConstructorDescriptor::isNoArg).findFirst().orElse(null);

		// Collect record component names if applicable
		final List<String> recordParamNames = new ArrayList<>();
		if (this.isRecord) {
			for (final ConstructorDescriptor cd : ctors) {
				for (final String name : cd.parameterNames()) {
					if (!recordParamNames.contains(name)) {
						recordParamNames.add(name);
					}
				}
			}
		}

		// 3. Build property descriptors from fields and methods
		final Map<String, PropertyDescriptor.Builder> builders = new LinkedHashMap<>();

		// 3a. Parse fields
		parseFields(clazz, builders);

		// 3b. Parse methods
		parseMethods(clazz, builders, recordParamNames);

		// 3c. Mark constructor-settable properties
		for (final ConstructorDescriptor cd : ctors) {
			for (final String paramName : cd.parameterNames()) {
				final PropertyDescriptor.Builder b = builders.get(paramName);
				if (b != null) {
					b.constructorSettable(true);
				}
			}
		}

		// 4. Build (preserving insertion order from LinkedHashMap)
		final List<PropertyDescriptor> props = new ArrayList<>();
		for (final PropertyDescriptor.Builder b : builders.values()) {
			props.add(b.build());
		}

		this.properties = Collections.unmodifiableList(props);
		final Map<String, PropertyDescriptor> byName = new LinkedHashMap<>();
		for (final PropertyDescriptor p : props) {
			byName.put(p.getName(), p);
		}
		this.propertiesByName = Collections.unmodifiableMap(byName);

		LOGGER.trace("Introspected '{}': {} properties, {} constructors",
				this.simpleName, props.size(), ctors.size());
	}

	// ========== Introspection steps ==========

	private static List<ConstructorDescriptor> parseConstructors(final Class<?> clazz) {
		final Constructor<?>[] rawCtors = clazz.getConstructors();
		final List<ConstructorDescriptor> result = new ArrayList<>();

		for (final Constructor<?> ctor : rawCtors) {
			if (!Modifier.isPublic(ctor.getModifiers())) {
				continue;
			}
			final Parameter[] params = ctor.getParameters();
			final String[] names = new String[params.length];
			final TypeInfo[] types = new TypeInfo[params.length];
			for (int i = 0; i < params.length; i++) {
				names[i] = params[i].getName();
				types[i] = TypeInfo.fromConstructorParameter(ctor, i);
			}
			result.add(new ConstructorDescriptor(ctor, names, types));
		}

		// Sort by parameter count descending (most specific first)
		result.sort((a, b) -> Integer.compare(b.parameterCount(), a.parameterCount()));
		return result;
	}

	private static void parseFields(final Class<?> clazz, final Map<String, PropertyDescriptor.Builder> builders) {
		for (final Field field : clazz.getFields()) {
			if (Modifier.isStatic(field.getModifiers())) {
				continue;
			}
			if (!Modifier.isPublic(field.getModifiers())) {
				continue;
			}

			final String name = field.getName();
			final TypeInfo typeInfo = TypeInfo.fromField(field);
			final boolean isFinal = Modifier.isFinal(field.getModifiers());

			final PropertyGetter getter = LambdaAccessorFactory.createFieldGetter(field);
			final PropertySetter setter = isFinal ? null : LambdaAccessorFactory.createFieldSetter(field);

			final PropertyDescriptor.Builder b = builders.computeIfAbsent(name, PropertyDescriptor::builder);
			b.typeInfo(typeInfo)
					.field(field)
					.readOnly(isFinal && setter == null)
					.lambdaGetter(getter);
			if (setter != null) {
				b.lambdaSetter(setter);
			}
		}
	}

	private void parseMethods(
			final Class<?> clazz,
			final Map<String, PropertyDescriptor.Builder> builders,
			final List<String> recordParamNames) {

		final Method[] methods = clazz.getMethods();

		for (final Method method : methods) {
			if (!Modifier.isPublic(method.getModifiers())) {
				continue;
			}
			if (Modifier.isStatic(method.getModifiers())) {
				continue;
			}
			if (method.getDeclaringClass() == Object.class) {
				continue;
			}

			final String methodName = method.getName();

			// Record accessors: match by name (no get/set/is prefix)
			if (this.isRecord && recordParamNames.contains(methodName)) {
				if (method.getParameterCount() == 0 && method.getReturnType() != void.class) {
					final TypeInfo typeInfo = TypeInfo.fromReturnType(method);
					final PropertyGetter getter = LambdaAccessorFactory.createGetter(method);
					final PropertyDescriptor.Builder b = builders.computeIfAbsent(methodName, PropertyDescriptor::builder);
					if (b.getTypeInfo() == null) {
						b.typeInfo(typeInfo);
					}
					b.getter(method).lambdaGetter(getter).readOnly(true);
				}
				continue;
			}

			// getXxx() — non-boolean return, no params, name length > 3, 4th char uppercase
			if (methodName.startsWith("get") && methodName.length() > 3
					&& Character.isUpperCase(methodName.charAt(3))
					&& method.getParameterCount() == 0
					&& method.getReturnType() != void.class
					&& method.getReturnType() != boolean.class
					&& method.getReturnType() != Boolean.class) {

				final String propName = decapitalize(methodName.substring(3));
				final TypeInfo typeInfo = TypeInfo.fromReturnType(method);
				final PropertyGetter getter = LambdaAccessorFactory.createGetter(method);

				final PropertyDescriptor.Builder b = builders.computeIfAbsent(propName, PropertyDescriptor::builder);
				if (b.getTypeInfo() == null) {
					b.typeInfo(typeInfo);
				}
				b.getter(method).lambdaGetter(getter);
				continue;
			}

			// isXxx() — boolean return, no params, name length > 2, 3rd char uppercase
			if (!this.isRecord && methodName.startsWith("is") && methodName.length() > 2
					&& Character.isUpperCase(methodName.charAt(2))
					&& method.getParameterCount() == 0
					&& (method.getReturnType() == boolean.class || method.getReturnType() == Boolean.class)) {

				final String propName = decapitalize(methodName.substring(2));
				final TypeInfo typeInfo = TypeInfo.fromReturnType(method);
				final PropertyGetter getter = LambdaAccessorFactory.createGetter(method);

				final PropertyDescriptor.Builder b = builders.computeIfAbsent(propName, PropertyDescriptor::builder);
				if (b.getTypeInfo() == null) {
					b.typeInfo(typeInfo);
				}
				b.getter(method).lambdaGetter(getter);
				continue;
			}

			// setXxx(value) — void return, 1 param, name length > 3, 4th char uppercase
			if (!this.isRecord && methodName.startsWith("set") && methodName.length() > 3
					&& Character.isUpperCase(methodName.charAt(3))
					&& method.getReturnType() == void.class
					&& method.getParameterCount() == 1) {

				final String propName = decapitalize(methodName.substring(3));
				final TypeInfo typeInfo = TypeInfo.fromFirstParameter(method);
				final PropertySetter setter = LambdaAccessorFactory.createSetter(method);

				final PropertyDescriptor.Builder b = builders.computeIfAbsent(propName, PropertyDescriptor::builder);
				if (b.getTypeInfo() == null) {
					b.typeInfo(typeInfo);
				}
				b.setter(method).lambdaSetter(setter).readOnly(false);
			}
		}
	}

	private static Map<Class<? extends Annotation>, Annotation> buildClassAnnotationCache(final Class<?> clazz) {
		final Map<Class<? extends Annotation>, Annotation> cache = new LinkedHashMap<>();
		// Class annotations
		for (final Annotation ann : clazz.getDeclaredAnnotations()) {
			cache.putIfAbsent(ann.annotationType(), ann);
		}
		// Interface annotations
		for (final Class<?> iface : clazz.getInterfaces()) {
			for (final Annotation ann : iface.getDeclaredAnnotations()) {
				cache.putIfAbsent(ann.annotationType(), ann);
			}
		}
		return Collections.unmodifiableMap(cache);
	}

	private static String decapitalize(final String name) {
		if (name.isEmpty()) {
			return name;
		}
		if (name.length() > 1 && Character.isUpperCase(name.charAt(1))) {
			// Keep "URL" as "URL", not "uRL"
			return name;
		}
		return Character.toLowerCase(name.charAt(0)) + name.substring(1);
	}

	@Override
	public String toString() {
		return "ClassModel{" + this.clazz.getSimpleName() + ", properties=" + this.properties.size()
				+ ", constructors=" + this.constructors.size() + "}";
	}
}
